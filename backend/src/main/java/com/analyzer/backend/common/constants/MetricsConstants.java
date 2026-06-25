package com.analyzer.backend.common.constants;

public final class MetricsConstants {

    private MetricsConstants() {}

    // Latency thresholds (ms)
    public static final double LATENCY_GOOD_MS = 200.0;
    public static final double LATENCY_WARN_MS = 500.0;
    public static final double LATENCY_CRITICAL_MS  = 2000.0;

    // Error rate thresholds [0.0, 1.0]
    public static final double ERROR_RATE_GOOD = 0.01;   // 1%
    public static final double ERROR_RATE_WARN = 0.05;   // 5%
    public static final double ERROR_RATE_CRITICAL = 0.10;   // 10%

    // Throughput (rps)
    public static final double THROUGHPUT_LOW_RPS = 10.0;

    // Score boundaries (MetricsCalculator.healthScore)
    public static final double SCORE_HEALTHY = 80.0;
    public static final double SCORE_DEGRADED = 50.0;

    // AI confidence floor — recommendations below this are suppressed
    public static final double MIN_CONFIDENCE_SCORE = 0.60;
}