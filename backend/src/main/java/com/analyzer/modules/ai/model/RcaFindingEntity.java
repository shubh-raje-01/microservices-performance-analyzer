package com.analyzer.modules.ai.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Entity
@Table(name = "rca_findings", indexes = {
    @Index(name = "idx_rca_service_id", columnList = "service_id"),
    @Index(name = "idx_rca_analyzed_at", columnList = "analyzed_at DESC"),
    @Index(name = "idx_rca_service_time", columnList = "service_id, analyzed_at DESC"),
    @Index(name = "idx_rca_bottleneck", columnList = "bottleneck_type")
})
public class RcaFindingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_id", nullable = false, length = 36)
    private String serviceId;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    @Column(name = "bottleneck_type", nullable = false, length = 50)
    private String bottleneckType;

    @Column(name = "confidence", nullable = false)
    private Double confidence;

    @Column(name = "reasoning", nullable = false, columnDefinition = "TEXT")
    private String reasoning;

    @Column(name = "evidence_json", columnDefinition = "TEXT")
    private String evidenceJson;

    @Column(name = "recommendations_json", columnDefinition = "TEXT")
    private String recommendationsJson;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "metrics_snapshot_json", columnDefinition = "TEXT")
    private String metricsSnapshotJson;

    @Column(name = "model_version", length = 20)
    private String modelVersion;

    @Column(name = "analyzed_at", nullable = false)
    private Instant analyzedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getBottleneckType() { return bottleneckType; }
    public void setBottleneckType(String bottleneckType) { this.bottleneckType = bottleneckType; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }

    public String getEvidenceJson() { return evidenceJson; }
    public void setEvidenceJson(String evidenceJson) { this.evidenceJson = evidenceJson; }

    public String getRecommendationsJson() { return recommendationsJson; }
    public void setRecommendationsJson(String recommendationsJson) { this.recommendationsJson = recommendationsJson; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getMetricsSnapshotJson() { return metricsSnapshotJson; }
    public void setMetricsSnapshotJson(String metricsSnapshotJson) { this.metricsSnapshotJson = metricsSnapshotJson; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public Instant getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(Instant analyzedAt) { this.analyzedAt = analyzedAt; }

    public Instant getCreatedAt() { return createdAt; }
}
