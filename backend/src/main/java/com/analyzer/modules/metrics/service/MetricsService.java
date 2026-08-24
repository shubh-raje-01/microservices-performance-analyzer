package com.analyzer.modules.metrics.service;

import com.analyzer.common.constants.CacheConstants;
import com.analyzer.common.dto.response.MetricsSummaryDto;
import com.analyzer.common.exceptions.AnalyzerException;
import com.analyzer.common.utils.MetricsCalculator;
import com.analyzer.modules.metrics.model.AggregatedMetrics;
import com.analyzer.modules.metrics.model.MetricSeverity;
import com.analyzer.modules.metrics.model.MetricSnapshot;
import com.analyzer.modules.metrics.model.MetricType;
import com.analyzer.modules.metrics.repository.MetricsRepository;
import com.analyzer.modules.simulation.model.Simulation;
import com.analyzer.modules.simulation.repository.SimulationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MetricsService {

    private final MetricsRepository    metricsRepository;
    private final SimulationRepository simulationRepository;

    // ── Primary read operations =>

    public List<MetricSnapshot> getAllForSimulation(Long simulationId) {
        guardSimulationExists(simulationId);
        return metricsRepository.findBySimulationIdOrderByRecordedAtAsc(simulationId);
    }

    public Page<MetricSnapshot> getPagedForSimulation(Long simulationId,
                                                      int page, int size) {
        guardSimulationExists(simulationId);
        return metricsRepository.findBySimulationId(
                simulationId, PageRequest.of(page, Math.min(size, 100)));
    }

    public List<MetricSnapshot> getByType(Long simulationId, MetricType type) {
        return metricsRepository.findBySimulationIdAndMetricType(simulationId, type);
    }

    public List<MetricSnapshot> getProblematic(Long simulationId) {
        return metricsRepository.findProblematicBySimulation(simulationId);
    }

    public List<MetricSnapshot> getForServiceSince(String serviceName,
                                                   MetricType type,
                                                   int hoursBack) {
        Instant since = Instant.now().minusSeconds(hoursBack * 3600L);
        return metricsRepository.findRecentByServiceAndType(serviceName, type, since);
    }

    public List<String> getDistinctServiceNames() {
        return metricsRepository.findDistinctServiceNames();
    }

    // ── Summary DTO (cached, used by dashboard + AI module)

    @Cacheable(value = CacheConstants.METRICS_SUMMARY, key = "#simulationId")
    public MetricsSummaryDto getSummary(Long simulationId) {
        log.debug("Building MetricsSummaryDto for simulation {}", simulationId);

        Simulation sim = simulationRepository.findById(simulationId)
                .orElseThrow(() -> AnalyzerException.notFound("Simulation", simulationId));

        Double avgP95 = metricsRepository.avgP95BySimulation(simulationId)
                .orElse(0.0);
        Double avgP99 = metricsRepository.avgP99BySimulation(simulationId)
                .orElse(0.0);
        Double maxLatency = metricsRepository.maxLatencyBySimulation(simulationId)
                .orElse(0.0);

        Double avgLatency = metricsRepository
                .avgValueBySimulationAndType(simulationId, MetricType.LATENCY)
                .orElse(0.0);
        Double avgThroughput = metricsRepository
                .avgValueBySimulationAndType(simulationId, MetricType.THROUGHPUT)
                .orElse(0.0);
        Double avgErrorRate = metricsRepository
                .avgValueBySimulationAndType(simulationId, MetricType.ERROR_RATE)
                .orElse(0.0);

        long totalReqs   = sim.getTotalRequests()  != null ? sim.getTotalRequests()  : 0L;
        long failedReqs  = sim.getFailedRequests() != null ? sim.getFailedRequests() : 0L;

        String healthStatus = MetricsSummaryDto.deriveHealth(avgErrorRate, avgP95);
        double healthScore  = MetricsCalculator.healthScore(avgP95, avgErrorRate, avgThroughput);

        return MetricsSummaryDto.builder()
                .simulationId(simulationId)
                .targetService(sim.getTargetService())
                .avgLatencyMs(MetricsCalculator.round(avgLatency,   2))
                .p95LatencyMs(MetricsCalculator.round(avgP95,       2))
                .p99LatencyMs(MetricsCalculator.round(avgP99,       2))
                .maxLatencyMs(MetricsCalculator.round(maxLatency, 2))
                .throughputRps(MetricsCalculator.round(avgThroughput, 2))
                .totalRequests(totalReqs)
                .failedRequests(failedReqs)
                .errorRate(MetricsCalculator.round(avgErrorRate, 6))
                .healthStatus(healthStatus)
                .healthScore(MetricsCalculator.round(healthScore, 1))
                .build();
    }

    // ── Full aggregation (used by AI module for feature input) ─

    @Cacheable(value = CacheConstants.METRICS_BY_SIM, key = "#simulationId")
    public AggregatedMetrics getAggregated(Long simulationId) {
        log.debug("Building AggregatedMetrics for simulation {}", simulationId);

        Simulation sim = simulationRepository.findById(simulationId)
                .orElseThrow(() -> AnalyzerException.notFound("Simulation", simulationId));

        List<MetricSnapshot> all = metricsRepository
                .findBySimulationIdOrderByRecordedAtAsc(simulationId);

        if (all.isEmpty()) {
            throw AnalyzerException.notFound("Metrics for simulation", simulationId);
        }

        // Extract latency snapshots for statistical computation
        List<Double> latencyValues = all.stream()
                .filter(MetricSnapshot::isLatency)
                .filter(m -> m.getAvgMs() != null)
                .map(MetricSnapshot::getAvgMs)
                .collect(Collectors.toList());

        List<Double> p95Values = all.stream()
                .filter(MetricSnapshot::isLatency)
                .filter(m -> m.getP95Ms() != null)
                .map(MetricSnapshot::getP95Ms)
                .collect(Collectors.toList());

        List<Double> throughputValues = all.stream()
                .filter(m -> m.getMetricType() == MetricType.THROUGHPUT)
                .map(MetricSnapshot::getValue)
                .collect(Collectors.toList());

        List<Double> errorRateValues = all.stream()
                .filter(m -> m.getMetricType() == MetricType.ERROR_RATE)
                .map(MetricSnapshot::getValue)
                .collect(Collectors.toList());

        // Snapshot count by type
        Map<String, Long> countByType = all.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getMetricType().name(),
                        Collectors.counting()));

        // Critical snapshots — surface for the dashboard
        List<MetricSnapshot> criticals = all.stream()
                .filter(MetricSnapshot::isCritical)
                .collect(Collectors.toList());

        // Worst severity across all snapshots
        MetricSeverity worst = all.stream()
                .map(MetricSnapshot::getSeverity)
                .max(Enum::compareTo)
                .orElse(MetricSeverity.NORMAL);

        double avgP95       = MetricsCalculator.mean(p95Values);
        double avgErrorRate = MetricsCalculator.mean(errorRateValues);
        double avgThroughput = MetricsCalculator.mean(throughputValues);

        long totalRequests  = sim.getTotalRequests()  != null ? sim.getTotalRequests()  : 0L;
        long failedRequests = sim.getFailedRequests() != null ? sim.getFailedRequests() : 0L;

        return AggregatedMetrics.builder()
                .simulationId(simulationId)
                .serviceName(sim.getTargetService())
                .avgLatencyMs(MetricsCalculator.round(MetricsCalculator.mean(latencyValues), 2))
                .p50LatencyMs(MetricsCalculator.round(MetricsCalculator.p50(latencyValues),  2))
                .p95LatencyMs(MetricsCalculator.round(avgP95, 2))
                .p99LatencyMs(MetricsCalculator.round(
                        metricsRepository.avgP99BySimulation(simulationId).orElse(0.0), 2))
                .maxLatencyMs(MetricsCalculator.round(MetricsCalculator.max(latencyValues),  2))
                .stdDevLatencyMs(MetricsCalculator.round(MetricsCalculator.stdDev(latencyValues), 2))
                .avgThroughputRps(MetricsCalculator.round(avgThroughput, 2))
                .peakThroughputRps(
                        MetricsCalculator.round(
                                sim.getPeakThroughputRps() != null
                                        ? sim.getPeakThroughputRps()
                                        : 0.0,
                                2))
                .avgErrorRate(MetricsCalculator.round(avgErrorRate,                        6))
                .maxErrorRate(MetricsCalculator.round(MetricsCalculator.max(errorRateValues), 6))
                .totalRequests(totalRequests)
                .totalFailedRequests(failedRequests)
                .healthStatus(MetricsSummaryDto.deriveHealth(avgErrorRate, avgP95))
                .healthScore(MetricsCalculator.round(
                        MetricsCalculator.healthScore(avgP95, avgErrorRate, avgThroughput), 1))
                .worstSeverity(worst)
                .windowStart(all.get(0).getRecordedAt())
                .windowEnd(all.get(all.size() - 1).getRecordedAt())
                .snapshotCountByType(countByType)
                .criticalSnapshots(criticals)
                .throughputVariability(
                        MetricsCalculator.round(
                                sim.getThroughputVariability() != null
                                        ? sim.getThroughputVariability()
                                        : 0.0,
                                4))
                .build();
    }

    // ── Stats for cross-simulation trending =>

    public Map<MetricType, Double> getAveragesByType(Long simulationId) {
        Map<MetricType, Double> result = new EnumMap<>(MetricType.class);
        for (MetricType type : MetricType.values()) {
            metricsRepository.avgValueBySimulationAndType(simulationId, type)
                    .ifPresent(avg -> result.put(type, MetricsCalculator.round(avg, 4)));
        }
        return result;
    }

    // ── Cascade delete =>

    @Transactional
    @CacheEvict(value = {
            CacheConstants.METRICS_SUMMARY,
            CacheConstants.METRICS_BY_SIM
    }, key = "#simulationId")
    public void deleteForSimulation(Long simulationId) {
        metricsRepository.deleteBySimulationId(simulationId);
        log.info("Metrics deleted for simulation {}", simulationId);
    }

    // ── Guard =>

    private void guardSimulationExists(Long simulationId) {
        if (!simulationRepository.existsById(simulationId)) {
            throw AnalyzerException.notFound("Simulation", simulationId);
        }
    }
}