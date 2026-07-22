package com.analyzer.service_registry.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceMetricsSummaryDto {

    private final String serviceId;
    private final String serviceName;
    private final CpuMetricsDto cpu;
    private final MemoryMetricsDto memory;
    private final LatencyMetricsDto latency;
    private final RequestMetricsDto requests;
    private final Map<String, List<MetricTrendPointDto>> allMetricTrends;
    private final Instant collectedAt;
    private final int hoursBack;
}
