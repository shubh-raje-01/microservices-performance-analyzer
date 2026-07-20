package com.analyzer.service_registry.controller;

import com.analyzer.common.dto.ApiResponse;
import com.analyzer.service_registry.dto.BenchmarkRequestDto;
import com.analyzer.service_registry.dto.BenchmarkResultDto;
import com.analyzer.service_registry.service.BenchmarkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    public ResponseEntity<ApiResponse<?>> runBenchmark(
            String serviceId, @Valid @RequestBody BenchmarkRequestDto request) {
        log.info("POST /services/{}/benchmark — endpoint: {}", serviceId, request.getEndpoint());
        BenchmarkResultDto result = benchmarkService.runBenchmark(serviceId, request);
        return ResponseEntity.ok(ApiResponse.success("Benchmark completed", result));
    }
}
