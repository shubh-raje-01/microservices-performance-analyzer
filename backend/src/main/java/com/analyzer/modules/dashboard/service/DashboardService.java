package com.analyzer.modules.dashboard.service;

import com.analyzer.common.constants.CacheConstants;
import com.analyzer.common.dto.response.AIInsightDto;
import com.analyzer.common.dto.response.DashboardSummaryDto;
import com.analyzer.common.dto.response.MetricsSummaryDto;
import com.analyzer.common.dto.response.RecommendationDto;
import com.analyzer.common.exceptions.AnalyzerException;
import com.analyzer.modules.ai.repository.AIInsightRepository;
import com.analyzer.modules.ai.service.AIService;
import com.analyzer.modules.dashboard.dto.DashboardOverviewDto;
import com.analyzer.modules.dashboard.dto.SystemHealthDto;
import com.analyzer.modules.metrics.service.MetricsService;
import com.analyzer.modules.recommendation.model.RecommendationPriority;
import com.analyzer.modules.recommendation.repository.RecommendationRepository;
import com.analyzer.modules.recommendation.service.RecommendationService;
import com.analyzer.modules.simulation.model.Simulation;
import com.analyzer.modules.simulation.model.SimulationStatus;
import com.analyzer.modules.simulation.repository.SimulationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final SimulationRepository simulationRepository;
    private final MetricsService metricsService;
    private final AIService aiService;
    private final AIInsightRepository aiInsightRepository;
    private final RecommendationService recommendationService;
    private final RecommendationRepository recommendationRepository;

    // ── Full summary for one simulation

    /**
     * Assembles the complete dashboard view for a single simulation by
     * calling each module's already-cached read methods. Every nested
     * call (metrics, AI insight, recommendations) is independently
     * @Cacheable in its own module, so repeated dashboard requests for
     * the same simulation rarely hit the database at all.
     */
    @Cacheable(value = CacheConstants.DASHBOARD_SUMMARY, key = "#simulationId")
    public DashboardSummaryDto getSummary (Long simulationId) {

        log.debug("Assembling DashboardSummaryDto for simulation {}", simulationId);

        Simulation sim = simulationRepository.findById(simulationId)
                .orElseThrow(() -> AnalyzerException.notFound("Simulation", simulationId));

        MetricsSummaryDto metrics = safeGetMetrics(simulationId);
        AIInsightDto insight = safeGetInsight(simulationId);
        List<RecommendationDto> recommendations = safeGetRecommendations(simulationId);

        return DashboardSummaryDto.builder()
                .simulationId(sim.getId())
                .scenarioName(sim.getScenarioName())
                .targetService(sim.getTargetService())
                .simulationStatus(sim.getStatus().name())
                .metrics(metrics)
                .aiInsight(insight)
                .recommendations(recommendations)
                .simulationStartedAt(sim.getStartedAt())
                .simulationCompletedAt(sim.getCompletedAt())
                .generatedAt(java.time.Instant.now())
                .build();
    }

    // ── Recent dashboards (list view)

    /**
     * Returns lightweight overview cards for the N most recently completed
     * simulations. Deliberately avoids the full DashboardSummaryDto shape
     * (no nested MetricsSummaryDto/AIInsightDto objects) to keep list
     * payloads small.
     */
    public List<DashboardOverviewDto> getRecentOverviews (int limit) {

        List<Simulation> recent = simulationRepository
                .findAllByOrderByCreatedAtDesc(
                        PageRequest.of(0, Math.min(limit, 50)))
                .getContent();

        return recent.stream()
                .filter(s -> s.getStatus() == SimulationStatus.COMPLETED
                        || s.getStatus() == SimulationStatus.FAILED)
                .map(this::toOverview)
                .collect(Collectors.toList());
    }

    private DashboardOverviewDto toOverview (Simulation sim) {

        MetricsSummaryDto metrics = safeGetMetrics(sim.getId());
        AIInsightDto insight = safeGetInsight(sim.getId());

        int totalRecs = (int) recommendationRepository.countBySimulationId(sim.getId());
        int highPriorityRecs = (int) recommendationRepository
                .countBySimulationIdAndPriority(
                        sim.getId(),
                        RecommendationPriority.HIGH);

        return DashboardOverviewDto.builder()
                .simulationId(sim.getId())
                .scenarioName(sim.getScenarioName())
                .targetService(sim.getTargetService())
                .simulationStatus(sim.getStatus().name())
                .p95LatencyMs(metrics != null ? metrics.getP95LatencyMs() : null)
                .errorRate(metrics != null ? metrics.getErrorRate() : null)
                .throughputRps(metrics != null ? metrics.getThroughputRps() : null)
                .healthStatus(metrics != null ? metrics.getHealthStatus() : "UNKNOWN")
                .anomalyType(insight != null ? insight.getAnomalyType() : null)
                .hasAnomaly(insight != null
                        && insight.getAnomalyType() != null
                        && !"NONE".equals(insight.getAnomalyType())
                        && !"UNKNOWN".equals(insight.getAnomalyType()))
                .highPriorityRecommendationCount(highPriorityRecs)
                .totalRecommendationCount(totalRecs)
                .completedAt(sim.getCompletedAt())
                .build();
    }

    // ── System-wide health rollup

    /**
     * Aggregates health status across the most recent completed simulation
     * for each distinct target service. Used for a top-level "system health"
     * banner showing how many services are healthy right now.
     */
    public SystemHealthDto getSystemHealth () {

        List<String> serviceNames = metricsService.getDistinctServiceNames();

        List<MetricsSummaryDto> latestPerService = new ArrayList<>();
        for (String service : serviceNames) {
            simulationRepository
                    .findTopByTargetServiceOrderByCreatedAtDesc(service)
                    .filter(s -> s.getStatus() == SimulationStatus.COMPLETED)
                    .ifPresent(s -> {
                        MetricsSummaryDto m = safeGetMetrics(s.getId());
                        if (m != null) latestPerService.add(m);
                    });
        }

        List<String> criticalServices = latestPerService.stream()
                .filter(m -> "CRITICAL".equals(m.getHealthStatus()))
                .map(MetricsSummaryDto::getTargetService)
                .collect(Collectors.toList());

        List<String> degradedServices = latestPerService.stream()
                .filter(m -> "DEGRADED".equals(m.getHealthStatus()))
                .map(MetricsSummaryDto::getTargetService)
                .collect(Collectors.toList());

        long healthyCount = latestPerService.stream()
                .filter(m -> "HEALTHY".equals(m.getHealthStatus()))
                .count();

        double avgHealthScore = latestPerService.isEmpty() ? 0.0
                : latestPerService.stream()
                .mapToDouble(m -> deriveScoreFromStatus(m.getHealthStatus()))
                .average()
                .orElse(0.0);

        return SystemHealthDto.builder()
                .totalServicesAnalysed(latestPerService.size())
                .healthyCount((int) healthyCount)
                .degradedCount(degradedServices.size())
                .criticalCount(criticalServices.size())
                .criticalServiceNames(criticalServices)
                .degradedServiceNames(degradedServices)
                .averageHealthScore(Math.round(avgHealthScore * 10.0) / 10.0)
                .build();
    }

    // ── Safe fetch helpers =>
    // Each underlying module call can legitimately fail (e.g. AI analysis was
    // never triggered for this simulation, or metrics collection failed).
    // The dashboard must degrade gracefully — a missing AI insight should not
    // prevent the rest of the summary from rendering.

    private MetricsSummaryDto safeGetMetrics (Long simulationId) {
        try {
            return metricsService.getSummary(simulationId);
        } catch (Exception ex) {
            log.debug("Dashboard: no metrics available for simulation {} — {}",
                    simulationId, ex.getMessage());
            return null;
        }
    }

    private AIInsightDto safeGetInsight (Long simulationId) {
        try {
            if (!aiInsightRepository.existsBySimulationId(simulationId)) {
                return null;
            }
            return aiService.getInsight(simulationId);
        } catch (Exception ex) {
            log.debug("Dashboard: no AI insight available for simulation {} — {}",
                    simulationId, ex.getMessage());
            return null;
        }
    }

    private List<RecommendationDto> safeGetRecommendations (Long simulationId) {
        try {
            return recommendationService.getForSimulation(simulationId);
        } catch (Exception ex) {
            log.debug("Dashboard: no recommendations available for simulation {} — {}",
                    simulationId, ex.getMessage());
            return List.of();
        }
    }

    private double deriveScoreFromStatus(String status) {
        return switch (status) {
            case "HEALTHY"  -> 90.0;
            case "DEGRADED" -> 60.0;
            case "CRITICAL" -> 25.0;
            default         -> 0.0;
        };
    }
}