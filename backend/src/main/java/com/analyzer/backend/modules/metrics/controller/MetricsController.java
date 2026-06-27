package com.analyzer.backend.modules.metrics.controller;

import com.analyzer.backend.common.dto.ApiResponse;
import com.analyzer.backend.common.dto.PagedResponse;
import com.analyzer.backend.common.dto.response.MetricsSummaryDto;
import com.analyzer.backend.modules.metrics.model.AggregatedMetrics;
import com.analyzer.backend.modules.metrics.model.MetricSnapshot;
import com.analyzer.backend.modules.metrics.model.MetricType;
import com.analyzer.backend.modules.metrics.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsService service;

    // Called by GlobalController.getMetrics()
    public ResponseEntity<ApiResponse<?>> getMetricsForSimulation(Long simulationId) {

        List<MetricSnapshot> snapshots = service.getAllForSimulation(simulationId);

        List<MetricSnapshotDto> dtos = snapshots.stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    // Called by GlobalController.getMetricsSummary()
    public ResponseEntity<ApiResponse<?>> getSummary(Long simulationId) {
        MetricsSummaryDto summary = service.getSummary(simulationId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    // Called by GlobalController.getServiceNames()
    public ResponseEntity<ApiResponse<?>> getDistinctServices() {
        List<String> services = service.getDistinctServiceNames();
        return ResponseEntity.ok(ApiResponse.success(services));
    }

    public ResponseEntity<ApiResponse<?>> getPagedMetrics(
            Long simulationId, int page, int size) {
        Page<MetricSnapshot> paged = service.getPagedForSimulation(
                simulationId, page, size);
        PagedResponse<MetricSnapshotDto> response =
                PagedResponse.of(paged, paged.getContent().stream()
                        .map(this::toDto).toList());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    public ResponseEntity<ApiResponse<?>> getAggregated(Long simulationId) {
        AggregatedMetrics aggregated = service.getAggregated(simulationId);
        return ResponseEntity.ok(ApiResponse.success(aggregated));
    }

    public ResponseEntity<ApiResponse<?>> getProblematic(Long simulationId) {
        List<MetricSnapshot> problematic = service.getProblematic(simulationId);
        return ResponseEntity.ok(ApiResponse.success(
                problematic.stream().map(this::toDto).toList()));
    }

    // ── DTO projection =>

    private MetricSnapshotDto toDto(MetricSnapshot s) {
        return MetricSnapshotDto.builder()
                .id(s.getId())
                .simulationId(s.getSimulationId())
                .serviceName(s.getServiceName())
                .metricType(s.getMetricType().name())
                .value(s.getValue())
                .unit(s.getUnit())
                .avgMs(s.getAvgMs())
                .p95Ms(s.getP95Ms())
                .p99Ms(s.getP99Ms())
                .maxMs(s.getMaxMs())
                .totalRequests(s.getTotalRequests())
                .failedRequests(s.getFailedRequests())
                .severity(s.getSeverity().name())
                .notes(s.getNotes())
                .recordedAt(s.getRecordedAt())
                .build();
    }
}