package com.analyzer.modules.dashboard.controller;

import com.analyzer.common.dto.ApiResponse;
import com.analyzer.common.dto.response.DashboardSummaryDto;
import com.analyzer.modules.dashboard.dto.DashboardOverviewDto;
import com.analyzer.modules.dashboard.dto.SystemHealthDto;
import com.analyzer.modules.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    // Called by GlobalController.getDashboard()
    public ResponseEntity<ApiResponse<DashboardSummaryDto>> getSummary(Long simulationId) {
        DashboardSummaryDto summary = service.getSummary(simulationId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    // Called by GlobalController.getRecentDashboard()
    public ResponseEntity<ApiResponse<?>> getRecentSummaries(int limit) {
        List<DashboardOverviewDto> overviews = service.getRecentOverviews(limit);
        return ResponseEntity.ok(ApiResponse.success(overviews));
    }

    public ResponseEntity<ApiResponse<?>> getSystemHealth() {
        SystemHealthDto health = service.getSystemHealth();
        return ResponseEntity.ok(ApiResponse.success(health));
    }
}