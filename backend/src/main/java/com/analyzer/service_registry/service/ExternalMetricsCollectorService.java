package com.analyzer.service_registry.service;

import com.analyzer.service_registry.model.Service;
import com.analyzer.service_registry.model.ServiceMetrics;
import com.analyzer.service_registry.repository.ServiceMetricsRepository;
import com.analyzer.service_registry.repository.ServiceRepository;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.util.retry.RetryBackoffSpec;

import java.net.ConnectException;
import java.net.http.HttpConnectTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@org.springframework.stereotype.Service
public class ExternalMetricsCollectorService {

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final Duration FIRST_BACKOFF = Duration.ofMillis(500);

    private final ServiceRepository serviceRepository;
    private final ServiceMetricsRepository metricsRepository;
    private final PrometheusMetricsParser parser;
    private final WebClient webClient;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

    public ExternalMetricsCollectorService(
            ServiceRepository serviceRepository,
            ServiceMetricsRepository metricsRepository,
            PrometheusMetricsParser parser,
            @Qualifier("monitoringWebClient") WebClient webClient) {
        this.serviceRepository = serviceRepository;
        this.metricsRepository = metricsRepository;
        this.parser = parser;
        this.webClient = webClient;
    }

    @Scheduled(fixedDelayString = "${monitoring.metrics.interval:30000}", initialDelay = 10000)
    public void collectAllMetrics() {
        List<Service> enabledServices = serviceRepository.findAllEnabled();
        if (enabledServices.isEmpty()) {
            return;
        }

        log.debug("Metrics collection cycle — collecting from {} services", enabledServices.size());

        for (Service service : enabledServices) {
            if (service.getMetricsEndpoint() == null || service.getMetricsEndpoint().isBlank()) {
                continue;
            }
            collectMetricsAsync(service);
        }
    }

    public void collectMetricsAsync(Service service) {
        String metricsUrl = buildMetricsUrl(service);

        RetryBackoffSpec retrySpec = RetryBackoffSpec.backoff(MAX_RETRY_ATTEMPTS, FIRST_BACKOFF)
                .filter(throwable -> throwable instanceof ConnectException
                        || throwable instanceof HttpConnectTimeoutException
                        || throwable instanceof WebClientRequestException
                        || throwable instanceof java.io.IOException)
                .doBeforeRetry(retrySignal -> log.warn("Retrying metrics collection for {} — {}",
                        service.getName(), retrySignal.failure().getMessage()));

        webClient.get()
                .uri(metricsUrl)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(15))
                .retryWhen(retrySpec)
                .subscribe(
                        rawMetrics -> {
                            processMetrics(service, rawMetrics);
                            consecutiveFailures.set(0);
                        },
                        error -> {
                            consecutiveFailures.incrementAndGet();
                            log.error("Metrics collection failed for {} after retries — {}",
                                    service.getName(), error.getMessage());
                        }
                );
    }

    @Retry(name = "metricsCollection", fallbackMethod = "collectMetricsFallback")
    public void collectMetrics(Service service) {
        String metricsUrl = buildMetricsUrl(service);

        String rawMetrics = webClient.get()
                .uri(metricsUrl)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(15))
                .block();

        processMetrics(service, rawMetrics);
    }

    public void collectMetricsFallback(Service service, Exception ex) {
        log.error("Metrics collection failed for {} — graceful degradation: {}",
                service.getName(), ex.getMessage());
    }

    private void processMetrics(Service service, String rawMetrics) {
        if (rawMetrics == null || rawMetrics.isBlank()) {
            log.debug("No metrics returned from {}", service.getName());
            return;
        }

        PrometheusMetricsParser.ParsedMetrics parsed = parser.parse(rawMetrics);
        Instant now = Instant.now();

        Map<String, Double> keyMetrics = parsed.getKeyMetrics();

        for (Map.Entry<String, Double> entry : keyMetrics.entrySet()) {
            ServiceMetrics metric = new ServiceMetrics();
            metric.setService(service);
            metric.setMetricName(entry.getKey());
            metric.setMetricValue(entry.getValue());
            metric.setMetricType(PrometheusMetricsParser.categorizeMetric(entry.getKey()));
            metric.setTimestamp(now);
            metricsRepository.save(metric);
        }

        log.debug("Collected {} metrics from {}", keyMetrics.size(), service.getName());
    }

    private String buildMetricsUrl(Service service) {
        String base = service.getBaseUrl().replaceAll("/+$", "");
        String endpoint = service.getMetricsEndpoint();
        if (!endpoint.startsWith("/")) {
            endpoint = "/" + endpoint;
        }
        return base + endpoint;
    }

    public List<ServiceMetrics> getMetricTrend(String serviceId, String metricName, int hours) {
        Instant since = Instant.now().minusSeconds(hours * 3600L);
        return metricsRepository.findMetricTrend(serviceId, metricName, since);
    }

    public Map<String, Double> getLatestMetrics(String serviceId) {
        Instant since = Instant.now().minusSeconds(300);
        List<Object[]> averages = metricsRepository.findMetricAverages(serviceId, since);
        return averages.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Double) row[1]
                ));
    }

    public List<ServiceMetrics> getMetricsByCategory(String serviceId, String category, int hours) {
        Instant since = Instant.now().minusSeconds(hours * 3600L);
        return metricsRepository.findByMetricType(serviceId, category, since);
    }

    public List<ServiceMetrics> getMetricsByNames(String serviceId, List<String> metricNames, int hours) {
        Instant since = Instant.now().minusSeconds(hours * 3600L);
        return metricsRepository.findByMetricNames(serviceId, metricNames, since);
    }

    public List<Object[]> getMetricTypeAggregates(String serviceId, String category, int hours) {
        Instant since = Instant.now().minusSeconds(hours * 3600L);
        return metricsRepository.findMetricTypeAggregates(serviceId, category, since);
    }

    public List<ServiceMetrics> getTimeSeries(String serviceId, int hours) {
        Instant since = Instant.now().minusSeconds(hours * 3600L);
        return metricsRepository.findTimeSeriesByService(serviceId, since);
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }
}
