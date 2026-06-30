package com.analyzer.modules.logging.model;

import java.util.Arrays;
import java.util.List;

public enum LogLevel {
    TRACE(0),
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4),
    FATAL(5);

    private final int severity;

    LogLevel(int severity) {
        this.severity = severity;
    }

    public int getSeverity() {
        return severity;
    }

    public boolean isAtLeast(LogLevel minimum) {
        return this.severity >= minimum.severity;
    }

    /** Returns all levels at or above the given minimum — used for threshold queries. */
    public static List<LogLevel> atAndAbove(LogLevel minimum) {
        return Arrays.stream(values())
                .filter(l -> l.isAtLeast(minimum))
                .toList();
    }
}