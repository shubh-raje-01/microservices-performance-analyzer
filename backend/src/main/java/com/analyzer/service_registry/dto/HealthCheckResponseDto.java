package com.analyzer.service_registry.dto;

import com.analyzer.service_registry.model.ServiceStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HealthCheckResponseDto {

    private final Long id;
    private final String serviceId;
    private final String serviceName;
    private final ServiceStatus status;
    private final Long latencyMs;
    private final Integer responseCode;
    private final String errorMessage;
    private final Instant checkTime;
}
