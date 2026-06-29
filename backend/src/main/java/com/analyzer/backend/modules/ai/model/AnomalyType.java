package com.analyzer.backend.modules.ai.model;

public enum AnomalyType {
    NONE,              // service is healthy, no anomaly detected
    LATENCY_SPIKE,     // p95 or p99 significantly above baseline
    ERROR_BURST,       // sudden spike in error rate
    THROUGHPUT_DROP,   // requests per second below expected floor
    SATURATION,        // concurrency causing queueing / back-pressure
    MEMORY_PRESSURE,   // GC pauses or OOM signals in log patterns
    CASCADING_FAILURE, // multiple anomaly types firing simultaneously
    UNKNOWN            // model confidence too low to classify
}