package com.analyzer.modules.ai.controller;

import com.analyzer.common.dto.ApiResponse;
import com.analyzer.common.dto.PagedResponse;
import com.analyzer.modules.ai.model.RcaFindingEntity;
import com.analyzer.modules.ai.service.RcaService;
import com.analyzer.modules.ai.adapter.RcaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RcaController {

    private final RcaService rcaService;

    public ResponseEntity<ApiResponse<?>> analyzeRootCause(
            String serviceId, String serviceName, int hours) {
        RcaResponse response = rcaService.analyze(serviceId, serviceName, hours);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    public ResponseEntity<ApiResponse<?>> getHistory(
            String serviceId, int page, int size) {
        Page<RcaFindingEntity> history = rcaService.getHistory(serviceId, page, size);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(history)));
    }

    public ResponseEntity<ApiResponse<?>> getRecent(
            String serviceId, int hours) {
        List<RcaFindingEntity> recent = rcaService.getRecent(serviceId, hours);
        return ResponseEntity.ok(ApiResponse.success(recent));
    }

    public ResponseEntity<ApiResponse<?>> getStats(
            String serviceId, int hours) {
        List<Object[]> stats = rcaService.getBottleneckStats(serviceId, hours);
        Map<String, Long> statsMap = stats.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));
        return ResponseEntity.ok(ApiResponse.success(statsMap));
    }
}
