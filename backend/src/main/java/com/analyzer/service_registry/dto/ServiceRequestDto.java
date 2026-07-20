package com.analyzer.service_registry.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Set;

public class ServiceRequestDto {

    @NotBlank(message = "Service name cannot be blank")
    @Size(max = 100, message = "Service name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Base URL cannot be blank")
    @Size(max = 255, message = "Base URL cannot exceed 255 characters")
    private String baseUrl;

    @NotBlank(message = "Health endpoint cannot be blank")
    @Size(max = 100, message = "Health endpoint cannot exceed 100 characters")
    private String healthEndpoint;

    @Size(max = 100, message = "Metrics endpoint cannot exceed 100 characters")
    private String metricsEndpoint;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @Size(max = 10, message = "Service cannot have more than 10 tags")
    private Set<String> tags;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getHealthEndpoint() {
        return healthEndpoint;
    }

    public void setHealthEndpoint(String healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    public String getMetricsEndpoint() {
        return metricsEndpoint;
    }

    public void setMetricsEndpoint(String metricsEndpoint) {
        this.metricsEndpoint = metricsEndpoint;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }
}
