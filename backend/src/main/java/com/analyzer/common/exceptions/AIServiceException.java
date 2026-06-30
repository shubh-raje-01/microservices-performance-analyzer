package com.analyzer.common.exceptions;

import org.springframework.http.HttpStatus;

public class AIServiceException extends AnalyzerException {

    public AIServiceException(String message) {
        super("AI service error: " + message,
                HttpStatus.BAD_GATEWAY,
                "AI_SERVICE_ERROR");
    }

    public AIServiceException(String message, int httpStatus) {
        super("AI service returned HTTP " + httpStatus + ": " + message,
                HttpStatus.BAD_GATEWAY,
                "AI_SERVICE_ERROR");
    }

    public static AIServiceException timeout() {
        return new AIServiceException("Request timed out after configured threshold");
    }

    public static AIServiceException connectionRefused() {
        return new AIServiceException("Connection refused — is the Python service running?");
    }

}
