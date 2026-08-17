package com.analyzer.api;

import com.analyzer.common.dto.ApiResponse;
import com.analyzer.common.dto.response.DashboardSummaryDto;
import com.analyzer.common.dto.response.RecommendationDto;
import com.analyzer.common.dto.request.AnalysisRequestDto;
import com.analyzer.modules.simulation.controller.SimulationController;
import com.analyzer.modules.metrics.controller.MetricsController;
import com.analyzer.modules.logging.controller.LoggingController;
import com.analyzer.modules.ai.controller.AIController;
import com.analyzer.modules.ai.controller.RcaController;
import com.analyzer.modules.recommendation.controller.RecommendationController;
import com.analyzer.modules.dashboard.controller.DashboardController;
import com.analyzer.modules.grafana.controller.GrafanaController;
import com.analyzer.modules.grafana.dto.GrafanaWidgetData;
import com.analyzer.service_registry.controller.ServiceRegistryController;
import com.analyzer.service_registry.controller.HealthMonitorController;
import com.analyzer.service_registry.controller.BenchmarkController;
import com.analyzer.service_registry.controller.ServiceMetricsController;
import com.analyzer.service_registry.dto.BenchmarkRequestDto;
import com.analyzer.service_registry.dto.ServiceRequestDto;
import com.analyzer.modules.tracing.controller.TraceController;
import com.analyzer.modules.tracing.dto.TraceDependencyGraphDto;
import com.analyzer.modules.tracing.dto.TraceDetailDto;

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
    private final ServiceRegistryController serviceRegistryController;
    private final HealthMonitorController healthMonitorController;
    private final BenchmarkController benchmarkController;
    private final TraceController traceController;
    private final ServiceMetricsController serviceMetricsController;
    private final RcaController rcaController;
    private final GrafanaController grafanaController;

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

    @GetMapping("/dashboard/observability")
    public ResponseEntity<ApiResponse<?>> getObservabilityDashboard(
            @RequestParam(defaultValue = "24") int hours) {
        return dashboardController.getObservabilityDashboard(hours);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SERVICE REGISTRY
    // ════════════════════════════════════════════════════════════════════

    @PostMapping("/services")
    public ResponseEntity<ApiResponse<?>> registerService(
            @Valid @RequestBody ServiceRequestDto dto) {
        log.info("POST /services — name: {}", dto.getName());
        return serviceRegistryController.create(dto);
    }

    @PutMapping("/services/{id}")
    public ResponseEntity<ApiResponse<?>> updateService(
            @PathVariable String id,
            @Valid @RequestBody ServiceRequestDto dto) {
        return serviceRegistryController.update(id, dto);
    }

    @DeleteMapping("/services/{id}")
    public ResponseEntity<ApiResponse<?>> deleteService(@PathVariable String id) {
        return serviceRegistryController.delete(id);
    }

    @GetMapping("/services/{id}")
    public ResponseEntity<ApiResponse<?>> getService(@PathVariable String id) {
        return serviceRegistryController.getById(id);
    }

    @GetMapping("/services")
    public ResponseEntity<ApiResponse<?>> listServices(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String name) {
        if (name != null && !name.isBlank()) {
            return serviceRegistryController.search(name);
        }
        return serviceRegistryController.listPaged(page, size);
    }

    @PostMapping("/services/{id}/enable")
    public ResponseEntity<ApiResponse<?>> enableService(@PathVariable String id) {
        return serviceRegistryController.enable(id);
    }

    @PostMapping("/services/{id}/disable")
    public ResponseEntity<ApiResponse<?>> disableService(@PathVariable String id) {
        return serviceRegistryController.disable(id);
    }

    // ════════════════════════════════════════════════════════════════════
    //  HEALTH MONITORING
    // ════════════════════════════════════════════════════════════════════

    @GetMapping("/health/status")
    public ResponseEntity<ApiResponse<?>> getCurrentHealthStatus() {
        return healthMonitorController.getCurrentStatus();
    }

    @GetMapping("/health/history")
    public ResponseEntity<ApiResponse<?>> getAllHealthHistory(
            @RequestParam(defaultValue = "1") int hours) {
        return healthMonitorController.getAllHealthHistory(hours);
    }

    @GetMapping("/health/history/{serviceId}")
    public ResponseEntity<ApiResponse<?>> getHealthHistory(
            @PathVariable String serviceId,
            @RequestParam(defaultValue = "24") int hours) {
        return healthMonitorController.getHealthHistory(serviceId, hours);
    }

    @PostMapping("/health/check/{serviceId}")
    public ResponseEntity<ApiResponse<?>> triggerHealthCheck(@PathVariable String serviceId) {
        log.info("POST /health/check/{}", serviceId);
        return healthMonitorController.triggerHealthCheck(serviceId);
    }

    @GetMapping("/health/latency")
    public ResponseEntity<ApiResponse<?>> getAverageLatency(
            @RequestParam String serviceId,
            @RequestParam(defaultValue = "1") int hours) {
        return healthMonitorController.getAverageLatency(serviceId, hours);
    }

    @GetMapping("/health/slowest")
    public ResponseEntity<ApiResponse<?>> getTopSlowest(
            @RequestParam(defaultValue = "1") int hours) {
        return healthMonitorController.getTopSlowest(hours);
    }

    // ════════════════════════════════════════════════════════════════════
    //  BENCHMARK
    // ════════════════════════════════════════════════════════════════════

    @PostMapping("/services/{id}/benchmark")
    public ResponseEntity<ApiResponse<?>> runBenchmark(
            @PathVariable String id,
            @Valid @RequestBody BenchmarkRequestDto request) {
        log.info("POST /services/{}/benchmark — endpoint: {}", id, request.getEndpoint());
        return benchmarkController.runBenchmark(id, request);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SERVICE METRICS (Prometheus)
    // ════════════════════════════════════════════════════════════════════

    @GetMapping("/metrics/service/{id}")
    public ResponseEntity<ApiResponse<?>> getAllServiceMetrics(
            @PathVariable String id,
            @RequestParam(defaultValue = "24") int hours) {
        return serviceMetricsController.getAllMetrics(id, hours);
    }

    @GetMapping("/metrics/service/{id}/cpu")
    public ResponseEntity<ApiResponse<?>> getServiceCpuMetrics(
            @PathVariable String id,
            @RequestParam(defaultValue = "24") int hours) {
        return serviceMetricsController.getCpuMetrics(id, hours);
    }

    @GetMapping("/metrics/service/{id}/memory")
    public ResponseEntity<ApiResponse<?>> getServiceMemoryMetrics(
            @PathVariable String id,
            @RequestParam(defaultValue = "24") int hours) {
        return serviceMetricsController.getMemoryMetrics(id, hours);
    }

    @GetMapping("/metrics/service/{id}/latency")
    public ResponseEntity<ApiResponse<?>> getServiceLatencyMetrics(
            @PathVariable String id,
            @RequestParam(defaultValue = "24") int hours) {
        return serviceMetricsController.getLatencyMetrics(id, hours);
    }

    @GetMapping("/metrics/service/{id}/requests")
    public ResponseEntity<ApiResponse<?>> getServiceRequestMetrics(
            @PathVariable String id,
            @RequestParam(defaultValue = "24") int hours) {
        return serviceMetricsController.getRequestMetrics(id, hours);
    }

    // ════════════════════════════════════════════════════════════════════
    //  DISTRIBUTED TRACING
    // ════════════════════════════════════════════════════════════════════

    @GetMapping("/traces")
    public ResponseEntity<ApiResponse<?>> listTraces(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String serviceName,
            @RequestParam(defaultValue = "24") int hoursBack) {
        return traceController.listTraces(page, size, serviceName, hoursBack);
    }

    @GetMapping("/traces/{traceId}")
    public ResponseEntity<ApiResponse<TraceDetailDto>> getTrace(@PathVariable String traceId) {
        return traceController.getTrace(traceId);
    }

    @GetMapping("/traces/{traceId}/graph")
    public ResponseEntity<ApiResponse<TraceDependencyGraphDto>> getTraceGraph(
            @PathVariable String traceId) {
        return traceController.getDependencyGraph(traceId);
    }

    // ════════════════════════════════════════════════════════════════════
    //  ROOT CAUSE ANALYSIS
    // ════════════════════════════════════════════════════════════════════

    @PostMapping("/rca/{serviceId}")
    public ResponseEntity<ApiResponse<?>> analyzeRootCause(
            @PathVariable String serviceId,
            @RequestParam(required = false) String serviceName,
            @RequestParam(defaultValue = "24") int hours) {
        log.info("POST /rca/{} — hours: {}", serviceId, hours);
        return rcaController.analyzeRootCause(serviceId, serviceName, hours);
    }

    @GetMapping("/rca/{serviceId}/history")
    public ResponseEntity<ApiResponse<?>> getRcaHistory(
            @PathVariable String serviceId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return rcaController.getHistory(serviceId, page, size);
    }

    @GetMapping("/rca/{serviceId}/recent")
    public ResponseEntity<ApiResponse<?>> getRecentRca(
            @PathVariable String serviceId,
            @RequestParam(defaultValue = "24") int hours) {
        return rcaController.getRecent(serviceId, hours);
    }

    @GetMapping("/rca/{serviceId}/stats")
    public ResponseEntity<ApiResponse<?>> getRcaStats(
            @PathVariable String serviceId,
            @RequestParam(defaultValue = "24") int hours) {
        return rcaController.getStats(serviceId, hours);
    }

    // ════════════════════════════════════════════════════════════════════
    //  GRAFANA MONITORING
    // ════════════════════════════════════════════════════════════════════

    // @GetMapping("/grafana/widgets")
    // public ResponseEntity<GrafanaWidgetData> getGrafanaWidgets(
    //         @RequestParam(defaultValue = "24") int hours) {
    //     return grafanaController.getWidgets(hours);
    // }

    // ════════════════════════════════════════════════════════════════════
    //  HEALTH
    // ════════════════════════════════════════════════════════════════════

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("analyzer is running"));
    }
}