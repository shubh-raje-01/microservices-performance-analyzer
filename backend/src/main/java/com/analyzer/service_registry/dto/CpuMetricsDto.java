package com.analyzer.service_registry.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CpuMetricsDto {

    private final Double systemCpuUsage;
    private final Double processCpuUsage;
    private final List<MetricTrendPointDto> systemCpuTrend;
    private final List<MetricTrendPointDto> processCpuTrend;
    private final Instant collectedAt;
}
