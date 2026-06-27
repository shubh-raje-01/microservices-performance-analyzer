package com.analyzer.backend.modules.metrics.collector;

import com.analyzer.backend.common.exceptions.MetricsCollectionException;
import com.analyzer.backend.modules.metrics.model.MetricSeverity;
import com.analyzer.backend.modules.metrics.model.MetricSnapshot;
import com.analyzer.backend.modules.metrics.model.MetricType;
import com.analyzer.backend.modules.metrics.repository.MetricsRepository;
import com.analyzer.backend.modules.simulation.events.SimulationCompletedEvent;
import com.analyzer.backend.modules.simulation.events.SimulationFailedEvent;
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
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsCollector {

    private final MetricsRepository        repository;
    private final MetricsSeverityEvaluator severityEvaluator;

    /**
     * Triggered automatically when SimulationService publishes
     * SimulationCompletedEvent. Runs on the async task executor
     * so it does not block the simulation thread.
     */
    @Async
    @EventListener
    @Transactional
    public void onSimulationCompleted(SimulationCompletedEvent event) {
        Simulation sim = event.getSimulation();
        SimulationResult result = event.getResult();

        log.info("MetricsCollector processing simulation {} — service={}",
                sim.getId(), sim.getTargetService());

        try {
            List<MetricSnapshot> snapshots = buildSnapshots(sim, result);
            repository.saveAll(snapshots);
            log.info("MetricsCollector persisted {} snapshots for simulation {}",
                    snapshots.size(), sim.getId());
        } catch (Exception ex) {
            log.error("MetricsCollector failed for simulation {}: {}",
                    sim.getId(), ex.getMessage(), ex);
            throw new MetricsCollectionException(sim.getId(), ex.getMessage());
        }
    }

    /**
     * Collects a minimal failure record so the dashboard
     * can show why a simulation produced no metrics.
     */
    @Async
    @EventListener
    @Transactional
    public void onSimulationFailed(SimulationFailedEvent event) {
        Simulation sim = event.getSimulation();
        log.warn("MetricsCollector recording failure snapshot for simulation {}",
                sim.getId());

        MetricSnapshot failureRecord = MetricSnapshot.builder()
                .simulation(sim)
                .simulationId(sim.getId())
                .serviceName(sim.getTargetService())
                .metricType(MetricType.AVAILABILITY)
                .value(0.0)
                .unit("ratio")
                .severity(MetricSeverity.CRITICAL)
                .notes("Simulation failed: " + event.getReason())
                .recordedAt(Instant.now())
                .build();

        repository.save(failureRecord);
    }

    // ── Snapshot builders =>

    private List<MetricSnapshot> buildSnapshots(Simulation sim,
                                                SimulationResult result) {
        List<MetricSnapshot> snapshots = new ArrayList<>();
        Instant now = Instant.now();

        snapshots.add(buildLatencySnapshot(sim, result, now));
        snapshots.add(buildThroughputSnapshot(sim, result, now));
        snapshots.add(buildErrorRateSnapshot(sim, result, now));
        snapshots.add(buildConcurrencySnapshot(sim, now));
        snapshots.add(buildAvailabilitySnapshot(sim, result, now));

        return snapshots;
    }

    private MetricSnapshot buildLatencySnapshot(Simulation sim,
                                                SimulationResult result,
                                                Instant recordedAt) {
        double p95 = result.getP95LatencyMs();
        MetricSeverity sv = severityEvaluator.evaluateP95Latency(p95);
        String notes = severityEvaluator.buildLatencyNote(
                result.getAvgLatencyMs(), p95,
                result.getP99LatencyMs(),
                sim.getErrorRateThreshold());

        return MetricSnapshot.builder()
                .simulation(sim)
                .simulationId(sim.getId())
                .serviceName(sim.getTargetService())
                .metricType(MetricType.LATENCY)
                .value(result.getAvgLatencyMs())    // primary value = avg
                .unit("ms")
                .avgMs(result.getAvgLatencyMs())
                .p95Ms(p95)
                .p99Ms(result.getP99LatencyMs())
                .maxMs(result.getP99LatencyMs() * 1.1) // estimate max from p99
                .severity(sv)
                .notes(notes)
                .recordedAt(recordedAt)
                .build();
    }

    private MetricSnapshot buildThroughputSnapshot (
            Simulation sim,
            SimulationResult result,
            Instant recordedAt
    ) {
        double rps = result.getThroughputRps();
        MetricSeverity sv = severityEvaluator.evaluate(MetricType.THROUGHPUT, rps);

        return MetricSnapshot.builder()
                .simulation(sim)
                .simulationId(sim.getId())
                .serviceName(sim.getTargetService())
                .metricType(MetricType.THROUGHPUT)
                .value(rps)
                .unit("rps")
                .totalRequests(result.getTotalRequests())
                .severity(sv)
                .notes(String.format("%.1f req/s over %ds",
                        rps, sim.getDurationSeconds()))
                .recordedAt(recordedAt)
                .build();
    }

    private MetricSnapshot buildErrorRateSnapshot (
            Simulation sim,
            SimulationResult result,
            Instant recordedAt
    ) {
        double errorRate = result.getActualErrorRate();
        MetricSeverity sv = severityEvaluator.evaluate(MetricType.ERROR_RATE, errorRate);

        boolean thresholdBreached = errorRate > sim.getErrorRateThreshold();
        String notes = thresholdBreached
                ? String.format("Error rate %.2f%% exceeds configured threshold %.2f%%",
                errorRate * 100, sim.getErrorRateThreshold() * 100)
                : String.format("Error rate %.2f%% within threshold %.2f%%",
                errorRate * 100, sim.getErrorRateThreshold() * 100);

        return MetricSnapshot.builder()
                .simulation(sim)
                .simulationId(sim.getId())
                .serviceName(sim.getTargetService())
                .metricType(MetricType.ERROR_RATE)
                .value(errorRate)
                .unit("ratio")
                .totalRequests(result.getTotalRequests())
                .failedRequests(result.getFailedRequests())
                .severity(sv)
                .notes(notes)
                .recordedAt(recordedAt)
                .build();
    }

    private MetricSnapshot buildConcurrencySnapshot (Simulation sim,
                                                    Instant recordedAt) {
        int users = sim.getConcurrentUsers();
        MetricSeverity sv = users > 1000
                ? MetricSeverity.WARNING : MetricSeverity.NORMAL;

        return MetricSnapshot.builder()
                .simulation(sim)
                .simulationId(sim.getId())
                .serviceName(sim.getTargetService())
                .metricType(MetricType.CONCURRENCY)
                .value((double) users)
                .unit("threads")
                .severity(sv)
                .notes(users + " concurrent users for " + sim.getDurationSeconds() + "s")
                .recordedAt(recordedAt)
                .build();
    }

    private MetricSnapshot buildAvailabilitySnapshot(Simulation sim,
                                                     SimulationResult result,
                                                     Instant recordedAt) {
        double availability = 1.0 - result.getActualErrorRate();
        MetricSeverity sv = severityEvaluator.evaluate(
                MetricType.AVAILABILITY, availability);

        return MetricSnapshot.builder()
                .simulation(sim)
                .simulationId(sim.getId())
                .serviceName(sim.getTargetService())
                .metricType(MetricType.AVAILABILITY)
                .value(availability)
                .unit("ratio")
                .totalRequests(result.getTotalRequests())
                .failedRequests(result.getFailedRequests())
                .severity(sv)
                .notes(String.format("%.4f%% availability (%d/%d successful)",
                        availability * 100,
                        result.getTotalRequests() - result.getFailedRequests(),
                        result.getTotalRequests()))
                .recordedAt(recordedAt)
                .build();
    }
}