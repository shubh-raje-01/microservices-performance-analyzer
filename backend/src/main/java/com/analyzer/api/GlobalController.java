package com.analyzer.api;

import com.analyzer.common.dto.ApiResponse;
import com.analyzer.common.dto.response.DashboardSummaryDto;
import com.analyzer.common.dto.response.RecommendationDto;
import com.analyzer.common.dto.request.AnalysisRequestDto;
import com.analyzer.modules.simulation.controller.SimulationController;
import com.analyzer.modules.metrics.controller.MetricsController;
import com.analyzer.modules.logging.controller.LoggingController;
import com.analyzer.modules.ai.controller.AIController;
import com.analyzer.modules.recommendation.controller.RecommendationController;
import com.analyzer.modules.dashboard.controller.DashboardController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GlobalController {

    private final SimulationController simulationController;
    private final MetricsController metricsController;
    private final LoggingController loggingController;
    private final AIController aiController;
    private final RecommendationController recommendationController;
    private final DashboardController dashboardController;

    //  SIMULATION

    @PostMapping("/simulate")
    public ResponseEntity<ApiResponse<?>> runSimulation(
            @Valid @RequestBody AnalysisRequestDto request) {
        log.info("POST /simulate — scenario: {}", request.getScenarioName());
        return simulationController.runSimulation(request);
    }

    @GetMapping("/simulate/{id}")
    public ResponseEntity<ApiResponse<?>> getSimulation(@PathVariable Long id) {
        return simulationController.getSimulationById(id);
    }

    @GetMapping("/simulate")
    public ResponseEntity<ApiResponse<?>> listSimulations(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return simulationController.listSimulations(page, size);
    }

    @DeleteMapping("/simulate/{id}")
    public ResponseEntity<ApiResponse<?>> deleteSimulation(@PathVariable Long id) {
        return simulationController.deleteSimulation(id);
    }

    //  METRICS

    @GetMapping("/metrics/{simulationId}")
    public ResponseEntity<ApiResponse<?>> getMetrics(@PathVariable Long simulationId) {
        return metricsController.getMetricsForSimulation(simulationId);
    }

    @GetMapping("/metrics/{simulationId}/summary")
    public ResponseEntity<ApiResponse<?>> getMetricsSummary(@PathVariable Long simulationId) {
        return metricsController.getSummary(simulationId);
    }

    @GetMapping("/metrics/{simulationId}/paged")
    public ResponseEntity<ApiResponse<?>> getPagedMetrics(
            @PathVariable Long simulationId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return metricsController.getPagedMetrics(simulationId, page, size);
    }

    @GetMapping("/metrics/{simulationId}/aggregated")
    public ResponseEntity<ApiResponse<?>> getAggregatedMetrics(@PathVariable Long simulationId) {
        return metricsController.getAggregated(simulationId);
    }

    @GetMapping("/metrics/{simulationId}/problematic")
    public ResponseEntity<ApiResponse<?>> getProblematicMetrics(@PathVariable Long simulationId) {
        return metricsController.getProblematic(simulationId);
    }

    @GetMapping("/metrics/services")
    public ResponseEntity<ApiResponse<?>> getServiceNames() {
        return metricsController.getDistinctServices();
    }

    //  LOGS

    @GetMapping("/logs/{simulationId}")
    public ResponseEntity<ApiResponse<?>> getLogs(
            @PathVariable Long simulationId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        return loggingController.getLogsForSimulation(simulationId, page, size);
    }

    @GetMapping("/logs/{simulationId}/summary")
    public ResponseEntity<ApiResponse<?>> getLogSummary(@PathVariable Long simulationId) {
        return loggingController.getLogSummary(simulationId);
    }

    @GetMapping("/logs/{simulationId}/errors")
    public ResponseEntity<ApiResponse<?>> getLogErrors(@PathVariable Long simulationId) {
        return loggingController.getErrors(simulationId);
    }

    @GetMapping("/logs/{simulationId}/problematic")
    public ResponseEntity<ApiResponse<?>> getProblematicLogs(@PathVariable Long simulationId) {
        return loggingController.getProblematic(simulationId);
    }

    @GetMapping("/logs/search")
    public ResponseEntity<ApiResponse<?>> searchLogs(
            @RequestParam(required = false) Long   simulationId,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        return loggingController.search(
                simulationId, level, category, keyword, from, to, page, size);
    }

    //  AI ANALYSIS

    @PostMapping("/analyze/{simulationId}")
    public ResponseEntity<ApiResponse<?>> analyzeSimulation(@PathVariable Long simulationId) {
        log.info("POST /analyze/{}", simulationId);
        return aiController.analyzeSimulation(simulationId);
    }

    @GetMapping("/analyze/{simulationId}/insights")
    public ResponseEntity<ApiResponse<?>> getInsights(@PathVariable Long simulationId) {
        return aiController.getInsights(simulationId);
    }

    @GetMapping("/analyze/anomalies/recent")
    public ResponseEntity<ApiResponse<?>> getRecentAnomalies(
            @RequestParam(defaultValue = "24") int hoursBack) {
        return aiController.getRecentAnomalies(hoursBack);
    }

    @GetMapping("/analyze/health")
    public ResponseEntity<ApiResponse<?>> getAIHealth() {
        return aiController.getFastAPIHealth();
    }

    //  RECOMMENDATIONS

    @GetMapping("/recommendations/{simulationId}")
    public ResponseEntity<ApiResponse<List<RecommendationDto>>> getRecommendations(
            @PathVariable Long simulationId) {
        return recommendationController.getRecommendations(simulationId);
    }

    @GetMapping("/recommendations/{simulationId}/high-priority")
    public ResponseEntity<ApiResponse<?>> getHighPriorityRecommendations(
            @PathVariable Long simulationId) {
        return recommendationController.getHighPriority(simulationId);
    }

    @GetMapping("/recommendations/{simulationId}/category/{category}")
    public ResponseEntity<ApiResponse<?>> getRecommendationsByCategory(
            @PathVariable Long   simulationId,
            @PathVariable String category) {
        return recommendationController.getByCategory(simulationId, category);
    }

    @GetMapping("/recommendations/{simulationId}/top")
    public ResponseEntity<ApiResponse<?>> getTopRecommendations(
            @PathVariable Long simulationId,
            @RequestParam(defaultValue = "5") int n) {
        return recommendationController.getTopN(simulationId, n);
    }

    @GetMapping("/recommendations/{simulationId}/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getRecommendationStats(
            @PathVariable Long simulationId) {
        return recommendationController.getStats(simulationId);
    }

    //  DASHBOARD

    @GetMapping("/dashboard/{simulationId}")
    public ResponseEntity<ApiResponse<DashboardSummaryDto>> getDashboard(
            @PathVariable Long simulationId) {
        return dashboardController.getSummary(simulationId);
    }

    @GetMapping("/dashboard/recent")
    public ResponseEntity<ApiResponse<?>> getRecentDashboard(
            @RequestParam(defaultValue = "10") int limit) {
        return dashboardController.getRecentSummaries(limit);
    }

    @GetMapping("/dashboard/system-health")
    public ResponseEntity<ApiResponse<?>> getSystemHealth() {
        return dashboardController.getSystemHealth();
    }

    //  HEALTH

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("analyzer is running"));
    }
}