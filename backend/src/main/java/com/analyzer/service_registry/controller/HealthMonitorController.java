package com.analyzer.service_registry.controller;

import com.analyzer.common.dto.ApiResponse;
import com.analyzer.service_registry.dto.HealthCheckResponseDto;
import com.analyzer.service_registry.model.ServiceHealthHistory;
import com.analyzer.service_registry.model.ServiceStatus;
import com.analyzer.service_registry.service.ExternalMetricsCollectorService;
import com.analyzer.service_registry.service.HealthMonitorService;
import com.analyzer.service_registry.service.ServiceRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HealthMonitorController {

    private final HealthMonitorService healthMonitorService;
    private final ExternalMetricsCollectorService metricsCollectorService;
    private final ServiceRegistryService registryService;

    public ResponseEntity<ApiResponse<?>> getHealthHistory(
            String serviceId, @RequestParam(defaultValue = "24") int hours) {
        List<ServiceHealthHistory> history = healthMonitorService.getRecentHealthHistory(serviceId, hours);
        List<HealthCheckResponseDto> dtos = history.stream()
                .map(this::toHealthDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    public ResponseEntity<ApiResponse<?>> getAllHealthHistory(
            @RequestParam(defaultValue = "1") int hours) {
        List<ServiceHealthHistory> history = healthMonitorService.getRecentHealthHistoryAll(hours);
        List<HealthCheckResponseDto> dtos = history.stream()
                .map(this::toHealthDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    public ResponseEntity<ApiResponse<?>> triggerHealthCheck(String serviceId) {
        var service = registryService.getById(serviceId);
        healthMonitorService.checkServiceHealth(service);
        return ResponseEntity.ok(ApiResponse.success("Health check triggered for " + service.getName(), null));
    }

    public ResponseEntity<ApiResponse<?>> getCurrentStatus() {
        List<com.analyzer.service_registry.dto.ServiceResponseDto> services = registryService.listEnabled();
        Map<String, Object> statusMap = new LinkedHashMap<>();
        statusMap.put("totalServices", services.size());
        statusMap.put("online", services.stream().filter(s -> s.getStatus() == ServiceStatus.ONLINE).count());
        statusMap.put("offline", services.stream().filter(s -> s.getStatus() == ServiceStatus.OFFLINE).count());
        statusMap.put("degraded", services.stream().filter(s -> s.getStatus() == ServiceStatus.DEGRADED).count());
        statusMap.put("unknown", services.stream().filter(s -> s.getStatus() == ServiceStatus.UNKNOWN).count());
        statusMap.put("services", services.stream().collect(Collectors.toMap(
                com.analyzer.service_registry.dto.ServiceResponseDto::getName,
                s -> Map.of(
                        "status", s.getStatus().name(),
                        "lastHeartbeat", s.getLastHeartbeat() != null ? s.getLastHeartbeat().toString() : "Never",
                        "enabled", s.isEnabled()
                )
        )));
        return ResponseEntity.ok(ApiResponse.success(statusMap));
    }

    public ResponseEntity<ApiResponse<?>> getMetricTrend(
            String serviceId, String metricName,
            @RequestParam(defaultValue = "24") int hours) {
        var metrics = metricsCollectorService.getMetricTrend(serviceId, metricName, hours);
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    public ResponseEntity<ApiResponse<?>> getAverageLatency(
            String serviceId, @RequestParam(defaultValue = "1") int hours) {
        Double avg = healthMonitorService.getAverageLatency(serviceId, hours);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "serviceId", serviceId,
                "averageLatencyMs", avg != null ? avg : 0.0,
                "hours", hours
        )));
    }

    public ResponseEntity<ApiResponse<?>> getTopSlowest(
            @RequestParam(defaultValue = "1") int hours) {
        List<Map.Entry<String, Double>> slowest = healthMonitorService.getRecentHealthHistoryAll(hours).stream()
                .collect(Collectors.groupingBy(
                        h -> h.getService().getId(),
                        Collectors.averagingLong(h -> h.getLatencyMs() != null ? h.getLatencyMs() : 0L)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(slowest));
    }

    private HealthCheckResponseDto toHealthDto(ServiceHealthHistory h) {
        return HealthCheckResponseDto.builder()
                .id(h.getId())
                .serviceId(h.getService().getId())
                .serviceName(h.getService().getName())
                .status(h.getStatus())
                .latencyMs(h.getLatencyMs())
                .responseCode(h.getResponseCode())
                .errorMessage(h.getErrorMessage())
                .checkTime(h.getCheckTime())
                .build();
    }
}
