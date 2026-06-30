package com.analyzer.modules.recommendation.controller;

import com.analyzer.common.dto.ApiResponse;
import com.analyzer.common.dto.response.RecommendationDto;
import com.analyzer.modules.recommendation.model.RecommendationCategory;
import com.analyzer.modules.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService service;

    // Called by GlobalController.getRecommendations()
    public ResponseEntity<ApiResponse<List<RecommendationDto>>> getRecommendations(
            Long simulationId) {
        return ResponseEntity.ok(
                ApiResponse.success(service.getForSimulation(simulationId)));
    }

    public ResponseEntity<ApiResponse<?>> getHighPriority(Long simulationId) {
        return ResponseEntity.ok(
                ApiResponse.success(service.getHighPriority(simulationId)));
    }

    public ResponseEntity<ApiResponse<?>> getByCategory(Long simulationId,
                                                        String category) {
        try {
            RecommendationCategory cat =
                    RecommendationCategory.valueOf(category.toUpperCase());
            return ResponseEntity.ok(
                    ApiResponse.success(service.getByCategory(simulationId, cat)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_CATEGORY",
                            "Unknown category '" + category + "'. Valid values: "
                                    + java.util.Arrays.toString(RecommendationCategory.values())));
        }
    }

    public ResponseEntity<ApiResponse<?>> getTopN(Long simulationId, int n) {
        return ResponseEntity.ok(
                ApiResponse.success(service.getTopN(simulationId, n)));
    }

    public ResponseEntity<ApiResponse<Map<String, Long>>> getStats(Long simulationId) {
        return ResponseEntity.ok(
                ApiResponse.success(service.getCountsByPriority(simulationId)));
    }
}