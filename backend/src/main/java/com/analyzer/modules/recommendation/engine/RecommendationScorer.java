package com.analyzer.modules.recommendation.engine;

import com.analyzer.modules.recommendation.model.Recommendation;
import com.analyzer.modules.recommendation.model.RecommendationCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RecommendationScorer {

    private final ScoringWeights weights = ScoringWeights.defaults();

    /**
     * Computes a composite score for a single recommendation given the current
     * simulation's ScoringContext.
     *
     * Formula (max ≈ 18.5):
     *   priority weight × 3 →  3 | 6 | 9
     *   confidence × 3 →  0..3
     *   impact × 2 →  0..2
     *   anomaly relevance bonus →  0..2.5
     *   health urgency →  0..1.5
     *   log error bonus →  0..0.5
     */
    public double score(Recommendation rec, ScoringContext ctx) {
        double score = 0.0;

        score += priorityScore(rec);
        score += confidenceScore(rec);
        score += impactScore(rec);
        score += anomalyRelevanceScore(rec, ctx);
        score += healthUrgencyScore(ctx);
        score += logErrorBonusScore(rec, ctx);

        double rounded = Math.round(score * 1000.0) / 1000.0;

        log.trace("Scored '{}' [{}|{}] context=[anomaly={} health={:.0f}] → {}",
                rec.getTitle(), rec.getCategory(), rec.getPriority(),
                ctx.getAnomalyType(), safeDouble(ctx.getHealthScore()), rounded);

        return rounded;
    }

    // ── Component scorers

    private double priorityScore(Recommendation rec) {
        return rec.getPriority().getWeight() * weights.getPriorityMultiplier();
    }

    private double confidenceScore(Recommendation rec) {
        if (rec.getConfidenceScore() == null) return 0.0;
        return rec.getConfidenceScore() * weights.getConfidenceWeight();
    }

    private double impactScore(Recommendation rec) {
        if (rec.getEstimatedImpact() == null) return 0.0;
        return rec.getEstimatedImpact() * weights.getImpactWeight();
    }

    private double anomalyRelevanceScore(Recommendation rec, ScoringContext ctx) {
        if (ctx.getAnomalyType() == null) return 0.0;
        if (!AnomalyRelevanceMap.isRelevant(ctx.getAnomalyType(), rec.getCategory())) {
            return 0.0;
        }

        double bonus = weights.getAnomalyRelevanceBonus();

        // Extra 25% when anomaly confidence is high — the model is very sure
        if (ctx.getAnomalyScore() != null && ctx.getAnomalyScore() > 0.7) {
            bonus *= 1.25;
        }

        return bonus;
    }

    private double healthUrgencyScore(ScoringContext ctx) {
        if (ctx.getHealthScore() == null) return 0.0;
        // Invert: score=0 (critical) → multiplier=1.0, score=100 (perfect) → 0.0
        double degradation = 1.0 - (ctx.getHealthScore() / 100.0);
        return degradation * weights.getHealthUrgencyScale();
    }

    private double logErrorBonusScore(Recommendation rec, ScoringContext ctx) {
        // Only boost recommendations that directly address the observed log signals
        boolean isRelevantCategory =
                rec.getCategory() == RecommendationCategory.LATENCY
                        || rec.getCategory() == RecommendationCategory.ERROR_RATE
                        || rec.getCategory() == RecommendationCategory.CONFIGURATION;

        if (isRelevantCategory
                && ctx.isLogThresholdBreached()
                && ctx.isLogHasErrors()) {
            return weights.getLogErrorBonus();
        }
        return 0.0;
    }

    // ── Helpers

    private double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }
}