package com.analyzer.modules.recommendation.model;

import lombok.Getter;

@Getter
public enum RecommendationPriority {
    HIGH(3),
    MEDIUM(2),
    LOW(1);

    private final int weight;

    RecommendationPriority(int weight) {
        this.weight = weight;
    }
}