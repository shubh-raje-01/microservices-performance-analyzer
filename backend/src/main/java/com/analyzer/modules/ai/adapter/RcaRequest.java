package com.analyzer.modules.ai.adapter;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RcaRequest {

    private final String serviceId;
    private final String serviceName;

    private final CpuMetrics cpu;
    private final MemoryMetrics memory;
    private final LatencyMetrics latency;
    private final GcMetrics gc;
    private final ThreadMetrics threads;
    private final HttpErrorMetrics httpErrors;
    private final ConnectionPoolMetrics connectionPool;
    private final HealthStatus health;
    private final List<HistoricalMetricPoint> historicalMetrics;
    private final int hoursBack;

    @Getter
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class CpuMetrics {
        private final double systemCpuUsage;
        private final double processCpuUsage;
    }

    @Getter
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class MemoryMetrics {
        private final double heapUsedBytes;
        private final double heapMaxBytes;
        private final double heapCommittedBytes;
        private final double heapUsagePercent;
    }

    @Getter
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class LatencyMetrics {
        private final double avgDurationSeconds;
        private final double maxDurationSeconds;
        private final int totalRequests;
    }

    @Getter
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class GcMetrics {
        private final double gcPauseSumSeconds;
        private final int gcPauseCount;
        private final double gcLiveDataBytes;
        private final double gcMaxDataBytes;
    }

    @Getter
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class ThreadMetrics {
        private final int tomcatThreadsBusy;
        private final int tomcatThreadsCurrent;
    }

    @Getter
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class HttpErrorMetrics {
        private final int totalRequests;
        private final int errorRequests;
        private final double errorRate;
    }

    @Getter
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class ConnectionPoolMetrics {
        private final int hikariActive;
        private final int hikariIdle;
        private final int hikariPending;
        private final int hikariMaxPoolSize;
    }

    @Getter
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class HealthStatus {
        private final String status;
        private final double latencyMs;
        private final int responseCode;
    }

    @Getter
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class HistoricalMetricPoint {
        private final String timestamp;
        private final String metricName;
        private final double value;
    }
}
