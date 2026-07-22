package com.analyzer.service_registry.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetricTrendPointDto {

    private final String metricName;
    private final Double value;
    private final String unit;
    private final Instant timestamp;
}
