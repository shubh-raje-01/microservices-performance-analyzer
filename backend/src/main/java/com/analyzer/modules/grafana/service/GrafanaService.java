package com.analyzer.modules.grafana.service;

import com.analyzer.modules.grafana.dto.GrafanaWidgetData;
import com.analyzer.modules.grafana.dto.GrafanaWidgetData.*;
import com.analyzer.modules.grafana.websocket.GrafanaWebSocketHandler;
import com.analyzer.service_registry.model.Service;
import com.analyzer.service_registry.model.ServiceHealthHistory;
import com.analyzer.service_registry.model.ServiceMetrics;
import com.analyzer.service_registry.model.ServiceStatus;
import com.analyzer.service_registry.repository.ServiceHealthHistoryRepository;
import com.analyzer.service_registry.repository.ServiceMetricsRepository;
import com.analyzer.service_registry.repository.ServiceRepository;
import com.analyzer.modules.tracing.model.TraceSpan;
import com.analyzer.modules.tracing.repository.TraceSpanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core service that assembles Grafana widget data and broadcasts updates.
 *
 * Architecture decisions:
 * - Builds all widget data in a single method to minimize DB round-trips.
 * - Each widget builder is wrapped in try-catch for graceful degradation:
 *   if one widget fails, the rest still render.
 * - @Transactional(readOnly=true) ensures a consistent snapshot across
 *   multiple repository reads within the same transaction.
 * - Precomputes maps (serviceById, historyByServiceId) to avoid O(n*m) scans
 *   and N+1 query patterns in widget builders.
 * - The Prometheus endpoint is served separately in the controller to avoid
 *   coupling text-format serialization with the JSON widget assembly.
 */
