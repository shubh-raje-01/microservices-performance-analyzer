package com.analyzer.backend.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardSummaryDto {

    private final Long simulationId;
    private final String scenarioName;
    private final String targetService;
    private final String simulationStatus;

    private final MetricsSummaryDto metrics;
    private final AIInsightDto aiInsight;
    private final List<RecommendationDto> recommendations;

    private final Instant simulationStartedAt;
    private final Instant simulationCompletedAt;
    private final Instant generatedAt;
}