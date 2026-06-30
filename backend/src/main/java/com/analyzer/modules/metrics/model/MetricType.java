package com.analyzer.modules.metrics.model;

public enum MetricType {
    LATENCY,        // response time measurements
    THROUGHPUT,     // requests per second
    ERROR_RATE,     // failure ratio [0.0, 1.0]
    CONCURRENCY,    // active thread / connection count
    SATURATION,     // queue depth or CPU/memory pressure signal
    AVAILABILITY    // uptime ratio [0.0, 1.0]
}
