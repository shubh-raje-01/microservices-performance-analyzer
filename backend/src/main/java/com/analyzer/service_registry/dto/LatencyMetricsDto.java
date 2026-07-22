package com.analyzer.service_registry.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LatencyMetricsDto {

    private final Double avgDurationSeconds;
    private final Double maxDurationSeconds;
    private final Long totalRequests;
    private final List<MetricTrendPointDto> avgDurationTrend;
    private final List<MetricTrendPointDto> maxDurationTrend;
    private final Instant collectedAt;
}
