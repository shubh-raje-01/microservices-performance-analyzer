package com.analyzer.modules.recommendation.engine;

import com.analyzer.modules.ai.model.AnomalyType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScoringContext {

    private final Long simulationId;

    // From AIInsight
    private final AnomalyType anomalyType;
    private final Double anomalyScore;   // [0.0, 1.0]

    // From AggregatedMetrics
    private final Double healthScore;    // [0.0, 100.0] — lower = more urgent
    private final String healthStatus;   // "HEALTHY" | "DEGRADED" | "CRITICAL"
    private final Double avgErrorRate;
    private final Double p95LatencyMs;
    private final Double throughputRps;
    private final String worstSeverity;

    // From LogSummaryDto
    private final boolean logThresholdBreached;
    private final boolean logHasErrors;
    private final long logErrorCount;
    private final long logWarnCount;
}