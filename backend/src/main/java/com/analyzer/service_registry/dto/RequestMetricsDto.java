package com.analyzer.service_registry.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestMetricsDto {

    private final Long totalRequests;
    private final Double requestsPerSecond;
    private final Long activeThreadsBusy;
    private final Long activeThreadsCurrent;
    private final Long hikariConnectionsActive;
    private final Long hikariConnectionsIdle;
    private final Long hikariConnectionsPending;
    private final List<MetricTrendPointDto> requestCountTrend;
    private final Instant collectedAt;
}
