package com.analyzer.modules.logging.model;

public enum LogCategory {
    SIMULATION,      // scenario lifecycle — started, running, completed, failed
    PERFORMANCE,     // latency / throughput / error-rate analysis results
    METRICS,         // metric collection and aggregation events
    AI_ANALYSIS,     // FastAPI calls, model responses, anomaly detection
    RECOMMENDATION,  // scoring, ranking, and recommendation generation
    SYSTEM,          // app startup, health checks, infrastructure connectivity
    AUDIT            // user-initiated actions received by the API layer
}