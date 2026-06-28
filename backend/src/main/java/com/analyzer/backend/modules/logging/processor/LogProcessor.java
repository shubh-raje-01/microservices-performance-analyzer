package com.analyzer.backend.modules.logging.processor;

import com.analyzer.backend.common.constants.MetricsConstants;
import com.analyzer.backend.modules.logging.model.LogCategory;
import com.analyzer.backend.modules.logging.model.LogEntry;
import com.analyzer.backend.modules.logging.model.LogLevel;
import com.analyzer.backend.modules.logging.repository.LogRepository;
import com.analyzer.backend.modules.simulation.events.SimulationCompletedEvent;
import com.analyzer.backend.modules.simulation.events.SimulationFailedEvent;
import com.analyzer.backend.modules.simulation.events.SimulationStartedEvent;
import com.analyzer.backend.modules.simulation.model.Simulation;
import com.analyzer.backend.modules.simulation.model.SimulationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogProcessor {

    private static final String SOURCE_SIMULATION = "SimulationService";
    private static final String SOURCE_ANALYZER = "MetricsAnalyzer";
    private static final String SOURCE_SYSTEM = "LogProcessor";

    private final LogRepository repository;

    // ── Event listeners =>

    @Async
    @EventListener
    @Transactional
    public void onSimulationStarted(SimulationStartedEvent event) {
        Simulation sim = event.getSimulation();
        log.debug("LogProcessor handling SimulationStartedEvent — id={}", sim.getId());

        LogEntry entry = startedEntry(sim);
        repository.save(entry);
    }

    @Async
    @EventListener
    @Transactional
    public void onSimulationCompleted(SimulationCompletedEvent event) {
        Simulation       sim    = event.getSimulation();
        SimulationResult result = event.getResult();
        log.debug("LogProcessor handling SimulationCompletedEvent — id={}", sim.getId());

        List<LogEntry> entries = buildCompletedEntries(sim, result);
        repository.saveAll(entries);
        log.info("LogProcessor persisted {} log entries for simulation {}",
                entries.size(), sim.getId());
    }

    @Async
    @EventListener
    @Transactional
    public void onSimulationFailed(SimulationFailedEvent event) {
        Simulation sim = event.getSimulation();
        log.debug("LogProcessor handling SimulationFailedEvent — id={}", sim.getId());

        LogEntry entry = failedEntry(sim, event.getReason());
        repository.save(entry);
    }

    // ── Entry builders =>

    private LogEntry startedEntry(Simulation sim) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("scenarioName", sim.getScenarioName());
        ctx.put("targetService", sim.getTargetService());
        ctx.put("durationSeconds", sim.getDurationSeconds());
        ctx.put("concurrentUsers", sim.getConcurrentUsers());
        ctx.put("errorRateThreshold", sim.getErrorRateThreshold());
        ctx.put("customParams", sim.getCustomParams());

        return LogEntry.builder()
                .simulation(sim)
                .simulationId(sim.getId())
                .level(LogLevel.INFO)
                .category(LogCategory.SIMULATION)
                .serviceName(sim.getTargetService())
                .source(SOURCE_SIMULATION)
                .message(String.format(
                        "Simulation '%s' started — %d users · %ds · service=%s",
                        sim.getScenarioName(), sim.getConcurrentUsers(),
                        sim.getDurationSeconds(), sim.getTargetService()))
                .context(ctx)
                .occurredAt(sim.getStartedAt() != null ? sim.getStartedAt() : Instant.now())
                .build();
    }

    private List<LogEntry> buildCompletedEntries(Simulation sim,
                                                 SimulationResult result) {
        List<LogEntry> entries = new ArrayList<>();
        Instant now = Instant.now();

        entries.add(completionSummaryEntry(sim, result, now));
        entries.add(latencyAnalysisEntry(sim, result, now));
        entries.add(errorRateAnalysisEntry(sim, result, now));
        entries.add(throughputAnalysisEntry(sim, result, now));
        entries.add(healthScoreEntry(sim, result, now));

        return entries;
    }

    // ── Completion summary =>

    private LogEntry completionSummaryEntry(Simulation sim,
                                            SimulationResult result,
                                            Instant occurredAt) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("scenarioName", sim.getScenarioName());
        ctx.put("targetService", sim.getTargetService());
        ctx.put("durationSeconds", sim.getDurationSeconds());
        ctx.put("concurrentUsers", sim.getConcurrentUsers());
        ctx.put("avgLatencyMs", result.getAvgLatencyMs());
        ctx.put("p95LatencyMs", result.getP95LatencyMs());
        ctx.put("p99LatencyMs", result.getP99LatencyMs());
        ctx.put("throughputRps", result.getThroughputRps());
        ctx.put("actualErrorRate", result.getActualErrorRate());
        ctx.put("totalRequests", result.getTotalRequests());
        ctx.put("failedRequests", result.getFailedRequests());

        return LogEntry.builder()
                .simulation(sim)
                .simulationId(sim.getId())
                .level(LogLevel.INFO)
                .category(LogCategory.SIMULATION)
                .serviceName(sim.getTargetService())
                .source(SOURCE_SIMULATION)
                .message(String.format(
                        "Simulation '%s' completed — p95=%.0fms · errorRate=%.2f%% · throughput=%.1frps · requests=%d",
                        sim.getScenarioName(),
                        result.getP95LatencyMs(),
                        result.getActualErrorRate() * 100,
                        result.getThroughputRps(),
                        result.getTotalRequests()))
                .context(ctx)
                .occurredAt(occurredAt)
                .build();
    }

    // ── Latency analysis =>

    private LogEntry latencyAnalysisEntry(Simulation sim,
                                          SimulationResult result,
                                          Instant occurredAt) {
        double p95 = result.getP95LatencyMs();
        double p99 = result.getP99LatencyMs();
        double avg = result.getAvgLatencyMs();
        double spread = p99 > 0 ? (p99 - avg) / avg : 0;  // tail spread ratio

        LogLevel level;
        if (p95 >= MetricsConstants.LATENCY_CRITICAL_MS) {
            level = LogLevel.ERROR;
        } else if (p95 >= MetricsConstants.LATENCY_WARN_MS) {
            level = LogLevel.WARN;
        } else {
            level = LogLevel.INFO;
        }

        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("avgMs", avg);
        ctx.put("p95Ms", p95);
        ctx.put("p99Ms", p99);
        ctx.put("tailSpreadRatio", Math.round(spread * 100.0) / 100.0);
        ctx.put("warnThresholdMs", MetricsConstants.LATENCY_WARN_MS);
        ctx.put("criticalThresholdMs",MetricsConstants.LATENCY_CRITICAL_MS);
        ctx.put("exceeded", level != LogLevel.INFO);

        String message = switch (level) {
            case ERROR -> String.format(
                    "CRITICAL latency: p95=%.0fms exceeds %.0fms ceiling — tail spread ratio=%.2f",
                    p95, MetricsConstants.LATENCY_CRITICAL_MS, spread);
            case WARN -> String.format(
                    "WARNING latency: p95=%.0fms exceeds %.0fms threshold — tail spread ratio=%.2f",
                    p95, MetricsConstants.LATENCY_WARN_MS, spread);
            default -> String.format(
                    "Latency within bounds — avg=%.0fms · p95=%.0fms · p99=%.0fms",
                    avg, p95, p99);
        };

        return LogEntry.builder()
                .simulation(sim)
                .simulationId(sim.getId())
                .level(level)
                .category(LogCategory.PERFORMANCE)
                .serviceName(sim.getTargetService())
                .source(SOURCE_ANALYZER)
                .message(message)
                .context(ctx)
                .durationMs(sim.getDurationSeconds() * 1000L)
                .occurredAt(occurredAt)
                .build();
    }

    // ── Error rate analysis =>

    private LogEntry errorRateAnalysisEntry(Simulation sim,
                                            SimulationResult result,
                                            Instant occurredAt) {
        double actual = result.getActualErrorRate();
        double threshold = sim.getErrorRateThreshold();
        boolean breached = actual > threshold;

        LogLevel level;
        if (actual >= MetricsConstants.ERROR_RATE_CRITICAL) {
            level = LogLevel.ERROR;
        } else if (breached || actual >= MetricsConstants.ERROR_RATE_WARN) {
            level = LogLevel.WARN;
        } else {
            level = LogLevel.INFO;
        }

        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("actualErrorRate", actual);
        ctx.put("configuredThreshold", threshold);
        ctx.put("failedRequests", result.getFailedRequests());
        ctx.put("totalRequests", result.getTotalRequests());
        ctx.put("thresholdBreached", breached);
        ctx.put("warnThreshold", MetricsConstants.ERROR_RATE_WARN);
        ctx.put("criticalThreshold", MetricsConstants.ERROR_RATE_CRITICAL);

        String message = breached
                ? String.format(
                "Error rate %.2f%% exceeds configured threshold %.2f%% — %d/%d requests failed",
                actual * 100, threshold * 100,
                result.getFailedRequests(), result.getTotalRequests())
                : String.format(
                "Error rate %.2f%% within threshold %.2f%% — %d/%d requests failed",
                actual * 100, threshold * 100,
                result.getFailedRequests(), result.getTotalRequests());

        return LogEntry.builder()
                .simulation(sim)
                .simulationId(sim.getId())
                .level(level)
                .category(LogCategory.PERFORMANCE)
                .serviceName(sim.getTargetService())
                .source(SOURCE_ANALYZER)
                .message(message)
                .context(ctx)
                .occurredAt(occurredAt)
                .build();
    }

    // ── Throughput analysis =>

    private LogEntry throughputAnalysisEntry(Simulation sim,
                                             SimulationResult result,
                                             Instant occurredAt) {
        double rps = result.getThroughputRps();
        boolean low = rps < MetricsConstants.THROUGHPUT_LOW_RPS;

        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("throughputRps", rps);
        ctx.put("totalRequests", result.getTotalRequests());
        ctx.put("durationSeconds", sim.getDurationSeconds());
        ctx.put("concurrentUsers", sim.getConcurrentUsers());
        ctx.put("lowThroughputThreshold", MetricsConstants.THROUGHPUT_LOW_RPS);
        ctx.put("belowThreshold", low);

        return LogEntry.builder()
                .simulation(sim)
                .simulationId(sim.getId())
                .level(low ? LogLevel.WARN : LogLevel.INFO)
                .category(LogCategory.PERFORMANCE)
                .serviceName(sim.getTargetService())
                .source(SOURCE_ANALYZER)
                .message(String.format(
                        "Throughput: %.1f req/s over %ds with %d users%s",
                        rps, sim.getDurationSeconds(), sim.getConcurrentUsers(),
                        low ? " — below minimum threshold of " +
                              MetricsConstants.THROUGHPUT_LOW_RPS + " rps" : ""))
                .context(ctx)
                .durationMs(sim.getDurationSeconds() * 1000L)
                .occurredAt(occurredAt)
                .build();
    }

    // ── Health score summary =>

    private LogEntry healthScoreEntry(Simulation sim,
                                      SimulationResult result,
                                      Instant occurredAt) {
        double score = com.analyzer.backend.common.utils.MetricsCalculator.healthScore(
                result.getP95LatencyMs(),
                result.getActualErrorRate(),
                result.getThroughputRps());
        String label = com.analyzer.backend.common.utils.MetricsCalculator.healthLabel(score);

        LogLevel level = switch (label) {
            case "CRITICAL" -> LogLevel.ERROR;
            case "DEGRADED" -> LogLevel.WARN;
            default -> LogLevel.INFO;
        };

        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("healthScore", score);
        ctx.put("healthStatus", label);
        ctx.put("p95LatencyMs", result.getP95LatencyMs());
        ctx.put("errorRate", result.getActualErrorRate());
        ctx.put("throughputRps",result.getThroughputRps());

        return LogEntry.builder()
                .simulation(sim)
                .simulationId(sim.getId())
                .level(level)
                .category(LogCategory.PERFORMANCE)
                .serviceName(sim.getTargetService())
                .source(SOURCE_ANALYZER)
                .message(String.format(
                        "Health assessment: %s (score=%.1f/100) — p95=%.0fms · error=%.2f%% · throughput=%.1frps",
                        label, score,
                        result.getP95LatencyMs(),
                        result.getActualErrorRate() * 100,
                        result.getThroughputRps()))
                .context(ctx)
                .occurredAt(occurredAt)
                .build();
    }

    // ── Failure entry =>

    private LogEntry failedEntry(Simulation sim, String reason) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("scenarioName", sim.getScenarioName());
        ctx.put("targetService", sim.getTargetService());
        ctx.put("failureReason", reason);
        ctx.put("durationSeconds", sim.getDurationSeconds());
        ctx.put("concurrentUsers", sim.getConcurrentUsers());

        return LogEntry.builder()
                .simulation(sim)
                .simulationId(sim.getId())
                .level(LogLevel.ERROR)
                .category(LogCategory.SIMULATION)
                .serviceName(sim.getTargetService())
                .source(SOURCE_SYSTEM)
                .message(String.format(
                        "Simulation '%s' failed: %s", sim.getScenarioName(), reason))
                .context(ctx)
                .stackTrace(reason)
                .occurredAt(Instant.now())
                .build();
    }
}