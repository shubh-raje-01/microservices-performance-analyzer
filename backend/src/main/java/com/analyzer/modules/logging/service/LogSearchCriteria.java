package com.analyzer.modules.logging.service;

import com.analyzer.modules.logging.model.LogCategory;
import com.analyzer.modules.logging.model.LogLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class LogSearchCriteria {

    private final Long simulationId;
    private final LogLevel level;           // exact level filter
    private final LogLevel minimumLevel;    // all levels at and above this
    private final LogCategory category;
    private final String serviceName;
    private final String source;
    private final String keyword;         // substring match on message
    private final Instant from;
    private final Instant to;

    @Builder.Default
    private final int page = 0;

    @Builder.Default
    private final int size = 50;

    /** Convenience factory — fetch all WARN and above for a simulation. */
    public static LogSearchCriteria problematic(Long simulationId) {
        return LogSearchCriteria.builder()
                .simulationId(simulationId)
                .minimumLevel(LogLevel.WARN)
                .build();
    }

    /** Convenience factory — all logs for a simulation in chronological order. */
    public static LogSearchCriteria forSimulation(Long simulationId) {
        return LogSearchCriteria.builder()
                .simulationId(simulationId)
                .build();
    }

    /** Convenience factory — all ERROR and FATAL entries for a simulation. */
    public static LogSearchCriteria errorsOnly(Long simulationId) {
        return LogSearchCriteria.builder()
                .simulationId(simulationId)
                .minimumLevel(LogLevel.ERROR)
                .build();
    }
}