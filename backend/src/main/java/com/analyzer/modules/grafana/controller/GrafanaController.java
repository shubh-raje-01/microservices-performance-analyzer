package com.analyzer.modules.grafana.controller;

import com.analyzer.modules.grafana.dto.GrafanaWidgetData;
import com.analyzer.modules.grafana.service.GrafanaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Grafana-compatible monitoring controller.
 *
 * Architecture decisions:
 * - Prometheus text format endpoint allows any Prometheus-compatible datasource
 *   (Grafana, VictoriaMetrics, etc.) to scrape without custom plugins.
 * - Widget APIs return pre-assembled JSON payloads so the frontend doesn't
 *   need to make 9 separate API calls.
 * - All repository access delegated to GrafanaService (single source of truth).
 * - Input validation on hours parameter prevents extreme queries.
 * - The /healthz endpoint lets Grafana health-check this monitoring layer.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/grafana")
@RequiredArgsConstructor
public class GrafanaController {

    private final GrafanaService grafanaService;

    /**
     * Prometheus-compatible metrics endpoint.
     * Grafana's Prometheus datasource can point directly at this URL.
     */
    @GetMapping(value = "/prometheus", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> prometheusMetrics(
            @RequestParam(defaultValue = "1") int hours) {
        int clamped = clampHours(hours);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(grafanaService.buildPrometheusMetrics(clamped));
    }

    /**
     * Aggregated widget data for all dashboard panels.
     * The frontend fetches this once on mount and then
     * receives incremental updates via WebSocket.
     */
    @GetMapping("/widgets")
    public ResponseEntity<GrafanaWidgetData> getWidgets(
            @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(grafanaService.buildAllWidgets(clampHours(hours)));
    }

    /**
     * Health check for the monitoring layer itself.
     */
    @GetMapping("/healthz")
    public ResponseEntity<Map<String, Object>> healthz() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", Instant.now()
        ));
    }

    /**
     * Clamps the hours parameter to a sane range [1, 168] (1 hour to 7 days).
     * Prevents unbounded queries from external consumers.
     */
    private int clampHours(int hours) {
        return Math.max(1, Math.min(hours, 168));
    }
}
