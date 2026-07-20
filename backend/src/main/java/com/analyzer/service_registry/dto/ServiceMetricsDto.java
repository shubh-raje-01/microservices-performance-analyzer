package com.analyzer.service_registry.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceMetricsDto {

    private final Long id;
    private final String serviceId;
    private final String serviceName;
    private final String metricName;
    private final Double metricValue;
    private final String metricType;
    private final String metricLabels;
    private final Instant timestamp;
}
