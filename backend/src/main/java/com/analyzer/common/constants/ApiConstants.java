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

    // Python FastAPI routes
    public static final String AI_ANALYZE = "/api/analyze";
    public static final String AI_HEALTH = "/health";
    public static final String AI_PREDICT = "/api/predict";

    // Default pagination
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
}