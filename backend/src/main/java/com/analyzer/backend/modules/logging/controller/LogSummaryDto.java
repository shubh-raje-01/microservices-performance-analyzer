package com.analyzer.backend.modules.logging.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogSummaryDto {

    private final Long simulationId;
    private final long totalCount;
    private final Map<String, Long> countByLevel;      // "INFO"->5, "WARN"->2
    private final Map<String, Long> countByCategory;   // "PERFORMANCE"->3
    private final long errorCount;
    private final long warnCount;
    private final boolean hasErrors;
    private final boolean thresholdBreached; // true if any PERFORMANCE WARN/ERROR log
}