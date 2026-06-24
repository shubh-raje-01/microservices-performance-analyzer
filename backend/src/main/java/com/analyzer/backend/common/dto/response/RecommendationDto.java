package com.analyzer.backend.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecommendationDto {

    private final Long id;
    private final Long simulationId;

    private final String category;      // "LATENCY" | "ERROR_RATE" | "THROUGHPUT" | "SCALING"
    private final String priority;      // "HIGH" | "MEDIUM" | "LOW"
    private final String title;
    private final String description;
    private final String action;        // Concrete step the user should take
    private final Double confidenceScore; // [0.0, 1.0] from AI model
    private final Double estimatedImpact; // Expected % improvement

    private final Instant createdAt;
}