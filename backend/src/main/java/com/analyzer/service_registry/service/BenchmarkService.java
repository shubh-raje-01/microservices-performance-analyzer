package com.analyzer.service_registry.service;

import com.analyzer.common.utils.MetricsCalculator;
import com.analyzer.service_registry.dto.BenchmarkRequestDto;
import com.analyzer.service_registry.dto.BenchmarkResultDto;
import com.analyzer.service_registry.model.Service;
import com.analyzer.service_registry.model.ServiceBenchmark;
import com.analyzer.service_registry.repository.ServiceBenchmarkRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@org.springframework.stereotype.Service
public class BenchmarkService {

    private final ServiceBenchmarkRepository benchmarkRepository;
    private final ServiceRegistryService registryService;
    private final WebClient webClient;

    private final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2,
            r -> {
                Thread t = new Thread(r, "benchmark-worker");
                t.setDaemon(true);
                return t;
            }
    );

    public BenchmarkService(
            ServiceBenchmarkRepository benchmarkRepository,
            ServiceRegistryService registryService,
            @Qualifier("monitoringWebClient") WebClient webClient) {
        this.benchmarkRepository = benchmarkRepository;
        this.registryService = registryService;
        this.webClient = webClient;
    }

    public BenchmarkResultDto runBenchmark(String serviceId, BenchmarkRequestDto request) {
        Service service = registryService.getById(serviceId);
        String fullUrl = buildUrl(service, request.getEndpoint());

        log.info("Starting benchmark: {} {} x{} (concurrency={})",
                request.getMethod(), fullUrl, request.getRequestCount(), request.getConcurrency());

        int requestCount = Math.min(request.getRequestCount(), 1000);
        int concurrency = Math.min(request.getConcurrency(), 50);
        int timeoutSeconds = request.getTimeoutSeconds() != null ? request.getTimeoutSeconds() : 10;

        List<Future<BenchmarkResultDto.RequestLatencyDto>> futures = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(requestCount);
        Semaphore semaphore = new Semaphore(concurrency);

        for (int i = 0; i < requestCount; i++) {
            final int seq = i + 1;
            futures.add(executor.submit(() -> {
                try {
                    semaphore.acquire();
                    return executeRequest(fullUrl, request.getMethod(), seq, timeoutSeconds,
                            request.getHeaders(), request.getBody());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return BenchmarkResultDto.RequestLatencyDto.builder()
                            .sequence(seq).latencyMs(0).success(false)
                            .errorMessage("Interrupted").build();
                } finally {
                    semaphore.release();
                    latch.countDown();
                }
            }));
        }

        try {
            latch.await(timeoutSeconds * (long) requestCount + 30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        List<BenchmarkResultDto.RequestLatencyDto> results = futures.stream()
                .map(f -> {
                    try {
                        return f.get(1, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        return BenchmarkResultDto.RequestLatencyDto.builder()
                                .sequence(0).latencyMs(0).success(false)
                                .errorMessage(e.getMessage()).build();
                    }
                })
                .collect(Collectors.toList());

        return buildResult(service, request, results);
    }

    private BenchmarkResultDto.RequestLatencyDto executeRequest(
            String url, String method, int sequence, int timeoutSeconds,
            Map<String, String> headers, String body) {

        long start = System.currentTimeMillis();
        try {
            WebClient.RequestHeadersSpec<?> requestSpec = webClient.method(
                    org.springframework.http.HttpMethod.valueOf(method.toUpperCase()))
                    .uri(url)
                    .headers(h -> {
                        if (headers != null) {
                            headers.forEach(h::add);
                        }
                    });

            if (body != null && !body.isBlank()
                    && (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT"))) {
                requestSpec = ((WebClient.RequestBodySpec) requestSpec).bodyValue(body);
            }

            var response = requestSpec
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            long latency = System.currentTimeMillis() - start;
            int statusCode = response != null ? response.getStatusCode().value() : 0;

            ServiceBenchmark benchmark = new ServiceBenchmark();
            benchmark.setEndpoint(url);
            benchmark.setMethod(method);
            benchmark.setHttpStatus(statusCode);
            benchmark.setLatencyMs(latency);
            benchmark.setTimestamp(Instant.now());
            benchmark.setUserAgent("MicroserviceAnalyzer/1.0");
            benchmark.setSuccess(statusCode >= 200 && statusCode < 400);
            benchmarkRepository.save(benchmark);

            return BenchmarkResultDto.RequestLatencyDto.builder()
                    .sequence(sequence)
                    .latencyMs(latency)
                    .statusCode(statusCode)
                    .success(statusCode >= 200 && statusCode < 400)
                    .build();

        } catch (Exception ex) {
            long latency = System.currentTimeMillis() - start;

            ServiceBenchmark benchmark = new ServiceBenchmark();
            benchmark.setEndpoint(url);
            benchmark.setMethod(method);
            benchmark.setHttpStatus(0);
            benchmark.setLatencyMs(latency);
            benchmark.setTimestamp(Instant.now());
            benchmark.setUserAgent("MicroserviceAnalyzer/1.0");
            benchmark.setSuccess(false);
            benchmarkRepository.save(benchmark);

            return BenchmarkResultDto.RequestLatencyDto.builder()
                    .sequence(sequence)
                    .latencyMs(latency)
                    .statusCode(0)
                    .success(false)
                    .errorMessage(truncate(ex.getMessage()))
                    .build();
        }
    }

    private BenchmarkResultDto buildResult(
            Service service, BenchmarkRequestDto request,
            List<BenchmarkResultDto.RequestLatencyDto> results) {

        List<Long> latencies = results.stream()
                .map(BenchmarkResultDto.RequestLatencyDto::getLatencyMs)
                .toList();

        List<Double> doubleLatencies = latencies.stream()
                .map(l -> (double) l)
                .collect(Collectors.toList());

        int successCount = (int) results.stream().filter(BenchmarkResultDto.RequestLatencyDto::isSuccess).count();
        int failureCount = results.size() - successCount;

        double totalLatency = latencies.stream().mapToLong(Long::longValue).sum();
        double avgLatency = results.isEmpty() ? 0 : totalLatency / results.size();
        double durationSeconds = totalLatency / 1000.0;
        double rps = durationSeconds > 0 ? results.size() / durationSeconds : 0;

        return BenchmarkResultDto.builder()
                .serviceId(service.getId())
                .serviceName(service.getName())
                .endpoint(request.getEndpoint())
                .method(request.getMethod())
                .requestCount(results.size())
                .successCount(successCount)
                .failureCount(failureCount)
                .avgLatencyMs(MetricsCalculator.round(avgLatency, 2))
                .minLatencyMs(MetricsCalculator.min(doubleLatencies))
                .maxLatencyMs(MetricsCalculator.max(doubleLatencies))
                .p50LatencyMs(MetricsCalculator.p50(doubleLatencies))
                .p95LatencyMs(MetricsCalculator.p95(doubleLatencies))
                .p99LatencyMs(MetricsCalculator.p99(doubleLatencies))
                .stdDeviation(MetricsCalculator.stdDev(doubleLatencies))
                .requestsPerSecond(MetricsCalculator.round(rps, 2))
                .errorRate(MetricsCalculator.round(
                        MetricsCalculator.errorRate(failureCount, results.size()), 4))
                .individualResults(results)
                .executedAt(Instant.now())
                .build();
    }

    private String buildUrl(Service service, String endpoint) {
        String base = service.getBaseUrl().replaceAll("/+$", "");
        if (endpoint == null || endpoint.isBlank()) {
            return base;
        }
        if (!endpoint.startsWith("/")) {
            endpoint = "/" + endpoint;
        }
        return base + endpoint;
    }

    private String truncate(String msg) {
        if (msg == null) return null;
        return msg.length() <= 200 ? msg : msg.substring(0, 197) + "...";
    }
}
