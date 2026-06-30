package com.analyzer.modules.recommendation.engine;

import com.analyzer.modules.ai.model.AnomalyType;
import com.analyzer.modules.recommendation.model.RecommendationCategory;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class AnomalyRelevanceMap {

    private AnomalyRelevanceMap() {}

    private static final Map<AnomalyType, Set<RecommendationCategory>> RELEVANCE =
            new EnumMap<>(AnomalyType.class);

    static {
        RELEVANCE.put(AnomalyType.LATENCY_SPIKE, Set.of(
                RecommendationCategory.LATENCY,
                RecommendationCategory.CONFIGURATION,
                RecommendationCategory.SCALING));

        RELEVANCE.put(AnomalyType.ERROR_BURST, Set.of(
                RecommendationCategory.ERROR_RATE,
                RecommendationCategory.CONFIGURATION,
                RecommendationCategory.ARCHITECTURE));

        RELEVANCE.put(AnomalyType.THROUGHPUT_DROP, Set.of(
                RecommendationCategory.THROUGHPUT,
                RecommendationCategory.SCALING,
                RecommendationCategory.CONFIGURATION));

        RELEVANCE.put(AnomalyType.SATURATION, Set.of(
                RecommendationCategory.SCALING,
                RecommendationCategory.THROUGHPUT,
                RecommendationCategory.MEMORY));

        RELEVANCE.put(AnomalyType.MEMORY_PRESSURE, Set.of(
                RecommendationCategory.MEMORY,
                RecommendationCategory.CONFIGURATION,
                RecommendationCategory.SCALING));

        RELEVANCE.put(AnomalyType.CASCADING_FAILURE, Set.of(
                RecommendationCategory.ARCHITECTURE,
                RecommendationCategory.ERROR_RATE,
                RecommendationCategory.CONFIGURATION));

        // No bonus for NONE / UNKNOWN — model did not detect a specific signal
        RELEVANCE.put(AnomalyType.NONE,    Set.of());
        RELEVANCE.put(AnomalyType.UNKNOWN, Set.of());
    }

    public static boolean isRelevant(AnomalyType anomaly,
                                     RecommendationCategory category) {
        if (anomaly == null) return false;
        Set<RecommendationCategory> relevant = RELEVANCE.get(anomaly);
        return relevant != null && relevant.contains(category);
    }
}