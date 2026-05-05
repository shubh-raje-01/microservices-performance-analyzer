package com.analyzer.backend.modules.simulation.controller;

import com.analyzer.backend.common.dto.ApiResponse;
import com.analyzer.backend.common.dto.request.AnalysisRequestDto;
import com.analyzer.backend.modules.simulation.model.Simulation;
import com.analyzer.backend.modules.simulation.service.SimulationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService service;

    public ResponseEntity<ApiResponse<?>> runSimulation(
            @Valid @RequestBody AnalysisRequestDto dto) {
        Simulation simulation = service.createAndRun(dto);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)          // 202 — async, not yet done
                .body(ApiResponse.success(
                        "Simulation started — poll /simulate/" + simulation.getId(),
                        toDto(simulation)
                ));
    }

    public ResponseEntity<ApiResponse<?>> getSimulationById(Long id) {
        Simulation simulation = service.getById(id);
        return ResponseEntity.ok(ApiResponse.success(toDto(simulation)));
    }

    public ResponseEntity<ApiResponse<?>> listSimulations(int page, int size) {
        Page<Simulation> pageResult = service.listAll(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                pageResult.map(this::toDto)
        ));
    }

    public ResponseEntity<ApiResponse<?>> deleteSimulation(Long id) {
        service.delete(id);
        return ResponseEntity.ok(
                ApiResponse.success("Simulation " + id + " deleted", null));
    }

    // DTO projection

    private SimulationResponseDto toDto(Simulation s) {
        return SimulationResponseDto.builder()
                .id(s.getId())
                .scenarioName(s.getScenarioName())
                .targetService(s.getTargetService())
                .durationSeconds(s.getDurationSeconds())
                .concurrentUsers(s.getConcurrentUsers())
                .errorRateThreshold(s.getErrorRateThreshold())
                .status(s.getStatus().name())
                .avgLatencyMs(s.getAvgLatencyMs())
                .p95LatencyMs(s.getP95LatencyMs())
                .p99LatencyMs(s.getP99LatencyMs())
                .throughputRps(s.getThroughputRps())
                .actualErrorRate(s.getActualErrorRate())
                .totalRequests(s.getTotalRequests())
                .failedRequests(s.getFailedRequests())
                .failureReason(s.getFailureReason())
                .startedAt(s.getStartedAt())
                .completedAt(s.getCompletedAt())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
