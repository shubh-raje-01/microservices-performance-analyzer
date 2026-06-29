package com.analyzer.backend.modules.ai.ml;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class AIFeatureSet {

    private final Long simulationId;
    private final String serviceName;

    // ── Latency features
    private final double avgLatencyMs;
    private final double p50LatencyMs;
    private final double p95LatencyMs;
    private final double p99LatencyMs;
    private final double maxLatencyMs;
    private final double stdDevLatencyMs;

    /** p99/p50 ratio — high values indicate a problematic long tail */
    private final double tailLatencyRatio;

    // ── Throughput features
    private final double avgThroughputRps;
    private final double peakThroughputRps;

    /** peak/avg — large spread signals bursty traffic */
    private final double throughputVariability;

    // ── Error features
    private final double avgErrorRate;
    private final double maxErrorRate;
    private final long totalRequests;
    private final long totalFailedRequests;

    /** Exceeded the scenario's configured threshold */
    private final boolean errorThresholdBreached;

    // ── Health composite
    private final double healthScore;
    private final String healthStatus;
    private final String worstMetricSeverity;

    // ── Log-derived signals
    private final long totalLogCount;
    private final long errorLogCount;
    private final long warnLogCount;
    private final boolean hasLogErrors;
    private final boolean logThresholdBreached;
    private final Map<String, Long> logCountByLevel;
    private final Map<String, Long> logCountByCategory;

    // ── Simulation config
    private final int durationSeconds;
    private final int concurrentUsers;
    private final double errorRateThreshold;
    private final String scenarioName;
}