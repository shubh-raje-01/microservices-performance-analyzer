package com.analyzer.modules.logging.processor;

import com.analyzer.modules.logging.model.LogEntry;
import com.analyzer.modules.logging.model.LogLevel;
import com.analyzer.modules.logging.model.LogCategory;
import com.analyzer.modules.logging.service.LogSearchCriteria;
import org.springframework.data.jpa.domain.Specification;

public final class LogSpecification {

    private LogSpecification() {}

    public static Specification<LogEntry> build(LogSearchCriteria c) {
        return Specification
                .where(simulationIdEquals(c.getSimulationId()))
                .and(levelEquals(c.getLevel()))
                .and(levelAtLeast(c.getMinimumLevel()))
                .and(categoryEquals(c.getCategory()))
                .and(serviceNameEquals(c.getServiceName()))
                .and(sourceEquals(c.getSource()))
                .and(messageContains(c.getKeyword()))
                .and(occurredAfter(c.getFrom()))
                .and(occurredBefore(c.getTo()));
    }

    // ── Predicates =>

    private static Specification<LogEntry> simulationIdEquals(Long id) {
        return (root, query, cb) ->
                id == null ? null : cb.equal(root.get("simulationId"), id);
    }

    private static Specification<LogEntry> levelEquals(LogLevel level) {
        return (root, query, cb) ->
                level == null ? null : cb.equal(root.get("level"), level);
    }

    /**
     * Filters to all entries whose level ordinal is >= the minimum.
     * Mutually exclusive with levelEquals — criteria should set only one.
     */
    private static Specification<LogEntry> levelAtLeast(LogLevel minimum) {
        if (minimum == null) return (root, query, cb) -> null;
        java.util.List<LogLevel> eligible = LogLevel.atAndAbove(minimum);
        return (root, query, cb) -> root.get("level").in(eligible);
    }

    private static Specification<LogEntry> categoryEquals(
            LogCategory category) {
        return (root, query, cb) ->
                category == null ? null : cb.equal(root.get("category"), category);
    }

    private static Specification<LogEntry> serviceNameEquals(String name) {
        return (root, query, cb) ->
                (name == null || name.isBlank()) ? null
                        : cb.equal(root.get("serviceName"), name);
    }

    private static Specification<LogEntry> sourceEquals(String source) {
        return (root, query, cb) ->
                (source == null || source.isBlank()) ? null
                        : cb.equal(root.get("source"), source);
    }

    private static Specification<LogEntry> messageContains(String keyword) {
        return (root, query, cb) ->
                (keyword == null || keyword.isBlank()) ? null
                        : cb.like(cb.lower(root.get("message")),
                        "%" + keyword.toLowerCase() + "%");
    }

    private static Specification<LogEntry> occurredAfter(java.time.Instant from) {
        return (root, query, cb) ->
                from == null ? null
                        : cb.greaterThanOrEqualTo(root.get("occurredAt"), from);
    }

    private static Specification<LogEntry> occurredBefore(java.time.Instant to) {
        return (root, query, cb) ->
                to == null ? null
                        : cb.lessThanOrEqualTo(root.get("occurredAt"), to);
    }
}