package com.analyzer.common.constants;

public final class EventConstants {

    private EventConstants() {}

    public static final String SIMULATION_STARTED = "simulation.started";
    public static final String SIMULATION_COMPLETED = "simulation.completed";
    public static final String SIMULATION_FAILED = "simulation.failed";

    public static final String METRICS_COLLECTED = "metrics.collected";
    public static final String AI_ANALYSIS_DONE = "ai.analysis.done";
    public static final String RECOMMENDATIONS_READY = "recommendations.ready";

    // Service Registry events
    public static final String SERVICE_REGISTERED = "service.registered";
    public static final String SERVICE_HEALTH_CHECKED = "service.health.checked";
    public static final String SERVICE_STATUS_CHANGED = "service.status.changed";
    public static final String SERVICE_METRICS_COLLECTED = "service.metrics.collected";
    public static final String SERVICE_BENCHMARK_COMPLETED = "service.benchmark.completed";
}