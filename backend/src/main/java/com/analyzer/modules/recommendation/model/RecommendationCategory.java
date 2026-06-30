package com.analyzer.modules.recommendation.model;

public enum RecommendationCategory {
    LATENCY,        // reduce response time
    ERROR_RATE,     // reduce failure ratio
    THROUGHPUT,     // increase request handling capacity
    SCALING,        // horizontal or vertical scaling advice
    MEMORY,         // GC tuning, heap sizing, off-heap strategies
    CONFIGURATION,  // timeouts, pool sizes, retry settings
    ARCHITECTURE    // structural or design-level suggestions
}