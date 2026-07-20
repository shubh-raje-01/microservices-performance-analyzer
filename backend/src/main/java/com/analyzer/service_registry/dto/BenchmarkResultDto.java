package com.analyzer.service_registry.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BenchmarkResultDto {

    private final String serviceId;
    private final String serviceName;
    private final String endpoint;
    private final String method;
    private final int requestCount;
    private final int successCount;
    private final int failureCount;
    private final double avgLatencyMs;
    private final double minLatencyMs;
    private final double maxLatencyMs;
    private final double p50LatencyMs;
    private final double p95LatencyMs;
    private final double p99LatencyMs;
    private final double stdDeviation;
    private final double requestsPerSecond;
    private final double errorRate;
    private final List<RequestLatencyDto> individualResults;
    private final Instant executedAt;

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RequestLatencyDto {
        private final int sequence;
        private final long latencyMs;
        private final int statusCode;
        private final boolean success;
        private final String errorMessage;
    }
}
