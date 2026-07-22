package com.analyzer.service_registry.controller;

import com.analyzer.common.dto.ApiResponse;
import com.analyzer.service_registry.dto.*;
import com.analyzer.service_registry.model.ServiceMetrics;
import com.analyzer.service_registry.service.ExternalMetricsCollectorService;
import com.analyzer.service_registry.service.PrometheusMetricsParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ServiceMetricsController {

    private final ExternalMetricsCollectorService metricsCollector;

    public ResponseEntity<ApiResponse<?>> getAllMetrics(String serviceId, int hours) {
        List<ServiceMetrics> allMetrics = metricsCollector.getTimeSeries(serviceId, hours);

        if (allMetrics.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No metrics found for service", Map.of()));
        }

        Map<String, List<MetricTrendPointDto>> trends = buildAllMetricTrends(allMetrics);

        ServiceMetricsSummaryDto summary = ServiceMetricsSummaryDto.builder()
                .serviceId(serviceId)
                .cpu(buildCpuMetrics(allMetrics, hours))
                .memory(buildMemoryMetrics(allMetrics, hours))
                .latency(buildLatencyMetrics(allMetrics, hours))
                .requests(buildRequestMetrics(allMetrics, hours))
                .allMetricTrends(trends)
                .collectedAt(allMetrics.isEmpty() ? Instant.now() : allMetrics.get(0).getTimestamp())
                .hoursBack(hours)
                .build();

        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    public ResponseEntity<ApiResponse<?>> getCpuMetrics(String serviceId, int hours) {
        List<ServiceMetrics> cpuMetrics = metricsCollector.getMetricsByCategory(
                serviceId, PrometheusMetricsParser.CATEGORY_CPU, hours);

        if (cpuMetrics.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No CPU metrics found", Map.of()));
        }

        CpuMetricsDto dto = buildCpuMetrics(cpuMetrics, hours);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    public ResponseEntity<ApiResponse<?>> getMemoryMetrics(String serviceId, int hours) {
        List<ServiceMetrics> memoryMetrics = metricsCollector.getMetricsByCategory(
                serviceId, PrometheusMetricsParser.CATEGORY_MEMORY, hours);

        if (memoryMetrics.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No memory metrics found", Map.of()));
        }

        MemoryMetricsDto dto = buildMemoryMetrics(memoryMetrics, hours);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    public ResponseEntity<ApiResponse<?>> getLatencyMetrics(String serviceId, int hours) {
        List<ServiceMetrics> latencyMetrics = metricsCollector.getMetricsByCategory(
                serviceId, PrometheusMetricsParser.CATEGORY_LATENCY, hours);

        if (latencyMetrics.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No latency metrics found", Map.of()));
        }

        LatencyMetricsDto dto = buildLatencyMetrics(latencyMetrics, hours);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    public ResponseEntity<ApiResponse<?>> getRequestMetrics(String serviceId, int hours) {
        List<ServiceMetrics> requestMetrics = metricsCollector.getMetricsByCategory(
                serviceId, PrometheusMetricsParser.CATEGORY_REQUESTS, hours);

        if (requestMetrics.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No request metrics found", Map.of()));
        }

        RequestMetricsDto dto = buildRequestMetrics(requestMetrics, hours);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    private CpuMetricsDto buildCpuMetrics(List<ServiceMetrics> metrics, int hours) {
        Map<String, List<ServiceMetrics>> byName = groupByName(metrics);

        Double systemCpu = getLatestValue(byName, "system.cpu.usage");
        Double processCpu = getLatestValue(byName, "process.cpu.usage");

        List<MetricTrendPointDto> systemTrend = byName.getOrDefault("system.cpu.usage", List.of()).stream()
                .map(m -> MetricTrendPointDto.builder()
                        .metricName(m.getMetricName())
                        .value(m.getMetricValue())
                        .timestamp(m.getTimestamp())
                        .build())
                .collect(Collectors.toList());

        List<MetricTrendPointDto> processTrend = byName.getOrDefault("process.cpu.usage", List.of()).stream()
                .map(m -> MetricTrendPointDto.builder()
                        .metricName(m.getMetricName())
                        .value(m.getMetricValue())
                        .timestamp(m.getTimestamp())
                        .build())
                .collect(Collectors.toList());

        return CpuMetricsDto.builder()
                .systemCpuUsage(systemCpu)
                .processCpuUsage(processCpu)
                .systemCpuTrend(systemTrend)
                .processCpuTrend(processTrend)
                .collectedAt(metrics.isEmpty() ? Instant.now() : metrics.get(0).getTimestamp())
                .build();
    }

    private MemoryMetricsDto buildMemoryMetrics(List<ServiceMetrics> metrics, int hours) {
        Map<String, List<ServiceMetrics>> byName = groupByName(metrics);

        Double heapUsed = getLatestValue(byName, "jvm.heap.used");
        Double heapMax = getLatestValue(byName, "jvm.heap.max");
        Double heapCommitted = getLatestValue(byName, "jvm.heap.committed");
        Double heapPercent = getLatestValue(byName, "jvm.heap.usage.percent");

        if (heapPercent == null && heapUsed != null && heapMax != null && heapMax > 0) {
            heapPercent = (heapUsed / heapMax) * 100.0;
        }

        List<MetricTrendPointDto> heapUsedTrend = byName.getOrDefault("jvm.heap.used", List.of()).stream()
                .map(m -> MetricTrendPointDto.builder()
                        .metricName(m.getMetricName())
                        .value(m.getMetricValue())
                        .timestamp(m.getTimestamp())
                        .build())
                .collect(Collectors.toList());

        List<MetricTrendPointDto> heapPercentTrend = byName.getOrDefault("jvm.heap.usage.percent", List.of()).stream()
                .map(m -> MetricTrendPointDto.builder()
                        .metricName(m.getMetricName())
                        .value(m.getMetricValue())
                        .timestamp(m.getTimestamp())
                        .build())
                .collect(Collectors.toList());

        return MemoryMetricsDto.builder()
                .heapUsedBytes(heapUsed)
                .heapMaxBytes(heapMax)
                .heapCommittedBytes(heapCommitted)
                .heapUsagePercent(heapPercent)
                .heapUsedTrend(heapUsedTrend)
                .heapUsageTrend(heapPercentTrend)
                .collectedAt(metrics.isEmpty() ? Instant.now() : metrics.get(0).getTimestamp())
                .build();
    }

    private LatencyMetricsDto buildLatencyMetrics(List<ServiceMetrics> metrics, int hours) {
        Map<String, List<ServiceMetrics>> byName = groupByName(metrics);

        Double avgDuration = getLatestValue(byName, "http.request.duration.avg");
        Double maxDuration = getLatestValue(byName, "http.request.duration.max");
        Long totalRequests = getLatestLongValue(byName, "http.request.count");

        List<MetricTrendPointDto> avgTrend = byName.getOrDefault("http.request.duration.avg", List.of()).stream()
                .map(m -> MetricTrendPointDto.builder()
                        .metricName(m.getMetricName())
                        .value(m.getMetricValue())
                        .timestamp(m.getTimestamp())
                        .build())
                .collect(Collectors.toList());

        List<MetricTrendPointDto> maxTrend = byName.getOrDefault("http.request.duration.max", List.of()).stream()
                .map(m -> MetricTrendPointDto.builder()
                        .metricName(m.getMetricName())
                        .value(m.getMetricValue())
                        .timestamp(m.getTimestamp())
                        .build())
                .collect(Collectors.toList());

        return LatencyMetricsDto.builder()
                .avgDurationSeconds(avgDuration)
                .maxDurationSeconds(maxDuration)
                .totalRequests(totalRequests)
                .avgDurationTrend(avgTrend)
                .maxDurationTrend(maxTrend)
                .collectedAt(metrics.isEmpty() ? Instant.now() : metrics.get(0).getTimestamp())
                .build();
    }

    private RequestMetricsDto buildRequestMetrics(List<ServiceMetrics> metrics, int hours) {
        Map<String, List<ServiceMetrics>> byName = groupByName(metrics);

        Long totalRequests = getLatestLongValue(byName, "http.request.count");
        Long threadsBusy = getLatestLongValue(byName, "tomcat.threads.busy");
        Long threadsCurrent = getLatestLongValue(byName, "tomcat.threads.current");
        Long hikariActive = getLatestLongValue(byName, "hikaricp.connections.active");
        Long hikariIdle = getLatestLongValue(byName, "hikaricp.connections.idle");
        Long hikariPending = getLatestLongValue(byName, "hikaricp.connections.pending");

        Double requestsPerSecond = null;
        if (totalRequests != null && hours > 0) {
            requestsPerSecond = (double) totalRequests / (hours * 3600.0);
        }

        List<MetricTrendPointDto> requestTrend = byName.getOrDefault("http.request.count", List.of()).stream()
                .map(m -> MetricTrendPointDto.builder()
                        .metricName(m.getMetricName())
                        .value(m.getMetricValue())
                        .timestamp(m.getTimestamp())
                        .build())
                .collect(Collectors.toList());

        return RequestMetricsDto.builder()
                .totalRequests(totalRequests)
                .requestsPerSecond(requestsPerSecond)
                .activeThreadsBusy(threadsBusy)
                .activeThreadsCurrent(threadsCurrent)
                .hikariConnectionsActive(hikariActive)
                .hikariConnectionsIdle(hikariIdle)
                .hikariConnectionsPending(hikariPending)
                .requestCountTrend(requestTrend)
                .collectedAt(metrics.isEmpty() ? Instant.now() : metrics.get(0).getTimestamp())
                .build();
    }

    private Map<String, List<MetricTrendPointDto>> buildAllMetricTrends(List<ServiceMetrics> allMetrics) {
        return allMetrics.stream()
                .collect(Collectors.groupingBy(
                        ServiceMetrics::getMetricName,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                m -> MetricTrendPointDto.builder()
                                        .metricName(m.getMetricName())
                                        .value(m.getMetricValue())
                                        .timestamp(m.getTimestamp())
                                        .build(),
                                Collectors.toList()
                        )
                ));
    }

    private Map<String, List<ServiceMetrics>> groupByName(List<ServiceMetrics> metrics) {
        return metrics.stream()
                .collect(Collectors.groupingBy(
                        ServiceMetrics::getMetricName,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private Double getLatestValue(Map<String, List<ServiceMetrics>> byName, String metricName) {
        List<ServiceMetrics> values = byName.get(metricName);
        if (values == null || values.isEmpty()) return null;
        return values.get(0).getMetricValue();
    }

    private Long getLatestLongValue(Map<String, List<ServiceMetrics>> byName, String metricName) {
        Double val = getLatestValue(byName, metricName);
        return val != null ? val.longValue() : null;
    }
}
