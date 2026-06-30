package com.analyzer.modules.ai.adapter;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FastAPIResponse {

    private Long simulationId;
    private String summary;
    private String anomalyType;       // maps to AnomalyType enum by name
    private Double anomalyScore;
    private String predictedTrend;    // "IMPROVING" | "STABLE" | "DEGRADING"
    private Double predictedP95Ms;
    private String modelVersion;

    private List<String>                   detectedPatterns;
    private Map<String, Double>            featureImportance;
    private List<FastAPIRecommendation>    recommendations;

    /** True when Python returned a graceful degraded response (model unavailable). */
    private boolean degraded;
    private String degradedReason;
}