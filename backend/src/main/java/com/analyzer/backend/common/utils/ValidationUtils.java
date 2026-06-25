package com.analyzer.backend.common.utils;

import com.analyzer.backend.common.exceptions.AnalyzerException;

import java.util.Collection;

public final class ValidationUtils {

    private ValidationUtils() {}

    // ── Null / blank =>

    public static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw AnalyzerException.badRequest(fieldName + " must not be null");
        }
    }

    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw AnalyzerException.badRequest(fieldName + " must not be blank");
        }
    }

    // ── Numeric bounds =>

    public static void requirePositive(Number value, String fieldName) {
        if (value.doubleValue() <= 0) {
            throw AnalyzerException.badRequest(fieldName + " must be positive");
        }
    }

    public static void requireInRange(long value, long min, long max, String fieldName) {
        if (value < min || value > max) {
            throw AnalyzerException.badRequest(
                    fieldName + " must be between " + min + " and " + max + ". Got: " + value);
        }
    }

    public static void requireInRange(double value, double min, double max, String fieldName) {
        if (value < min || value > max) {
            throw AnalyzerException.badRequest(
                    fieldName + " must be between " + min + " and " + max + ". Got: " + value);
        }
    }

    public static void requireNonNegative(double value, String fieldName) {
        if (value < 0) {
            throw AnalyzerException.badRequest(fieldName + " must not be negative");
        }
    }

    // ── Collections =>

    public static void requireNonEmpty(Collection<?> collection, String fieldName) {
        if (collection == null || collection.isEmpty()) {
            throw AnalyzerException.badRequest(fieldName + " must not be empty");
        }
    }

    // ── IDs =>

    public static void requireValidId(Long id, String entityName) {
        if (id == null || id <= 0) {
            throw AnalyzerException.badRequest(
                    entityName + " ID must be a positive number. Got: " + id);
        }
    }

    // ── String format =>

    public static void requireMaxLength(String value, int max, String fieldName) {
        if (value != null && value.length() > max) {
            throw AnalyzerException.badRequest(
                    fieldName + " must not exceed " + max + " characters");
        }
    }

    public static void requirePattern(String value, String regex, String fieldName) {
        if (value != null && !value.matches(regex)) {
            throw AnalyzerException.badRequest(
                    fieldName + " does not match required format");
        }
    }
}