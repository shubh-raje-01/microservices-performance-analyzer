package com.analyzer.backend.modules.simulation.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SimulationResponseDto {

    private final Long id;
    private final String scenarioName;
    private final String targetService;
    private final int durationSeconds;
    private final int concurrentUsers;
    private final double errorRateThreshold;
    private final String status;

    // Only populated once COMPLETED
    private final Double avgLatencyMs;
    private final Double p95LatencyMs;
    private final Double p99LatencyMs;
    private final Double throughputRps;
    private final Double actualErrorRate;
    private final Long totalRequests;
    private final Long failedRequests;

    // Only populated on FAILED
    private final String failureReason;

    private final Instant startedAt;
    private final Instant completedAt;
    private final Instant createdAt;
}