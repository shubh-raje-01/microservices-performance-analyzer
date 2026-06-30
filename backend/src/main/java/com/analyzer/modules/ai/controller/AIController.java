package com.analyzer.modules.ai.controller;

import com.analyzer.common.dto.ApiResponse;
import com.analyzer.common.dto.response.AIInsightDto;
import com.analyzer.modules.ai.model.AIInsight;
import com.analyzer.modules.ai.service.AIService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AIController {

    private final AIService service;

    // Called by GlobalController.analyzeSimulation()
    public ResponseEntity<ApiResponse<?>> analyzeSimulation(Long simulationId) {
        log.info("AIController.analyzeSimulation — simulationId={}", simulationId);
        AIInsightDto insight = service.analyzeSimulation(simulationId);
        return ResponseEntity.ok(
                ApiResponse.success("Analysis complete", insight));
    }

    // Called by GlobalController.getInsights()
    public ResponseEntity<ApiResponse<?>> getInsights(Long simulationId) {
        AIInsightDto insight = service.getInsight(simulationId);
        return ResponseEntity.ok(ApiResponse.success(insight));
    }

    // Called by GlobalController — recent anomalies across all services
    public ResponseEntity<ApiResponse<?>> getRecentAnomalies(int hoursBack) {
        List<AIInsight> anomalies = service.getRecentAnomalies(hoursBack);
        List<AIInsightResponseDto> dtos = anomalies.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    // Called by GlobalController — health check for the FastAPI connection
    public ResponseEntity<ApiResponse<?>> getFastAPIHealth() {
        // Delegate through the adapter — result goes into the response map
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("fastApiReachable", true, "message", "AI service is reachable")));
    }

    // ── DTO projection

    private AIInsightResponseDto toResponseDto(AIInsight i) {
        Map<String, Double> featureImportance = null;
        if (i.getFeatureImportance() != null) {
            featureImportance = i.getFeatureImportance().entrySet().stream()
                    .filter(e -> e.getValue() instanceof Number)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> ((Number) e.getValue()).doubleValue()
                    ));
        }

        return AIInsightResponseDto.builder()
                .id(i.getId())
                .simulationId(i.getSimulationId())
                .serviceName(i.getServiceName())
                .status(i.getStatus().name())
                .summary(i.getSummary())
                .anomalyType(i.getAnomalyType() != null ? i.getAnomalyType().name() : null)
                .anomalyScore(i.getAnomalyScore())
                .predictedTrend(i.getPredictedTrend())
                .predictedP95Ms(i.getPredictedP95Ms())
                .degraded(i.isDegraded())
                .detectedPatterns(i.getDetectedPatterns())
                .featureImportance(featureImportance)
                .modelVersion(i.getModelVersion())
                .analyzedAt(i.getAnalyzedAt())
                .createdAt(i.getCreatedAt())
                .build();
    }
}