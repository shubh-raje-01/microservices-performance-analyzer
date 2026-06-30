package com.analyzer.modules.ai.model;

public enum AIAnalysisStatus {
    PENDING,     // analysis triggered, waiting for FastAPI
    PROCESSING,  // FastAPI call in-flight
    COMPLETED,   // insight persisted successfully
    FAILED,      // FastAPI call or persistence failed
    DEGRADED     // circuit open — returned fallback insight
}