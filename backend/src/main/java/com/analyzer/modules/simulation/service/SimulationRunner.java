package com.analyzer.modules.simulation.service;

import com.analyzer.modules.simulation.model.Simulation;
import com.analyzer.modules.simulation.model.SimulationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
public class SimulationRunner {

    private static final Random RNG = new Random();

    public SimulationResult run(Simulation sim) {
        log.info("SimulationRunner starting — scenario={} service={} users={} duration={}s",
                sim.getScenarioName(), sim.getTargetService(), sim.getConcurrentUsers(), sim.getDurationSeconds());

        List<Double> throughputWindows = generateThroughputWindows(sim);

        double avgThroughput = throughputWindows.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double peakThroughput = throughputWindows.stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        double throughputVariability = avgThroughput > 0
                ? (peakThroughput - avgThroughput) / avgThroughput
                : 0.0;

        int totalRequests = (int) Math.round(
                avgThroughput * sim.getDurationSeconds()
        );

        List<Double> latencies = generateLatencies(totalRequests, sim);

        Collections.sort(latencies);

        double avgLatency = latencies.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double p95Latency = percentile(latencies, 95);
        double p99Latency = percentile(latencies, 99);

        double maxLatency = latencies.stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        long failedRequests = Math.round(totalRequests * injectErrorRate(sim));
        double actualError = (double) failedRequests / totalRequests;

        log.info("SimulationRunner complete — avg={:.1f}ms p95={:.1f}ms p99={:.1f}ms " +
                "rps={:.1f} errorRate={:.4f}",
                avgLatency, p95Latency, p99Latency, maxLatency, failedRequests, actualError);

        return SimulationResult.builder()
                .avgLatencyMs(round(avgLatency))
                .p95LatencyMs(round(p95Latency))
                .p99LatencyMs(round(p99Latency))
                .maxLatencyMs(round(maxLatency))
                .throughputRps(round(avgThroughput))
                .peakThroughputRps(round(peakThroughput))
                .throughputVariability(round(throughputVariability))
                .actualErrorRate(round(actualError))
                .totalRequests(totalRequests)
                .failedRequests(failedRequests)
                .build();
    }

    private List<Double> generateLatencies(int count, Simulation sim) {
        List<Double> latencies = new ArrayList<>(count);
        // Base latency increases with concurrency load
        double baseMean = 80 + (sim.getConcurrentUsers() * 0.8);

        for (int i = 0; i < count; i++) {
            double latency;
            double roll = RNG.nextDouble();
            if (roll < 0.70) {
                // 70% — fast requests (normal distribution around mean)
                latency = Math.max(5, baseMean + RNG.nextGaussian() * (baseMean * 0.25));
            } else if (roll < 0.92) {
                // 22% — moderate tail
                latency = baseMean * (1.5 + RNG.nextDouble() * 2.0);
            } else if (roll < 0.99) {
                // 7% — long tail
                latency = baseMean * (4 + RNG.nextDouble() * 6.0);
            } else {
                // 1% — outliers (GC pauses, cold starts, etc.)
                latency = baseMean * (15 + RNG.nextDouble() * 10.0);
            }
            latencies.add(latency);
        }
        return latencies;
    }

    private double injectErrorRate(Simulation sim) {
        // Errors grow with concurrency beyond a threshold
        double baseError = 0.01;
        if (sim.getConcurrentUsers() > 500) {
            baseError += (sim.getConcurrentUsers() - 500) * 0.00005;
        }
        // Add noise around threshold for realism
        return Math.min(1.0, baseError + RNG.nextDouble() * 0.02);
    }

    private double percentile(List<Double> sorted, int p) {
        if (sorted.isEmpty()) return 0;
        int idx = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(idx, sorted.size() - 1)));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private List<Double> generateThroughputWindows(Simulation sim) {
        int duration = Math.max(1, sim.getDurationSeconds());

        // Keep the number of windows bounded for longer simulations.
        int windowCount = Math.max(1, Math.min(12, (int) Math.ceil(duration / 5.0)));

        double baseRps = Math.max(1.0, sim.getConcurrentUsers() / 5.0);

        List<Double> windows = new ArrayList<>(windowCount);

        for (int i = 0; i < windowCount; i++) {
            double loadFactor = 0.90 + RNG.nextDouble() * 0.20;

            // Small periodic pressure variation prevents a perfectly flat profile.
            double phase = (double) i / Math.max(1, windowCount - 1);
            double pressureFactor = 1.0 + Math.sin(phase * Math.PI) * 0.08;

            double rps = baseRps * loadFactor * pressureFactor;

            windows.add(Math.max(1.0, rps));
        }

        return windows;
    }

}
