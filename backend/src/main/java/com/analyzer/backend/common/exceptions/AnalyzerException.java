package com.analyzer.backend.common.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AnalyzerException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public AnalyzerException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status    = status;
        this.errorCode = errorCode;
    }

    public AnalyzerException(String message, HttpStatus status) {
        this(message, status, status.name());
    }

    // Static factories — cover the common cases

    public static AnalyzerException notFound(String entity, Long id) {
        return new AnalyzerException(
                entity + " not found with id: " + id,
                HttpStatus.NOT_FOUND,
                "NOT_FOUND"
        );
    }

    public static AnalyzerException notFound(String entity, String identifier) {
        return new AnalyzerException(
                entity + " not found: " + identifier,
                HttpStatus.NOT_FOUND,
                "NOT_FOUND"
        );
    }

    public static AnalyzerException badRequest(String message) {
        return new AnalyzerException(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }

    public static AnalyzerException conflict(String message) {
        return new AnalyzerException(message, HttpStatus.CONFLICT, "CONFLICT");
    }

    public static AnalyzerException serviceUnavailable(String service) {
        return new AnalyzerException(
                service + " is currently unavailable — please try again shortly",
                HttpStatus.SERVICE_UNAVAILABLE,
                "SERVICE_UNAVAILABLE"
        );
    }

    public static AnalyzerException timeout(String service) {
        return new AnalyzerException(
                "Request to " + service + " timed out",
                HttpStatus.GATEWAY_TIMEOUT,
                "TIMEOUT"
        );
    }

    public static AnalyzerException internalError(String message) {
        return new AnalyzerException(
                message,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR"
        );
    }
}