@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class GrafanaService {

    private final ServiceRepository serviceRepository;
    private final ServiceHealthHistoryRepository healthHistoryRepository;
    private final ServiceMetricsRepository serviceMetricsRepository;
    private final TraceSpanRepository traceSpanRepository;
    private final GrafanaWebSocketHandler webSocketHandler;

    /**
     * Assembles the complete widget payload for the Grafana dashboard.
     * Each widget section is built independently with exception isolation:
     * if one widget fails, the rest still render with null for the failed section.
     */
    @Transactional(readOnly = true)
    public GrafanaWidgetData buildAllWidgets(int historyHours) {
        Instant since = Instant.now().minusSeconds(historyHours * 3600L);
        List<Service> enabledServices = serviceRepository.findAllEnabled();
        List<ServiceHealthHistory> recentHistory = healthHistoryRepository.findRecentAll(since);

        // Precompute lookup maps to avoid O(n*m) scans in widget builders
        Map<String, Service> serviceById = enabledServices.stream()
                .collect(Collectors.toMap(Service::getId, s -> s));
        Map<String, List<ServiceHealthHistory>> historyByServiceId = recentHistory.stream()
                .collect(Collectors.groupingBy(h -> h.getService().getId()));

        return GrafanaWidgetData.builder()
                .serviceHealth(safeBuild("serviceHealth", () ->
                        buildServiceHealth(enabledServices, historyByServiceId)))
                .cpu(safeBuild("cpu", () -> buildCpuWidget(enabledServices, since)))
                .memory(safeBuild("memory", () -> buildMemoryWidget(enabledServices, since)))
                .latency(safeBuild("latency", () -> buildLatencyWidget(recentHistory)))
                .requestRate(safeBuild("requestRate", () ->
                        buildRequestRateWidget(enabledServices, since)))
                .errorRate(safeBuild("errorRate", () -> buildErrorRateWidget(recentHistory)))
                .topSlowest(safeBuild("topSlowest", () ->
                        buildTopSlowestWidget(historyByServiceId, serviceById)))
                .recentFailures(safeBuild("recentFailures", () ->
                        buildRecentFailures(recentHistory)))
                .dependencyGraph(safeBuild("dependencyGraph", this::buildDependencyGraph))
                .generatedAt(Instant.now())
                .build();
    }

    /**
     * Returns Prometheus-format metrics text for the configured time window.
     * Delegates to the service layer rather than querying repositories directly.
     */
    @Transactional(readOnly = true)
    public String buildPrometheusMetrics(int hours) {
        Instant since = Instant.now().minusSeconds(hours * 3600L);
        List<Service> services = serviceRepository.findAllEnabled();
        List<ServiceHealthHistory> recentHistory = healthHistoryRepository.findRecentAll(since);

        // Precompute per-service metrics to avoid N+1 queries
        Map<String, Double> avgLatencyByService = new HashMap<>();
        Map<String, Double> cpuByService = new HashMap<>();
        Map<String, Double> memoryUsedByService = new HashMap<>();
        Map<String, Double> memoryMaxByService = new HashMap<>();

        for (Service s : services) {
            Double avgLat = healthHistoryRepository.averageLatencyByService(s.getId(), since);
            if (avgLat != null) avgLatencyByService.put(s.getId(), avgLat);

            List<ServiceMetrics> cpuMetrics = serviceMetricsRepository
                    .findMetricTrend(s.getId(), "system.cpu.usage", since);
            if (!cpuMetrics.isEmpty()) {
                cpuByService.put(s.getId(),
                        cpuMetrics.get(cpuMetrics.size() - 1).getMetricValue() * 100);
            }

            List<ServiceMetrics> heapUsed = serviceMetricsRepository
                    .findMetricTrend(s.getId(), "jvm.heap.used", since);
            if (!heapUsed.isEmpty()) {
                memoryUsedByService.put(s.getId(),
                        heapUsed.get(heapUsed.size() - 1).getMetricValue());
            }

            List<ServiceMetrics> heapMax = serviceMetricsRepository
                    .findMetricTrend(s.getId(), "jvm.heap.max", since);
            if (!heapMax.isEmpty()) {
                memoryMaxByService.put(s.getId(),
                        heapMax.get(heapMax.size() - 1).getMetricValue());
            }
        }

        // Precompute failure counts
        Map<String, Long> failureCounts = new HashMap<>();
        for (Service s : services) {
            long failures = recentHistory.stream()
                    .filter(h -> h.getService().getId().equals(s.getId())
                            && h.getStatus() == ServiceStatus.OFFLINE)
                    .count();
            failureCounts.put(s.getId(), failures);
        }

        long online = services.stream().filter(s -> s.getStatus() == ServiceStatus.ONLINE).count();
        long degraded = services.stream().filter(s -> s.getStatus() == ServiceStatus.DEGRADED).count();
        long offline = services.stream().filter(s -> s.getStatus() == ServiceStatus.OFFLINE).count();

        StringBuilder sb = new StringBuilder(2048);

        // Service Health
        sb.append("# HELP microservice_analyzer_service_health_status Current health status (1=online, 0.5=degraded, 0=offline)\n");
        sb.append("# TYPE microservice_analyzer_service_health_status gauge\n");
        for (Service s : services) {
            double value = switch (s.getStatus()) {
                case ONLINE -> 1.0;
                case DEGRADED -> 0.5;
                case OFFLINE -> 0.0;
                default -> -1.0;
            };
            sb.append(String.format("microservice_analyzer_service_health_status{service_name=\"%s\",status=\"%s\"} %.2f%n",
                    escapeLabel(s.getName()), s.getStatus().name(), value));
        }

        // Service Latency
        sb.append("\n# HELP microservice_service_latency_ms Average service latency in milliseconds\n");
        sb.append("# TYPE microservice_service_latency_ms gauge\n");
        for (Service s : services) {
            Double avgLatency = avgLatencyByService.get(s.getId());
            if (avgLatency != null) {
                sb.append(String.format("microservice_service_latency_ms{service_name=\"%s\"} %.2f%n",
                        escapeLabel(s.getName()), avgLatency));
            }
        }

        // CPU Usage
        sb.append("\n# HELP microservice_cpu_usage_percent Current CPU usage percentage\n");
        sb.append("# TYPE microservice_cpu_usage_percent gauge\n");
        for (Service s : services) {
            Double cpu = cpuByService.get(s.getId());
            if (cpu != null) {
                sb.append(String.format("microservice_cpu_usage_percent{service_name=\"%s\"} %.2f%n",
                        escapeLabel(s.getName()), cpu));
            }
        }

        // Memory Usage
        sb.append("\n# HELP microservice_memory_usage_bytes Current JVM heap memory usage in bytes\n");
        sb.append("# TYPE microservice_memory_usage_bytes gauge\n");
        for (Service s : services) {
            Double used = memoryUsedByService.get(s.getId());
            if (used != null) {
                sb.append(String.format("microservice_memory_usage_bytes{service_name=\"%s\"} %.0f%n",
                        escapeLabel(s.getName()), used));
            }
        }

        sb.append("\n# HELP microservice_memory_max_bytes Maximum JVM heap memory in bytes\n");
        sb.append("# TYPE microservice_memory_max_bytes gauge\n");
        for (Service s : services) {
            Double max = memoryMaxByService.get(s.getId());
            if (max != null) {
                sb.append(String.format("microservice_memory_max_bytes{service_name=\"%s\"} %.0f%n",
                        escapeLabel(s.getName()), max));
            }
        }

        // Health Check Failures
        sb.append("\n# HELP microservice_health_failures_total Total health check failures\n");
        sb.append("# TYPE microservice_health_failures_total counter\n");
        for (Service s : services) {
            sb.append(String.format("microservice_health_failures_total{service_name=\"%s\"} %d%n",
                    escapeLabel(s.getName()), failureCounts.getOrDefault(s.getId(), 0L)));
        }

        // Summary counts
        sb.append("\n# HELP microservice_analyzer_active_services Number of active monitored services\n");
        sb.append("# TYPE microservice_analyzer_active_services gauge\n");
        sb.append(String.format("microservice_analyzer_active_services %d%n", services.size()));

        sb.append("\n# HELP microservice_analyzer_services_by_status Number of services in each status\n");
        sb.append("# TYPE microservice_analyzer_services_by_status gauge\n");
        sb.append(String.format("microservice_analyzer_services_by_status{status=\"online\"} %d%n", online));
        sb.append(String.format("microservice_analyzer_services_by_status{status=\"degraded\"} %d%n", degraded));
        sb.append(String.format("microservice_analyzer_services_by_status{status=\"offline\"} %d%n", offline));

        return sb.toString();
    }

    // ─── Widget Builders ──────────────────────────────────────────────

    private ServiceHealthWidget buildServiceHealth(
            List<Service> enabledServices,
            Map<String, List<ServiceHealthHistory>> historyByServiceId) {

        long online = enabledServices.stream()
                .filter(s -> s.getStatus() == ServiceStatus.ONLINE).count();
        long degraded = enabledServices.stream()
                .filter(s -> s.getStatus() == ServiceStatus.DEGRADED).count();
        long offline = enabledServices.stream()
                .filter(s -> s.getStatus() == ServiceStatus.OFFLINE).count();

        List<ServiceHealthEntry> entries = enabledServices.stream()
                .map(s -> {
                    List<ServiceHealthHistory> serviceHistory =
                            historyByServiceId.getOrDefault(s.getId(), List.of());
                    ServiceHealthHistory latest = serviceHistory.isEmpty()
                            ? null : serviceHistory.get(0);
                    return ServiceHealthEntry.builder()
                            .serviceId(s.getId())
                            .serviceName(s.getName())
                            .status(s.getStatus().name())
                            .latencyMs(latest != null ? latest.getLatencyMs() : null)
                            .lastHeartbeat(s.getLastHeartbeat())
                            .build();
                })
                .collect(Collectors.toList());

        return ServiceHealthWidget.builder()
                .totalServices(enabledServices.size())
                .onlineCount(online)
                .degradedCount(degraded)
                .offlineCount(offline)
                .services(entries)
                .build();
    }

    private CpuWidget buildCpuWidget(List<Service> enabledServices, Instant since) {
        List<ServiceCpuEntry> perService = new ArrayList<>();
        List<MetricTrendPoint> allTrendPoints = new ArrayList<>();

        for (Service service : enabledServices) {
            List<ServiceMetrics> cpuMetrics = serviceMetricsRepository
                    .findMetricTrend(service.getId(), "system.cpu.usage", since);

            double latest = cpuMetrics.isEmpty() ? 0 :
                    cpuMetrics.get(cpuMetrics.size() - 1).getMetricValue() * 100;
            perService.add(ServiceCpuEntry.builder()
                    .serviceId(service.getId())
                    .serviceName(service.getName())
                    .usagePercent(Math.round(latest * 100.0) / 100.0)
                    .build());

            cpuMetrics.forEach(m -> allTrendPoints.add(MetricTrendPoint.builder()
                    .time(m.getTimestamp())
                    .value(m.getMetricValue() * 100)
                    .build()));
        }

        double avg = perService.isEmpty() ? 0 :
                perService.stream().mapToDouble(ServiceCpuEntry::getUsagePercent).average().orElse(0);
        double max = perService.isEmpty() ? 0 :
                perService.stream().mapToDouble(ServiceCpuEntry::getUsagePercent).max().orElse(0);

        return CpuWidget.builder()
                .averageUsagePercent(Math.round(avg * 100.0) / 100.0)
                .maxUsagePercent(Math.round(max * 100.0) / 100.0)
                .trend(allTrendPoints.stream()
                        .sorted(Comparator.comparing(MetricTrendPoint::getTime))
                        .toList())
                .perService(perService)
                .build();
    }

    private MemoryWidget buildMemoryWidget(List<Service> enabledServices, Instant since) {
        List<ServiceMemoryEntry> perService = new ArrayList<>();
        List<MetricTrendPoint> allTrendPoints = new ArrayList<>();

        for (Service service : enabledServices) {
            List<ServiceMetrics> heapUsed = serviceMetricsRepository
                    .findMetricTrend(service.getId(), "jvm.heap.used", since);
            List<ServiceMetrics> heapMax = serviceMetricsRepository
                    .findMetricTrend(service.getId(), "jvm.heap.max", since);

            double used = heapUsed.isEmpty() ? 0 :
                    heapUsed.get(heapUsed.size() - 1).getMetricValue();
            double max = heapMax.isEmpty() ? 1 :
                    heapMax.get(heapMax.size() - 1).getMetricValue();
            double pct = max > 0 ? (used / max) * 100 : 0;

            perService.add(ServiceMemoryEntry.builder()
                    .serviceId(service.getId())
                    .serviceName(service.getName())
                    .usagePercent(Math.round(pct * 100.0) / 100.0)
                    .usedBytes((long) used)
                    .maxBytes((long) max)
                    .build());

            heapUsed.forEach(m -> allTrendPoints.add(MetricTrendPoint.builder()
                    .time(m.getTimestamp())
                    .value(pct)
                    .build()));
        }

        double avg = perService.isEmpty() ? 0 :
                perService.stream().mapToDouble(ServiceMemoryEntry::getUsagePercent).average().orElse(0);
        double maxPct = perService.isEmpty() ? 0 :
                perService.stream().mapToDouble(ServiceMemoryEntry::getUsagePercent).max().orElse(0);

        return MemoryWidget.builder()
                .averageUsagePercent(Math.round(avg * 100.0) / 100.0)
                .maxUsagePercent(Math.round(maxPct * 100.0) / 100.0)
                .totalUsedBytes(perService.stream().mapToLong(ServiceMemoryEntry::getUsedBytes).sum())
                .totalMaxBytes(perService.stream().mapToLong(ServiceMemoryEntry::getMaxBytes).sum())
                .trend(allTrendPoints.stream()
                        .sorted(Comparator.comparing(MetricTrendPoint::getTime))
                        .toList())
                .perService(perService)
                .build();
    }

    private LatencyWidget buildLatencyWidget(List<ServiceHealthHistory> recentHistory) {
        List<Double> latencies = recentHistory.stream()
                .filter(h -> h.getLatencyMs() != null)
                .map(h -> h.getLatencyMs().doubleValue())
                .sorted()
                .collect(Collectors.toList());

        List<MetricTrendPoint> trend = recentHistory.stream()
                .filter(h -> h.getLatencyMs() != null)
                .sorted(Comparator.comparing(ServiceHealthHistory::getCheckTime))
                .map(h -> MetricTrendPoint.builder()
                        .time(h.getCheckTime())
                        .value(h.getLatencyMs())
                        .build())
                .toList();

        return LatencyWidget.builder()
                .averageMs(average(latencies))
                .p50Ms(percentile(latencies, 50))
                .p95Ms(percentile(latencies, 95))
                .p99Ms(percentile(latencies, 99))
                .maxMs(latencies.isEmpty() ? 0 : latencies.get(latencies.size() - 1))
                .trend(trend)
                .build();
    }

    private RequestRateWidget buildRequestRateWidget(List<Service> enabledServices, Instant since) {
        long totalRequests = 0;
        long errorCount = 0;
        List<MetricTrendPoint> trend = new ArrayList<>();

        for (Service service : enabledServices) {
            List<ServiceMetrics> httpRequests = serviceMetricsRepository
                    .findMetricTrend(service.getId(), "http.server.requests", since);

            for (ServiceMetrics m : httpRequests) {
                totalRequests += m.getMetricValue().longValue();
                trend.add(MetricTrendPoint.builder()
                        .time(m.getTimestamp())
                        .value(m.getMetricValue())
                        .build());
            }

            List<ServiceMetrics> httpErrors = serviceMetricsRepository
                    .findMetricTrend(service.getId(), "http.server.errors", since);
            for (ServiceMetrics m : httpErrors) {
                errorCount += m.getMetricValue().longValue();
            }
        }

        double rps = trend.isEmpty() ? 0 :
                trend.stream().mapToDouble(MetricTrendPoint::getValue).average().orElse(0);

        return RequestRateWidget.builder()
                .totalRps(Math.round(rps * 100.0) / 100.0)
                .totalRequests(totalRequests)
                .successfulRequests(totalRequests - errorCount)
                .trend(trend.stream()
                        .sorted(Comparator.comparing(MetricTrendPoint::getTime))
                        .toList())
                .build();
    }

    private ErrorRateWidget buildErrorRateWidget(List<ServiceHealthHistory> recentHistory) {
        long totalChecks = recentHistory.size();
        long failures = recentHistory.stream()
                .filter(h -> h.getStatus() == ServiceStatus.OFFLINE).count();
        double errorRate = totalChecks > 0 ? ((double) failures / totalChecks) * 100 : 0;

        List<MetricTrendPoint> trend = recentHistory.stream()
                .sorted(Comparator.comparing(ServiceHealthHistory::getCheckTime))
                .map(h -> MetricTrendPoint.builder()
                        .time(h.getCheckTime())
                        .value(h.getStatus() == ServiceStatus.OFFLINE ? 1 : 0)
                        .build())
                .toList();

        return ErrorRateWidget.builder()
                .errorRatePercent(Math.round(errorRate * 100.0) / 100.0)
                .totalErrors(failures)
                .totalRequests(totalChecks)
                .trend(trend)
                .build();
    }

    private TopSlowestWidget buildTopSlowestWidget(
            Map<String, List<ServiceHealthHistory>> historyByServiceId,
            Map<String, Service> serviceById) {

        List<TopSlowestEntry> entries = historyByServiceId.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .map(e -> {
                    double avg = e.getValue().stream()
                            .filter(h -> h.getLatencyMs() != null)
                            .mapToLong(ServiceHealthHistory::getLatencyMs)
                            .average().orElse(0);
                    Service service = serviceById.get(e.getKey());
                    return TopSlowestEntry.builder()
                            .serviceId(e.getKey())
                            .serviceName(service != null ? service.getName() : e.getKey())
                            .averageLatencyMs(Math.round(avg * 100.0) / 100.0)
                            .checkCount(e.getValue().size())
                            .build();
                })
                .sorted(Comparator.comparingDouble(TopSlowestEntry::getAverageLatencyMs).reversed())
                .limit(10)
                .toList();

        return TopSlowestWidget.builder().services(entries).build();
    }

    private RecentFailuresWidget buildRecentFailures(List<ServiceHealthHistory> recentHistory) {
        List<FailureEntry> failures = recentHistory.stream()
                .filter(h -> h.getStatus() == ServiceStatus.OFFLINE && h.getErrorMessage() != null)
                .sorted(Comparator.comparing(ServiceHealthHistory::getCheckTime).reversed())
                .limit(20)
                .map(h -> FailureEntry.builder()
                        .serviceId(h.getService().getId())
                        .serviceName(h.getService().getName())
                        .errorMessage(h.getErrorMessage())
                        .occurredAt(h.getCheckTime())
                        .build())
                .toList();

        return RecentFailuresWidget.builder()
                .failures(failures)
                .totalFailures(failures.size())
                .build();
    }

    private DependencyGraphWidget buildDependencyGraph() {
        Instant since = Instant.now().minusSeconds(24 * 3600L);
        // Limit to 5000 spans to prevent unbounded memory usage
        List<TraceSpan> recentSpans = traceSpanRepository.findRecentSpansBounded(since, 5000);

        if (recentSpans.isEmpty()) {
            return DependencyGraphWidget.builder()
                    .nodes(List.of())
                    .edges(List.of())
                    .build();
        }

        // Build nodes from distinct service names
        Map<String, List<TraceSpan>> spansByService = recentSpans.stream()
                .collect(Collectors.groupingBy(TraceSpan::getServiceName));

        List<GraphNode> nodes = spansByService.entrySet().stream()
                .map(e -> GraphNode.builder()
                        .serviceName(e.getKey())
                        .status(e.getValue().stream()
                                .anyMatch(s -> "ERROR".equals(s.getStatusCode())) ? "ERROR" : "OK")
                        .avgLatencyMs(e.getValue().stream()
                                .mapToLong(TraceSpan::getDurationMs)
                                .average().orElse(0))
                        .spanCount(e.getValue().size())
                        .build())
                .toList();

        // Build edges from parent-child span relationships
        Map<String, TraceSpan> spanById = recentSpans.stream()
                .collect(Collectors.toMap(TraceSpan::getSpanId, s -> s, (a, b) -> a));

        Map<String, GraphEdge> edgeMap = new LinkedHashMap<>();
        for (TraceSpan span : recentSpans) {
            if (span.getParentSpanId() != null) {
                TraceSpan parent = spanById.get(span.getParentSpanId());
                if (parent != null && !parent.getServiceName().equals(span.getServiceName())) {
                    String key = parent.getServiceName() + "->" + span.getServiceName();
                    edgeMap.merge(key, GraphEdge.builder()
                            .source(parent.getServiceName())
                            .target(span.getServiceName())
                            .callCount(1)
                            .avgDurationMs(span.getDurationMs())
                            .build(), (existing, ignored) -> GraphEdge.builder()
                            .source(existing.getSource())
                            .target(existing.getTarget())
                            .callCount(existing.getCallCount() + 1)
                            .avgDurationMs((existing.getAvgDurationMs() * existing.getCallCount()
                                    + span.getDurationMs()) / (existing.getCallCount() + 1))
                            .build());
                }
            }
        }

        return DependencyGraphWidget.builder()
                .nodes(nodes)
                .edges(new ArrayList<>(edgeMap.values()))
                .build();
    }

    // ─── Scheduled WebSocket Broadcast ────────────────────────────────

    @Scheduled(fixedDelayString = "${monitoring.grafana.broadcast-interval:15000}", initialDelay = 10000)
    public void broadcastUpdate() {
        if (webSocketHandler.getActiveConnections() == 0) return;

        try {
            GrafanaWidgetData data = buildAllWidgets(1);
            webSocketHandler.broadcast("dashboard", data);
            log.debug("Grafana WebSocket broadcast sent (connections: {})",
                    webSocketHandler.getActiveConnections());
        } catch (Exception e) {
            log.error("Failed to broadcast Grafana update", e);
        }
    }

    // ─── Utility ──────────────────────────────────────────────────────

    /**
     * Executes a widget builder with exception isolation.
     * Returns null if the builder throws, allowing the rest of the
     * dashboard to render successfully.
     */
    private <T> T safeBuild(String widgetName, WidgetBuilder<T> builder) {
        try {
            return builder.build();
        } catch (Exception e) {
            log.error("Failed to build widget '{}': {}", widgetName, e.getMessage(), e);
            return null;
        }
    }

    private double average(List<Double> values) {
        return values.isEmpty() ? 0 :
                values.stream().mapToDouble(d -> d).average().orElse(0);
    }

    /**
     * Computes the p-th percentile from an already-sorted list.
     * Caller MUST pass a sorted list.
     */
    private double percentile(List<Double> sortedValues, int p) {
        if (sortedValues.isEmpty()) return 0;
        int index = (int) Math.ceil(p / 100.0 * sortedValues.size()) - 1;
        return sortedValues.get(Math.max(0, index));
    }

    /**
     * Escapes special characters in Prometheus label values.
     */
    private String escapeLabel(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    @FunctionalInterface
    private interface WidgetBuilder<T> {
        T build() throws Exception;
    }
}
