package com.analyzer.service_registry.dto;

import com.analyzer.service_registry.model.ServiceStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Set;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceResponseDto {

    private final String id;
    private final String name;
    private final String baseUrl;
    private final String healthEndpoint;
    private final String metricsEndpoint;
    private final String description;
    private final Set<String> tags;
    private final ServiceStatus status;
    private final boolean enabled;
    private final Instant lastHeartbeat;
    private final Instant createdAt;
    private final Instant updatedAt;
}
