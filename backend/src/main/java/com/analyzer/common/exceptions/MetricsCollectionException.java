package com.analyzer.common.exceptions;

import org.springframework.http.HttpStatus;

public class MetricsCollectionException extends AnalyzerException {

    public MetricsCollectionException(Long simulationId, String reason) {
        super("Metrics collection failed for simulation " + simulationId + ": " + reason,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "METRICS_COLLECTION_FAILED");
    }

    public MetricsCollectionException(String reason) {
        super("Metrics collection failed: " + reason,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "METRICS_COLLECTION_FAILED");
    }

}
