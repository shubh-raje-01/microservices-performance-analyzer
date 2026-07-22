package com.analyzer.modules.grafana.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Unified DTO carrying all widget data in a single REST response.
 * Also used as the WebSocket broadcast payload so the frontend
 * gets the same shape regardless of the transport.
 *
 * Design rationale:
 * - A single aggregate DTO reduces the number of API calls the frontend needs.
 * - Widgets that have no data return null, which Jackson omits thanks to NON_NULL.
 * - The 'generatedAt' timestamp lets the frontend detect stale data.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GrafanaWidgetData {

    private final ServiceHealthWidget serviceHealth;
    private final CpuWidget cpu;
    private final MemoryWidget memory;
    private final LatencyWidget latency;
    private final RequestRateWidget requestRate;
    private final ErrorRateWidget errorRate;
    private final TopSlowestWidget topSlowest;
    private final RecentFailuresWidget recentFailures;
    private final DependencyGraphWidget dependencyGraph;
    private final Instant generatedAt;

    // ─── Individual Widget Shapes ─────────────────────────────────────

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ServiceHealthWidget {
        private final int totalServices;
        private final long onlineCount;
        private final long degradedCount;
        private final long offlineCount;
        private final List<ServiceHealthEntry> services;
    }

    @Getter
    @Builder
    public static class ServiceHealthEntry {
        private final String serviceId;
        private final String serviceName;
        private final String status;
        private final Long latencyMs;
        private final Instant lastHeartbeat;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CpuWidget {
        private final double averageUsagePercent;
        private final double maxUsagePercent;
        private final List<MetricTrendPoint> trend;
        private final List<ServiceCpuEntry> perService;
    }

    @Getter
    @Builder
    public static class ServiceCpuEntry {
        private final String serviceId;
        private final String serviceName;
        private final double usagePercent;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MemoryWidget {
        private final double averageUsagePercent;
        private final double maxUsagePercent;
        private final long totalUsedBytes;
        private final long totalMaxBytes;
        private final List<MetricTrendPoint> trend;
        private final List<ServiceMemoryEntry> perService;
    }

    @Getter
    @Builder
    public static class ServiceMemoryEntry {
        private final String serviceId;
        private final String serviceName;
        private final double usagePercent;
        private final long usedBytes;
        private final long maxBytes;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LatencyWidget {
        private final double averageMs;
        private final double p50Ms;
        private final double p95Ms;
        private final double p99Ms;
        private final double maxMs;
        private final List<MetricTrendPoint> trend;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RequestRateWidget {
        private final double totalRps;
        private final long totalRequests;
        private final long successfulRequests;
        private final List<MetricTrendPoint> trend;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorRateWidget {
        private final double errorRatePercent;
        private final long totalErrors;
        private final long totalRequests;
        private final List<MetricTrendPoint> trend;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TopSlowestWidget {
        private final List<TopSlowestEntry> services;
    }

    @Getter
    @Builder
    public static class TopSlowestEntry {
        private final String serviceId;
        private final String serviceName;
        private final double averageLatencyMs;
        private final long checkCount;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RecentFailuresWidget {
        private final List<FailureEntry> failures;
        private final int totalFailures;
    }

    @Getter
    @Builder
    public static class FailureEntry {
        private final String serviceId;
        private final String serviceName;
        private final String errorMessage;
        private final Instant occurredAt;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DependencyGraphWidget {
        private final List<GraphNode> nodes;
        private final List<GraphEdge> edges;
    }

    @Getter
    @Builder
    public static class GraphNode {
        private final String serviceName;
        private final String status;
        private final double avgLatencyMs;
        private final int spanCount;
    }

    @Getter
    @Builder
    public static class GraphEdge {
        private final String source;
        private final String target;
        private final int callCount;
        private final double avgDurationMs;
    }

    @Getter
    @Builder
    public static class MetricTrendPoint {
        private final Instant time;
        private final double value;
    }
}
