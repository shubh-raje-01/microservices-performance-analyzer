package com.analyzer.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIInsightDto {

    private final Long simulationId;
    private final String summary;           // One-paragraph human-readable analysis
    private final String anomalyType;       // e.g. "LATENCY_SPIKE", "ERROR_BURST", "NONE"
    private final Double anomalyScore;      // [0.0, 1.0]
    private final String predictedTrend;    // "IMPROVING" | "STABLE" | "DEGRADING"
    private final Double predictedP95Ms;    // Forecasted p95 for next period

    private final List<String> detectedPatterns;
    private final Map<String, Double> featureImportance;
    private final List<RecommendationDto> recommendations;
}