package com.analyzer.infrastructure.database;

import com.fasterxml.jackson.core.type.TypeReference;
import com.analyzer.common.utils.JsonUtils;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Converter
public class JsonListConverter
        implements AttributeConverter<List<String>, String> {

    private static final TypeReference<List<String>> TYPE_REF =
            new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) return null;
        return JsonUtils.toJson(attribute);
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return List.of();
        return JsonUtils.fromJson(dbData, TYPE_REF).orElseGet(() -> {
            log.warn("JsonListConverter: failed to deserialise column — returning empty list");
            return List.of();
        });
    }
}