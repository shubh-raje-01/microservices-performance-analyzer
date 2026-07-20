package com.analyzer.modules.dashboard.dto;

import com.analyzer.service_registry.model.ServiceStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ObservabilityDashboardDto {

    private final ServiceStatusSummary statusSummary;
    private final LatencySummary latencySummary;
    private final List<ServiceStatusDto> services;
    private final List<FailureEntry> recentFailures;
    private final List<HealthTimelineEntry> healthTimeline;
    private final List<TopSlowestService> topSlowestServices;
    private final Instant generatedAt;

    @Getter
    @Builder
    public static class ServiceStatusSummary {
        private final int totalServices;
        private final long onlineCount;
        private final long offlineCount;
        private final long degradedCount;
        private final long unknownCount;
        private final long disabledCount;
    }

    @Getter
    @Builder
    public static class LatencySummary {
        private final double averageLatencyMs;
        private final double p50LatencyMs;
        private final double p95LatencyMs;
        private final double p99LatencyMs;
    }

    @Getter
    @Builder
    public static class ServiceStatusDto {
        private final String serviceId;
        private final String serviceName;
        private final ServiceStatus status;
        private final boolean enabled;
        private final Instant lastHeartbeat;
        private final Double avgLatencyMs;
        private final Map<String, Double> latestMetrics;
    }

    @Getter
    @Builder
    public static class FailureEntry {
        private final String serviceId;
        private final String serviceName;
        private final String errorMessage;
        private final Instant occurredAt;
    }

    @Getter
    @Builder
    public static class HealthTimelineEntry {
        private final String serviceId;
        private final String serviceName;
        private final List<TimelinePoint> points;
    }

    @Getter
    @Builder
    public static class TimelinePoint {
        private final Instant time;
        private final ServiceStatus status;
        private final Long latencyMs;
    }

    @Getter
    @Builder
    public static class TopSlowestService {
        private final String serviceId;
        private final String serviceName;
        private final double averageLatencyMs;
        private final long checkCount;
    }
}
