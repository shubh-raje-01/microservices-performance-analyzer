package com.analyzer.backend.modules.ai.controller;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIInsightResponseDto {

    private final Long id;
    private final Long simulationId;
    private final String serviceName;
    private final String status;

    // Insight content
    private final String summary;
    private final String anomalyType;
    private final Double anomalyScore;
    private final String predictedTrend;
    private final Double predictedP95Ms;
    private final boolean degraded;

    private final List<String> detectedPatterns;
    private final Map<String, Double> featureImportance;

    // Metadata
    private final String modelVersion;
    private final Instant analyzedAt;
    private final Instant createdAt;
}