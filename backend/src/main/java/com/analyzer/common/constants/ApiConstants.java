package com.analyzer.common.constants;

public final class ApiConstants {

    private ApiConstants() {}

    public static final String API_VERSION = "/api/v1";

    public static final String SIMULATE = API_VERSION + "/simulate";
    public static final String METRICS = API_VERSION + "/metrics";
    public static final String ANALYZE = API_VERSION + "/analyze";
    public static final String RECOMMEND = API_VERSION + "/recommendations";
    public static final String DASHBOARD = API_VERSION + "/dashboard";
    public static final String HEALTH = API_VERSION + "/health";

    // Service Registry
    public static final String SERVICES = API_VERSION + "/services";
    public static final String SERVICES_BY_ID = SERVICES + "/{id}";
    public static final String SERVICES_SEARCH = SERVICES + "/search";
    public static final String SERVICES_ENABLE = SERVICES + "/{id}/enable";
    public static final String SERVICES_DISABLE = SERVICES + "/{id}/disable";
    public static final String SERVICES_BENCHMARK = SERVICES + "/{id}/benchmark";

    // Health Monitoring
    public static final String HEALTH_STATUS = API_VERSION + "/health/status";
    public static final String HEALTH_HISTORY = API_VERSION + "/health/history";
    public static final String HEALTH_HISTORY_SERVICE = API_VERSION + "/health/history/{serviceId}";
    public static final String HEALTH_CHECK_TRIGGER = API_VERSION + "/health/check/{serviceId}";
    public static final String HEALTH_LATENCY = API_VERSION + "/health/latency";
    public static final String HEALTH_SLOWEST = API_VERSION + "/health/slowest";

    // Metrics (External Services)
    public static final String EXTERNAL_METRICS = API_VERSION + "/monitoring/metrics";
    public static final String EXTERNAL_METRICS_TREND = EXTERNAL_METRICS + "/{serviceId}/trend";

    // Python FastAPI routes
    public static final String AI_ANALYZE = "/api/analyze";
    public static final String AI_HEALTH = "/health";
    public static final String AI_PREDICT = "/api/predict";

    // Default pagination
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
}