package com.analyzer.backend.modules.ai.adapter;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FastAPIRecommendation {
    private String category;         // "LATENCY" | "ERROR_RATE" | "THROUGHPUT" | "SCALING"
    private String priority;         // "HIGH" | "MEDIUM" | "LOW"
    private String title;
    private String description;
    private String action;
    private Double confidenceScore;
    private Double estimatedImpact;
}