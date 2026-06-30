package com.analyzer.modules.metrics.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetricSnapshotDto {

    private final Long id;
    private final Long simulationId;
    private final String serviceName;
    private final String metricType;
    private final Double value;
    private final String unit;

    // Populated only for LATENCY type
    private final Double avgMs;
    private final Double p95Ms;
    private final Double p99Ms;
    private final Double maxMs;

    // Populated for ERROR_RATE and AVAILABILITY
    private final Long totalRequests;
    private final Long failedRequests;

    private final String severity;
    private final String notes;
    private final Instant recordedAt;
}