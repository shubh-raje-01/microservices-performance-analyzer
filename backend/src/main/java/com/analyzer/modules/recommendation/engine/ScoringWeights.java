package com.analyzer.modules.recommendation.engine;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScoringWeights {

    /**
     * Multiplied by RecommendationPriority.weight (1, 2, or 3).
     * Max contribution: 3 × 3 = 9 points.
     */
    @Builder.Default
    private final double priorityMultiplier   = 3.0;

    /**
     * Multiplied by confidenceScore [0..1].
     * Max contribution: 3 points.
     */
    @Builder.Default
    private final double confidenceWeight     = 3.0;

    /**
     * Multiplied by estimatedImpact [0..1].
     * Max contribution: 2 points.
     */
    @Builder.Default
    private final double impactWeight         = 2.0;

    /**
     * Flat bonus when the recommendation category directly addresses
     * the detected anomaly type.
     * Extra 25% applied when anomalyScore > 0.7.
     * Max contribution: 2.5 points.
     */
    @Builder.Default
    private final double anomalyRelevanceBonus = 2.0;

    /**
     * Multiplied by (1 - healthScore / 100).
     * Worse health → higher urgency multiplier.
     * Max contribution: 1.5 points (when healthScore = 0).
     */
    @Builder.Default
    private final double healthUrgencyScale   = 1.5;

    /**
     * Flat bonus applied to LATENCY and ERROR_RATE recommendations
     * when the log processor reported threshold breaches and errors.
     * Max contribution: 0.5 points.
     */
    @Builder.Default
    private final double logErrorBonus        = 0.5;

    // ── Maximum possible score: 9 + 3 + 2 + 2.5 + 1.5 + 0.5 = 18.5
    public static final double MAX_SCORE = 18.5;

    public static ScoringWeights defaults() {
        return ScoringWeights.builder().build();
    }
}