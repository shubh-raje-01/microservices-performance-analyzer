package com.analyzer.backend.modules.ai.adapter;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FastAPIRequest {

    private final Long simulationId;
    private final String serviceName;

    private final MetricsPayload metrics;
    private final LogsPayload logs;
    private final SimConfigPayload simulationConfig;

    // ── Nested payloads =>

    @Getter
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class MetricsPayload {
        private final double avgLatencyMs;
        private final double p50LatencyMs;
        private final double p95LatencyMs;
        private final double p99LatencyMs;
        private final double maxLatencyMs;
        private final double stdDevLatencyMs;
        private final double tailLatencyRatio;
        private final double avgThroughputRps;
        private final double peakThroughputRps;
        private final double throughputVariability;
        private final double avgErrorRate;
        private final double maxErrorRate;
        private final long totalRequests;
        private final long totalFailedRequests;
        private final double healthScore;
        private final String healthStatus;
        private final String worstSeverity;
    }

    @Getter
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class LogsPayload {
        private final long totalCount;
        private final long errorCount;
        private final long warnCount;
        private final boolean hasErrors;
        private final boolean thresholdBreached;
        private final Map<String, Long> countByLevel;
        private final Map<String, Long> countByCategory;
    }

    @Getter
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class SimConfigPayload {
        private final int durationSeconds;
        private final int concurrentUsers;
        private final double errorRateThreshold;
        private final String scenarioName;
    }
}