package com.analyzer.backend.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

@Slf4j
public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private JsonUtils() {}

    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            log.error("JSON serialisation failed for {}: {}", value.getClass().getSimpleName(), ex.getMessage());
            throw new IllegalStateException("Failed to serialise to JSON", ex);
        }
    }

    public static String toPrettyJson(Object value) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialise to JSON", ex);
        }
    }

    public static <T> Optional<T> fromJson(String json, Class<T> type) {
        try {
            return Optional.ofNullable(MAPPER.readValue(json, type));
        } catch (JsonProcessingException ex) {
            log.warn("JSON deserialisation failed for type {}: {}", type.getSimpleName(), ex.getMessage());
            return Optional.empty();
        }
    }

    public static <T> Optional<T> fromJson(String json, TypeReference<T> ref) {
        try {
            return Optional.ofNullable(MAPPER.readValue(json, ref));
        } catch (JsonProcessingException ex) {
            log.warn("JSON deserialisation failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public static Map<String, Object> toMap(Object value) {
        return MAPPER.convertValue(value, new TypeReference<>() {});
    }

    public static <T> T convert(Object source, Class<T> targetType) {
        return MAPPER.convertValue(source, targetType);
    }

    public static boolean isValidJson(String json) {
        try {
            MAPPER.readTree(json);
            return true;
        } catch (JsonProcessingException ex) {
            return false;
        }
    }
}
