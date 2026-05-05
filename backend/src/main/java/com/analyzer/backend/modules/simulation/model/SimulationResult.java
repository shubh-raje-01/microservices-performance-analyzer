package com.analyzer.backend.modules.simulation.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SimulationResult {

    private final double avgLatencyMs;
    private final double p95LatencyMs;
    private final double p99LatencyMs;
    private final double throughputRps;
    private final double actualErrorRate;
    private final long totalRequests;
    private final long failedRequests;

}
