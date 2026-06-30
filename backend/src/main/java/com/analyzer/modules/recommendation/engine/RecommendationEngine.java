package com.analyzer.modules.recommendation.engine;

import com.analyzer.common.utils.JsonUtils;
import com.analyzer.modules.ai.adapter.FastAPIRecommendation;
import com.analyzer.modules.ai.model.AIInsight;
import com.analyzer.modules.ai.model.AnomalyType;
import com.analyzer.modules.logging.controller.LogSummaryDto;
import com.analyzer.modules.metrics.model.AggregatedMetrics;
import com.analyzer.modules.recommendation.model.Recommendation;
import com.analyzer.modules.recommendation.model.RecommendationCategory;
import com.analyzer.modules.recommendation.model.RecommendationPriority;
import com.analyzer.modules.recommendation.model.RecommendationSource;
import com.analyzer.modules.simulation.model.Simulation;

import com.fasterxml.jackson.core.type.TypeReference;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationEngine {

    private final RecommendationScorer scorer;
    private final RuleBasedRecommendationGenerator ruleGenerator;

    private static final TypeReference<List<FastAPIRecommendation>> RECS_TYPE =
            new TypeReference<>() {};

    /**
     * Full pipeline — call once per AIAnalysisCompletedEvent:
     *  1. Parse AI recommendations from AIInsight.rawRecommendations JSON
     *  2. Generate rule-based recommendations from ScoringContext thresholds
     *  3. Merge, deduplicate (AI takes precedence in its covered categories)
     *  4. Score every recommendation via RecommendationScorer
     *  5. Sort by composite score, assign 1-based ranks
     *  6. Return entities ready for repository.saveAll()
     */
    public List<Recommendation> process (
            Simulation sim,
            AIInsight insight,
            AggregatedMetrics metrics,
            LogSummaryDto logs
    ) {

        ScoringContext ctx = buildScoringContext(sim, insight, metrics, logs);

        List<Recommendation> aiRecs = parseAIRecommendations(sim, insight);
        List<Recommendation> ruleRecs = ruleGenerator.generate(sim, ctx);

        log.info("RecommendationEngine: {} AI + {} rule-based for simulation {}",
                aiRecs.size(), ruleRecs.size(), sim.getId());

        List<Recommendation> merged = mergeAndDeduplicate(aiRecs, ruleRecs);
        scoreAll(merged, ctx);
        List<Recommendation> ranked = rankByScore(merged);

        log.info("RecommendationEngine: {} final recommendations for simulation {} " +
                        "(top score={}, worst severity={})",
                ranked.size(), sim.getId(),
                ranked.isEmpty() ? "N/A" : ranked.get(0).getCompositeScore(),
                ctx.getWorstSeverity());

        return ranked;
    }

    // ── Step 1: Parse AI recommendations

    private List<Recommendation> parseAIRecommendations (Simulation sim, AIInsight insight) {

        String raw = insight.getRawRecommendations();
        if (raw == null || raw.isBlank()) {
            log.debug("No raw recommendations in AIInsight for simulation {}", sim.getId());
            return List.of();
        }

        List<FastAPIRecommendation> parsed = JsonUtils
                .fromJson(raw, RECS_TYPE)
                .orElse(List.of());

        List<Recommendation> recs = new ArrayList<>();
        for (FastAPIRecommendation f : parsed) {
            try {
                recs.add(mapFromFastAPI(sim, insight, f));
            } catch (Exception ex) {
                log.warn("RecommendationEngine: could not map AI recommendation '{}' — {}",
                        f.getTitle(), ex.getMessage());
            }
        }

        return recs;
    }

    private Recommendation mapFromFastAPI (
            Simulation sim,
            AIInsight insight,
            FastAPIRecommendation f
    ) {
        return Recommendation.builder()
                .simulation(sim)
                .simulationId(sim.getId())
                .aiInsightId(insight.getId())
                .category(resolveCategory(f.getCategory()))
                .priority(resolvePriority(f.getPriority()))
                .source(RecommendationSource.AI_MODEL)
                .title(coalesce(f.getTitle(), "Untitled recommendation"))
                .description(coalesce(f.getDescription(), "No description provided"))
                .action(coalesce(f.getAction(), "No action specified"))
                .confidenceScore(clamp(f.getConfidenceScore()))
                .estimatedImpact(clamp(f.getEstimatedImpact()))
                .compositeScore(0.0)
                .rank(0)
                .build();
    }

    // ── Step 2: Merge and deduplicate

    /**
     * Keeps all AI recommendations. Admits a rule-based recommendation only when
     * no AI recommendation already covers the same category at equal or higher
     * priority. This prevents doubling up on LATENCY advice when FastAPI already
     * returned a latency recommendation.
     */
    private List<Recommendation> mergeAndDeduplicate (
            List<Recommendation> aiRecs,
            List<Recommendation> ruleRecs
    ) {
        List<Recommendation> merged = new ArrayList<>(aiRecs);

        for (Recommendation rule : ruleRecs) {
            boolean coveredByAI = aiRecs.stream().anyMatch(ai ->
                    ai.getCategory() == rule.getCategory()
                            && ai.getPriority().getWeight() >= rule.getPriority().getWeight());

            if (!coveredByAI) {
                merged.add(rule);
                log.debug("Admitted rule-based rec '{}' [{}] — no AI coverage",
                        rule.getTitle(), rule.getCategory());
            } else {
                log.debug("Dropped rule-based rec '{}' [{}] — covered by AI at >= priority",
                        rule.getTitle(), rule.getCategory());
            }
        }

        return merged;
    }

    // ── Step 3: Score

    private void scoreAll(List<Recommendation> recs, ScoringContext ctx) {

        recs.forEach(rec -> rec.setCompositeScore(scorer.score(rec, ctx)));
    }

    // ── Step 4: Rank

    private List<Recommendation> rankByScore(List<Recommendation> recs) {

        List<Recommendation> sorted = recs.stream()
                .sorted(Comparator
                        .comparingDouble(Recommendation::getCompositeScore).reversed()
                        .thenComparing(
                                r -> r.getPriority().getWeight(),
                                Comparator.reverseOrder())
                        .thenComparing(
                                r -> r.getSource() == RecommendationSource.AI_MODEL ? 0 : 1))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        AtomicInteger rankCounter = new AtomicInteger(1);
        sorted.forEach(r -> r.setRank(rankCounter.getAndIncrement()));
        return sorted;
    }

    // ── Context builder

    private ScoringContext buildScoringContext (
            Simulation sim,
            AIInsight insight,
            AggregatedMetrics metrics,
            LogSummaryDto logs
    ) {
        return ScoringContext.builder()
                .simulationId(sim.getId())
                .anomalyType(insight.getAnomalyType() != null
                        ? insight.getAnomalyType() : AnomalyType.NONE)
                .anomalyScore(insight.getAnomalyScore())
                .healthScore(metrics.getHealthScore())
                .healthStatus(metrics.getHealthStatus())
                .avgErrorRate(metrics.getAvgErrorRate())
                .p95LatencyMs(metrics.getP95LatencyMs())
                .throughputRps(metrics.getAvgThroughputRps())
                .worstSeverity(metrics.getWorstSeverity() != null
                        ? metrics.getWorstSeverity().name() : "NORMAL")
                .logThresholdBreached(logs.isThresholdBreached())
                .logHasErrors(logs.isHasErrors())
                .logErrorCount(logs.getErrorCount())
                .logWarnCount(logs.getWarnCount())
                .build();
    }

    // ── Parse helpers

    private RecommendationCategory resolveCategory (String raw) {

        if (raw == null) return RecommendationCategory.CONFIGURATION;
        try {
            return RecommendationCategory.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown category '{}' — defaulting to CONFIGURATION", raw);
            return RecommendationCategory.CONFIGURATION;
        }
    }

    private RecommendationPriority resolvePriority (String raw) {

        if (raw == null) return RecommendationPriority.MEDIUM;
        try {
            return RecommendationPriority.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown priority '{}' — defaulting to MEDIUM", raw);
            return RecommendationPriority.MEDIUM;
        }

    }

    private String coalesce (String value, String fallback) {
        return (value != null && !value.isBlank()) ? value.trim() : fallback;
    }

    private Double clamp (Double value) {
        if (value == null) return null;
        return Math.max(0.0, Math.min(1.0, value));
    }
}