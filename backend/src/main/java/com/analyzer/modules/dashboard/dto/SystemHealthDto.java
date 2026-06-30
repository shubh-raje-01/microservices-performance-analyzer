package com.analyzer.modules.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SystemHealthDto {

    private final int totalServicesAnalysed;
    private final int healthyCount;
    private final int degradedCount;
    private final int criticalCount;

    private final List<String> criticalServiceNames;
    private final List<String> degradedServiceNames;

    private final double averageHealthScore;
}