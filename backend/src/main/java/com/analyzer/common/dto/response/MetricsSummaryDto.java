package com.analyzer.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetricsSummaryDto {

    private final Long simulationId;
    private final String targetService;

    // Latency
    private final Double avgLatencyMs;
    private final Double p50LatencyMs;
    private final Double p95LatencyMs;
    private final Double p99LatencyMs;
    private final Double maxLatencyMs;

    // Throughput
    private final Double throughputRps;
    private final Long totalRequests;
    private final Long failedRequests;

    // Error rate — [0.0, 1.0]
    private final Double errorRate;

    // Derived health signal
    private final String healthStatus;   // "HEALTHY" | "DEGRADED" | "CRITICAL"

    public static String deriveHealth(double errorRate, double p95LatencyMs) {
        if (errorRate > 0.10 || p95LatencyMs > 2000) return "CRITICAL";
        if (errorRate > 0.02 || p95LatencyMs > 500)  return "DEGRADED";
        return "HEALTHY";
    }

}
