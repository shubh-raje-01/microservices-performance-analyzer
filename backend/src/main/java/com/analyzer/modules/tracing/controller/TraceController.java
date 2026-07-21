package com.analyzer.modules.tracing.controller;

import com.analyzer.common.dto.ApiResponse;
import com.analyzer.common.dto.PagedResponse;
import com.analyzer.modules.tracing.dto.TraceDependencyGraphDto;
import com.analyzer.modules.tracing.dto.TraceDetailDto;
import com.analyzer.modules.tracing.dto.TraceSummaryDto;
import com.analyzer.modules.tracing.service.TraceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/traces")
@RequiredArgsConstructor
public class TraceController {

    private final TraceService traceService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> listTraces(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String serviceName,
            @RequestParam(defaultValue = "24") int hoursBack) {

        log.debug("GET /traces — page={}, size={}, service={}, hours={}", page, size, serviceName, hoursBack);
        Page<TraceSummaryDto> result = traceService.getTraces(page, size, serviceName, hoursBack);

        PagedResponse<TraceSummaryDto> paged = PagedResponse.of(result);

        return ResponseEntity.ok(ApiResponse.success(paged));
    }

    @GetMapping("/{traceId}")
    public ResponseEntity<ApiResponse<TraceDetailDto>> getTrace(@PathVariable String traceId) {
        log.debug("GET /traces/{}", traceId);
        TraceDetailDto detail = traceService.getTraceById(traceId);
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    @GetMapping("/{traceId}/graph")
    public ResponseEntity<ApiResponse<TraceDependencyGraphDto>> getDependencyGraph(
            @PathVariable String traceId) {
        log.debug("GET /traces/{}/graph", traceId);
        TraceDependencyGraphDto graph = traceService.getDependencyGraph(traceId);
        return ResponseEntity.ok(ApiResponse.success(graph));
    }
}
