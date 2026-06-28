package com.analyzer.backend.modules.logging.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogEntryDto {

    private final Long id;
    private final Long simulationId;
    private final String level;
    private final String category;
    private final String serviceName;
    private final String source;
    private final String message;
    private final Map<String, Object> context;
    private final String stackTrace;
    private final Long durationMs;
    private final Instant occurredAt;
}