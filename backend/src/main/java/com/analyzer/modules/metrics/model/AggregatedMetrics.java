package com.analyzer.modules.metrics.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class AggregatedMetrics {

    private final Long simulationId;
    private final String serviceName;

    // Latency aggregates across all LATENCY snapshots
    private final Double avgLatencyMs;
    private final Double p50LatencyMs;
    private final Double p95LatencyMs;
    private final Double p99LatencyMs;
    private final Double maxLatencyMs;
    private final Double stdDevLatencyMs;

    // Throughput
    private final Double avgThroughputRps;
    private final Double peakThroughputRps;

    // Error rate
    private final Double avgErrorRate;
    private final Double maxErrorRate;
    private final Long totalRequests;
    private final Long totalFailedRequests;

    // Health
    private final String healthStatus;     // "HEALTHY" | "DEGRADED" | "CRITICAL"
    private final Double healthScore;      // 0–100
    private final MetricSeverity worstSeverity;

    // Time window
    private final Instant windowStart;
    private final Instant windowEnd;

    // Per-type breakdown — keyed by MetricType name
    private final Map<String, Long> snapshotCountByType;
    private final List<MetricSnapshot> criticalSnapshots;
}