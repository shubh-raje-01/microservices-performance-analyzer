package com.analyzer.infrastructure.database;

import com.analyzer.common.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@Converter
public class JsonMapConverter
        implements AttributeConverter<Map<String, Object>, String> {

    private static final TypeReference<Map<String, Object>> TYPE_REF =
            new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null || attribute.isEmpty()) return null;
        return JsonUtils.toJson(attribute);
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return Map.of();
        return JsonUtils.fromJson(dbData, TYPE_REF).orElseGet(() -> {
            log.warn("JsonMapConverter: failed to deserialise column value — returning empty map");
            return Map.of();
        });
    }
}