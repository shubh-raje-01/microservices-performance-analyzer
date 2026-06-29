package com.analyzer.backend.common.constants;

public final class CacheConstants {

    private CacheConstants() {}

    // ── Simulation module
    public static final String SIMULATION_BY_ID = "simulation-by-id";
    public static final String SIMULATIONS_LIST = "simulations-list";

    // ── Metrics module
    public static final String METRICS_BY_SIM = "metrics-by-sim";
    public static final String METRICS_SUMMARY = "metrics-summary";

    // ── Logging module
    public static final String LOG_SUMMARY = "log-summary";
    public static final String LOGS_BY_SIM = "logs-by-sim";

    // ── AI module
    public static final String AI_INSIGHT = "ai-insight";

    // ── Recommendation module
    public static final String RECOMMENDATIONS = "recommendations";

    // ── Dashboard module
    public static final String DASHBOARD_SUMMARY = "dashboard-summary";

    // ── Shared cross-module
    public static final String SERVICE_NAMES = "service-names";
}