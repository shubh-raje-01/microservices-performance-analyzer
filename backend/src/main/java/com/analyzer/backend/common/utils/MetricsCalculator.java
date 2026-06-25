package com.analyzer.backend.common.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MetricsCalculator {

    private MetricsCalculator() {}

    // ── Percentiles =>

    public static double percentile(List<Double> values, int p) {
        if (values == null || values.isEmpty()) return 0.0;
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int idx = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(idx, sorted.size() - 1)));
    }

    public static double p50(List<Double> values) { return percentile(values, 50); }
    public static double p95(List<Double> values) { return percentile(values, 95); }
    public static double p99(List<Double> values) { return percentile(values, 99); }

    // ── Descriptive stats =>

    public static double mean(List<Double> values) {
        if (values == null || values.isEmpty()) return 0.0;
        return values.stream().mapToDouble(d -> d).average().orElse(0.0);
    }

    public static double stdDev(List<Double> values) {
        if (values == null || values.size() < 2) return 0.0;
        double avg = mean(values);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - avg, 2))
                .average().orElse(0.0);
        return Math.sqrt(variance);
    }

    public static double max(List<Double> values) {
        if (values == null || values.isEmpty()) return 0.0;
        return values.stream().mapToDouble(d -> d).max().orElse(0.0);
    }

    public static double min(List<Double> values) {
        if (values == null || values.isEmpty()) return 0.0;
        return values.stream().mapToDouble(d -> d).min().orElse(0.0);
    }

    // ── Rate calculations =>

    public static double errorRate(long failed, long total) {
        if (total == 0) return 0.0;
        return round((double) failed / total, 6);
    }

    public static double throughput(long requests, long durationSeconds) {
        if (durationSeconds == 0) return 0.0;
        return round((double) requests / durationSeconds, 2);
    }

    // ── Health scoring =>

    /**
     * Composite score from 0 (worst) to 100 (best).
     * Weights: p95 latency 50%, error rate 30%, throughput stability 20%.
     */
    public static double healthScore(double p95Ms, double errorRate, double throughputRps) {
        double latencyScore = Math.max(0, 100 - (p95Ms / 50.0));
        double errorScore = Math.max(0, 100 - (errorRate * 1000));
        double throughputScore = Math.min(100, throughputRps / 10.0);
        return round((latencyScore * 0.5) + (errorScore * 0.3) + (throughputScore * 0.2), 1);
    }

    public static String healthLabel(double score) {
        if (score >= 80) return "HEALTHY";
        if (score >= 50) return "DEGRADED";
        return "CRITICAL";
    }

    // ── Utility =>

    public static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}