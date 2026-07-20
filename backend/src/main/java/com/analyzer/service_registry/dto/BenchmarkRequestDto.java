package com.analyzer.service_registry.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class BenchmarkRequestDto {

    @NotBlank(message = "Endpoint cannot be blank")
    private String endpoint;

    private String method = "GET";

    @NotNull(message = "Request count is required")
    @Min(value = 1, message = "Request count must be at least 1")
    private Integer requestCount = 10;

    private Integer concurrency = 1;
    private Integer timeoutSeconds = 10;
    private Map<String, String> headers;
    private String body;
}
