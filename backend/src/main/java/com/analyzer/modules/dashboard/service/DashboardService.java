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
import com.analyzer.modules.dashboard.dto.ObservabilityDashboardDto;
import com.analyzer.modules.dashboard.dto.SystemHealthDto;
import com.analyzer.modules.metrics.service.MetricsService;
import com.analyzer.modules.recommendation.model.RecommendationPriority;
import com.analyzer.modules.recommendation.repository.RecommendationRepository;
import com.analyzer.modules.recommendation.service.RecommendationService;
import com.analyzer.modules.simulation.model.Simulation;
import com.analyzer.modules.simulation.model.SimulationStatus;
import com.analyzer.modules.simulation.repository.SimulationRepository;
import com.analyzer.service_registry.model.Service;
import com.analyzer.service_registry.model.ServiceHealthHistory;
import com.analyzer.service_registry.model.ServiceMetrics;
import com.analyzer.service_registry.model.ServiceStatus;
import com.analyzer.service_registry.repository.ServiceHealthHistoryRepository;
import com.analyzer.service_registry.repository.ServiceMetricsRepository;
import com.analyzer.service_registry.repository.ServiceRepository;
import com.analyzer.service_registry.service.ExternalMetricsCollectorService;
import com.analyzer.service_registry.service.HealthMonitorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;

import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final SimulationRepository simulationRepository;
    private final MetricsService metricsService;
    private final AIService aiService;
    private final AIInsightRepository aiInsightRepository;
    private final RecommendationService recommendationService;
    private final RecommendationRepository recommendationRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceHealthHistoryRepository healthHistoryRepository;
    private final ServiceMetricsRepository serviceMetricsRepository;
    private final ExternalMetricsCollectorService metricsCollectorService;
    private final HealthMonitorService healthMonitorService;

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

    // ── Observability Dashboard ───────────────────────────────────────

    public ObservabilityDashboardDto getObservabilityDashboard(int historyHours) {
        List<Service> enabledServices = serviceRepository.findAllEnabled();

        // Status summary
        long online = enabledServices.stream().filter(s -> s.getStatus() == ServiceStatus.ONLINE).count();
        long offline = enabledServices.stream().filter(s -> s.getStatus() == ServiceStatus.OFFLINE).count();
        long degraded = enabledServices.stream().filter(s -> s.getStatus() == ServiceStatus.DEGRADED).count();
        long unknown = enabledServices.stream().filter(s -> s.getStatus() == ServiceStatus.UNKNOWN).count();
        long disabled = serviceRepository.findAll().size() - enabledServices.size();

        ObservabilityDashboardDto.ServiceStatusSummary statusSummary =
                ObservabilityDashboardDto.ServiceStatusSummary.builder()
                        .totalServices(enabledServices.size())
                        .onlineCount(online)
                        .offlineCount(offline)
                        .degradedCount(degraded)
                        .unknownCount(unknown)
                        .disabledCount(disabled)
                        .build();

        // Latency summary across all services
        java.time.Instant since = java.time.Instant.now().minusSeconds(historyHours * 3600L);
        List<Object[]> slowestData = healthHistoryRepository.findSlowestServices(since);
        List<Double> allLatencies = new ArrayList<>();
        for (Object[] row : slowestData) {
            if (row[1] != null) {
                allLatencies.add(((Number) row[1]).doubleValue());
            }
        }

        ObservabilityDashboardDto.LatencySummary latencySummary =
                ObservabilityDashboardDto.LatencySummary.builder()
                        .averageLatencyMs(allLatencies.isEmpty() ? 0 :
                                allLatencies.stream().mapToDouble(d -> d).average().orElse(0))
                        .p50LatencyMs(com.analyzer.common.utils.MetricsCalculator.p50(allLatencies))
                        .p95LatencyMs(com.analyzer.common.utils.MetricsCalculator.p95(allLatencies))
                        .p99LatencyMs(com.analyzer.common.utils.MetricsCalculator.p99(allLatencies))
                        .build();

        // Service details
        List<ObservabilityDashboardDto.ServiceStatusDto> serviceDetails = enabledServices.stream()
                .map(s -> {
                    Double avgLatency = healthHistoryRepository.averageLatencyByService(s.getId(), since);
                    Map<String, Double> latestMetrics = metricsCollectorService.getLatestMetrics(s.getId());
                    return ObservabilityDashboardDto.ServiceStatusDto.builder()
                            .serviceId(s.getId())
                            .serviceName(s.getName())
                            .status(s.getStatus())
                            .enabled(s.isEnabled())
                            .lastHeartbeat(s.getLastHeartbeat())
                            .avgLatencyMs(avgLatency)
                            .latestMetrics(latestMetrics)
                            .build();
                })
                .collect(Collectors.toList());

        // Recent failures
        List<ServiceHealthHistory> recentHistory = healthHistoryRepository.findRecentAll(since);
        List<ObservabilityDashboardDto.FailureEntry> recentFailures = recentHistory.stream()
                .filter(h -> h.getStatus() == ServiceStatus.OFFLINE && h.getErrorMessage() != null)
                .sorted((a, b) -> b.getCheckTime().compareTo(a.getCheckTime()))
                .limit(20)
                .map(h -> ObservabilityDashboardDto.FailureEntry.builder()
                        .serviceId(h.getService().getId())
                        .serviceName(h.getService().getName())
                        .errorMessage(h.getErrorMessage())
                        .occurredAt(h.getCheckTime())
                        .build())
                .collect(Collectors.toList());

        // Health timeline per service
        List<ObservabilityDashboardDto.HealthTimelineEntry> healthTimeline = enabledServices.stream()
                .map(s -> {
                    List<ServiceHealthHistory> history = healthHistoryRepository
                            .findTop20ByServiceIdOrderByCheckTimeDesc(s.getId());
                    List<ObservabilityDashboardDto.TimelinePoint> points = history.stream()
                            .sorted((a, b) -> a.getCheckTime().compareTo(b.getCheckTime()))
                            .map(h -> ObservabilityDashboardDto.TimelinePoint.builder()
                                    .time(h.getCheckTime())
                                    .status(h.getStatus())
                                    .latencyMs(h.getLatencyMs())
                                    .build())
                            .collect(Collectors.toList());
                    return ObservabilityDashboardDto.HealthTimelineEntry.builder()
                            .serviceId(s.getId())
                            .serviceName(s.getName())
                            .points(points)
                            .build();
                })
                .collect(Collectors.toList());

        // Top slowest services
        List<ObservabilityDashboardDto.TopSlowestService> topSlowest = slowestData.stream()
                .limit(10)
                .map(row -> {
                    String serviceId = (String) row[0];
                    double avgLat = row[1] != null ? ((Number) row[1]).doubleValue() : 0;
                    Service service = serviceRepository.findById(serviceId).orElse(null);
                    long checkCount = recentHistory.stream()
                            .filter(h -> h.getService().getId().equals(serviceId))
                            .count();
                    return ObservabilityDashboardDto.TopSlowestService.builder()
                            .serviceId(serviceId)
                            .serviceName(service != null ? service.getName() : serviceId)
                            .averageLatencyMs(avgLat)
                            .checkCount(checkCount)
                            .build();
                })
                .collect(Collectors.toList());

        return ObservabilityDashboardDto.builder()
                .statusSummary(statusSummary)
                .latencySummary(latencySummary)
                .services(serviceDetails)
                .recentFailures(recentFailures)
                .healthTimeline(healthTimeline)
                .topSlowestServices(topSlowest)
                .generatedAt(java.time.Instant.now())
                .build();
    }
}