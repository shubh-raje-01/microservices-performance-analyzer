package com.analyzer.backend.modules.metrics.collector;

import com.analyzer.backend.common.constants.MetricsConstants;
import com.analyzer.backend.modules.metrics.model.MetricSeverity;
import com.analyzer.backend.modules.metrics.model.MetricType;
import org.springframework.stereotype.Component;

@Component
public class MetricsSeverityEvaluator {

    /**
     * Derives severity by comparing a raw metric value against
     * the project-wide thresholds defined in MetricsConstants.
     */
    public MetricSeverity evaluate(MetricType type, double value) {
        return switch (type) {
            case LATENCY -> evaluateLatency(value);
            case ERROR_RATE -> evaluateErrorRate(value);
            case THROUGHPUT -> evaluateThroughput(value);
            case AVAILABILITY -> evaluateAvailability(value);
            default -> MetricSeverity.NORMAL;
        };
    }

    /**
     * Evaluates p95 latency specifically — the primary SLA indicator.
     */
    public MetricSeverity evaluateP95Latency(double p95Ms) {
        if (p95Ms >= MetricsConstants.LATENCY_CRITICAL_MS) return MetricSeverity.CRITICAL;
        if (p95Ms >= MetricsConstants.LATENCY_WARN_MS)     return MetricSeverity.WARNING;
        return MetricSeverity.NORMAL;
    }

    public String buildLatencyNote(double avgMs, double p95Ms, double p99Ms,
                                   double errorRateThreshold) {
        StringBuilder note = new StringBuilder();

        if (p95Ms >= MetricsConstants.LATENCY_CRITICAL_MS) {
            note.append(String.format(
                    "CRITICAL: p95=%.0fms exceeds %.0fms ceiling. ", p95Ms,
                    MetricsConstants.LATENCY_CRITICAL_MS));
        } else if (p95Ms >= MetricsConstants.LATENCY_WARN_MS) {
            note.append(String.format(
                    "WARNING: p95=%.0fms above %.0fms warn threshold. ", p95Ms,
                    MetricsConstants.LATENCY_WARN_MS));
        }

        if (p99Ms >= MetricsConstants.LATENCY_CRITICAL_MS * 1.5) {
            note.append(String.format(
                    "Long tail detected: p99=%.0fms. ", p99Ms));
        }

        return note.isEmpty()
                ? String.format("Latency within bounds. avg=%.0fms p95=%.0fms", avgMs, p95Ms)
                : note.toString().trim();
    }

    // ── Private evaluators =>

    private MetricSeverity evaluateLatency(double avgMs) {
        if (avgMs >= MetricsConstants.LATENCY_CRITICAL_MS) return MetricSeverity.CRITICAL;
        if (avgMs >= MetricsConstants.LATENCY_WARN_MS)     return MetricSeverity.WARNING;
        return MetricSeverity.NORMAL;
    }

    private MetricSeverity evaluateErrorRate(double rate) {
        if (rate >= MetricsConstants.ERROR_RATE_CRITICAL) return MetricSeverity.CRITICAL;
        if (rate >= MetricsConstants.ERROR_RATE_WARN)     return MetricSeverity.WARNING;
        return MetricSeverity.NORMAL;
    }

    private MetricSeverity evaluateThroughput(double rps) {
        if (rps < MetricsConstants.THROUGHPUT_LOW_RPS) return MetricSeverity.WARNING;
        return MetricSeverity.NORMAL;
    }

    private MetricSeverity evaluateAvailability(double ratio) {
        if (ratio < 0.95) return MetricSeverity.CRITICAL;
        if (ratio < 0.99) return MetricSeverity.WARNING;
        return MetricSeverity.NORMAL;
    }
}