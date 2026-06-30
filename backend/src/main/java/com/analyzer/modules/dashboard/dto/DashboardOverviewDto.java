package com.analyzer.modules.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardOverviewDto {

    private final Long simulationId;
    private final String scenarioName;
    private final String targetService;
    private final String simulationStatus;

    // Flattened key metrics — avoids nesting MetricsSummaryDto for list views
    private final Double p95LatencyMs;
    private final Double errorRate;
    private final Double throughputRps;
    private final String healthStatus;

    // Flattened AI signal
    private final String anomalyType;
    private final boolean hasAnomaly;

    private final int highPriorityRecommendationCount;
    private final int totalRecommendationCount;

    private final Instant completedAt;
}