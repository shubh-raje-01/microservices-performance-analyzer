package com.analyzer.service_registry.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemoryMetricsDto {

    private final Double heapUsedBytes;
    private final Double heapMaxBytes;
    private final Double heapCommittedBytes;
    private final Double heapUsagePercent;
    private final List<MetricTrendPointDto> heapUsedTrend;
    private final List<MetricTrendPointDto> heapUsageTrend;
    private final Instant collectedAt;
}
