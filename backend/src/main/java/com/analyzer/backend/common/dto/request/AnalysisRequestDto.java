package com.analyzer.backend.common.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Map;

@Data
public class AnalysisRequestDto {

    @NotBlank(message = "Scenario name is required")
    @Size(min = 3, max = 100, message = "Scenario name must be 3–100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_\\-\\s]+$",
            message = "Scenario name may only contain letters, digits, hyphens, underscores, and spaces")
    private String scenarioName;

    @NotBlank(message = "Target service is required")
    @Size(max = 100)
    private String targetService;

    @Min(value = 1, message = "Duration must be at least 1 second")
    @Max(value = 3600, message = "Duration cannot exceed 3600 seconds (1 hour)")
    private int durationSeconds = 60;

    @Min(value = 1, message = "Must have at least 1 concurrent user")
    @Max(value = 10_000, message = "Cannot exceed 10 000 concurrent users")
    private int concurrentUsers = 100;

    @DecimalMin(value = "0.0", message = "Error threshold cannot be negative")
    @DecimalMax(value = "1.0", message = "Error threshold cannot exceed 1.0 (100%)")
    private double errorRateThreshold = 0.05;

    private Map<@NotBlank String, Object> customParams;
}
