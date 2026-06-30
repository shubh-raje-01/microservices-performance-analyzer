package com.analyzer.modules.ai.ml;

import com.analyzer.common.utils.MetricsCalculator;
import com.analyzer.modules.ai.adapter.FastAPIRequest;
import com.analyzer.modules.logging.controller.LogSummaryDto;
import com.analyzer.modules.metrics.model.AggregatedMetrics;
import com.analyzer.modules.simulation.model.Simulation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AIFeatureBuilder {

    /**
     * Assembles an AIFeatureSet from the three data sources
     * the AI service can observe about a completed simulation.
     */
    public AIFeatureSet build (Simulation sim, AggregatedMetrics metrics, LogSummaryDto logSummary) {

        double p99 = safeDouble(metrics.getP99LatencyMs());
        double p50 = safeDouble(metrics.getP50LatencyMs());
        double avg = safeDouble(metrics.getAvgLatencyMs());

        double tailRatio = p50 > 0 ? p99 / p50 : 0.0;
        double peakThroughput = safeDouble(metrics.getPeakThroughputRps());
        double avgThroughput = safeDouble(metrics.getAvgThroughputRps());
        double throughputVar = avgThroughput > 0
                ? (peakThroughput - avgThroughput) / avgThroughput : 0.0;

        return AIFeatureSet.builder()
                // Latency
                .avgLatencyMs(avg)
                .p50LatencyMs(p50)
                .p95LatencyMs(safeDouble(metrics.getP95LatencyMs()))
                .p99LatencyMs(p99)
                .maxLatencyMs(safeDouble(metrics.getMaxLatencyMs()))
                .stdDevLatencyMs(safeDouble(metrics.getStdDevLatencyMs()))
                .tailLatencyRatio(MetricsCalculator.round(tailRatio, 4))
                // Throughput
                .avgThroughputRps(avgThroughput)
                .peakThroughputRps(peakThroughput)
                .throughputVariability(MetricsCalculator.round(throughputVar, 4))
                // Errors
                .avgErrorRate(safeDouble(metrics.getAvgErrorRate()))
                .maxErrorRate(safeDouble(metrics.getMaxErrorRate()))
                .totalRequests(safeLong(metrics.getTotalRequests()))
                .totalFailedRequests(safeLong(metrics.getTotalFailedRequests()))
                .errorThresholdBreached(
                        metrics.getAvgErrorRate() != null
                                && metrics.getAvgErrorRate() > sim.getErrorRateThreshold())
                // Health
                .healthScore(safeDouble(metrics.getHealthScore()))
                .healthStatus(metrics.getHealthStatus())
                .worstMetricSeverity(
                        metrics.getWorstSeverity() != null
                                ? metrics.getWorstSeverity().name() : "NORMAL")
                // Log signals
                .simulationId(sim.getId())
                .serviceName(sim.getTargetService())
                .totalLogCount(logSummary.getTotalCount())
                .errorLogCount(logSummary.getErrorCount())
                .warnLogCount(logSummary.getWarnCount())
                .hasLogErrors(logSummary.isHasErrors())
                .logThresholdBreached(logSummary.isThresholdBreached())
                .logCountByLevel(logSummary.getCountByLevel())
                .logCountByCategory(logSummary.getCountByCategory())
                // Simulation config
                .durationSeconds(sim.getDurationSeconds())
                .concurrentUsers(sim.getConcurrentUsers())
                .errorRateThreshold(sim.getErrorRateThreshold())
                .scenarioName(sim.getScenarioName())
                .build();
    }

    /**
     * Converts an AIFeatureSet into the wire-format request the Python service expects.
     */
    public FastAPIRequest toRequest(AIFeatureSet features) {
        return FastAPIRequest.builder()
                .simulationId(features.getSimulationId())
                .serviceName(features.getServiceName())
                .metrics(FastAPIRequest.MetricsPayload.builder()
                        .avgLatencyMs(features.getAvgLatencyMs())
                        .p50LatencyMs(features.getP50LatencyMs())
                        .p95LatencyMs(features.getP95LatencyMs())
                        .p99LatencyMs(features.getP99LatencyMs())
                        .maxLatencyMs(features.getMaxLatencyMs())
                        .stdDevLatencyMs(features.getStdDevLatencyMs())
                        .tailLatencyRatio(features.getTailLatencyRatio())
                        .avgThroughputRps(features.getAvgThroughputRps())
                        .peakThroughputRps(features.getPeakThroughputRps())
                        .throughputVariability(features.getThroughputVariability())
                        .avgErrorRate(features.getAvgErrorRate())
                        .maxErrorRate(features.getMaxErrorRate())
                        .totalRequests(features.getTotalRequests())
                        .totalFailedRequests(features.getTotalFailedRequests())
                        .healthScore(features.getHealthScore())
                        .healthStatus(features.getHealthStatus())
                        .worstSeverity(features.getWorstMetricSeverity())
                        .build())
                .logs(FastAPIRequest.LogsPayload.builder()
                        .totalCount(features.getTotalLogCount())
                        .errorCount(features.getErrorLogCount())
                        .warnCount(features.getWarnLogCount())
                        .hasErrors(features.isHasLogErrors())
                        .thresholdBreached(features.isLogThresholdBreached())
                        .countByLevel(features.getLogCountByLevel())
                        .countByCategory(features.getLogCountByCategory())
                        .build())
                .simulationConfig(FastAPIRequest.SimConfigPayload.builder()
                        .durationSeconds(features.getDurationSeconds())
                        .concurrentUsers(features.getConcurrentUsers())
                        .errorRateThreshold(features.getErrorRateThreshold())
                        .scenarioName(features.getScenarioName())
                        .build())
                .build();
    }

    // ── Null-safe helpers

    private double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }

    private long safeLong(Long value) {
        return value != null ? value : 0L;
    }
}