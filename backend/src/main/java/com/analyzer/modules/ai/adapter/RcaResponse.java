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
public class RcaResponse {

    private String serviceId;
    private String serviceName;
    private List<RcaFinding> findings;
    private RcaFinding primaryFinding;
    private String summary;
    private String modelVersion;
    private String analyzedAt;
    private Map<String, Object> metricsSnapshot;

    @Getter
    @NoArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class RcaFinding {
        private String bottleneckType;
        private double confidence;
        private String reasoning;
        private List<EvidenceItem> evidence;
        private List<RcaRecommendation> recommendations;
    }

    @Getter
    @NoArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class EvidenceItem {
        private String metric;
        private Object value;
        private String unit;
        private String status;
        private String description;
    }

    @Getter
    @NoArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class RcaRecommendation {
        private String title;
        private String description;
        private String priority;
        private String category;
        private double confidence;
    }
}
