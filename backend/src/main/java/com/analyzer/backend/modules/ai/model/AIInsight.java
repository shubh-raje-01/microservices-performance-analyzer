package com.analyzer.backend.modules.ai.model;

import com.analyzer.backend.infrastructure.database.JsonListConverter;
import com.analyzer.backend.infrastructure.database.JsonMapConverter;
import com.analyzer.backend.modules.simulation.model.Simulation;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Entity
@Table(
        name = "ai_insights",
        indexes = {
                @Index(name = "idx_ai_simulation_id",
                        columnList = "simulation_id"),
                @Index(name = "idx_ai_status",
                        columnList = "status"),
                @Index(name = "idx_ai_anomaly_type",
                        columnList = "anomaly_type"),
                @Index(name = "idx_ai_service_analyzed",
                        columnList = "service_name, analyzed_at DESC")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "simulation_id", nullable = false)
    private Simulation simulation;

    @Column(name = "simulation_id", insertable = false, updatable = false)
    private Long simulationId;

    @Column(nullable = false, length = 100)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private AIAnalysisStatus status = AIAnalysisStatus.PENDING;

    // ── FastAPI response fields

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "anomaly_type", length = 30)
    private AnomalyType anomalyType;

    private Double anomalyScore;         // [0.0, 1.0] — higher = more anomalous

    @Column(length = 20)
    private String predictedTrend;       // "IMPROVING" | "STABLE" | "DEGRADING"

    private Double predictedP95Ms;       // forecasted p95 for the next period

    @Convert(converter = JsonListConverter.class)
    @Column(name = "detected_patterns", columnDefinition = "TEXT")
    private List<String> detectedPatterns;

    @Convert(converter = JsonMapConverter.class)
    @Column(name = "feature_importance", columnDefinition = "TEXT")
    private Map<String, Object> featureImportance;

    /**
     * Raw recommendation JSON from FastAPI — stored here so the
     * recommendation module can parse it without re-calling Python.
     */
    @Column(name = "raw_recommendations", columnDefinition = "TEXT")
    private String rawRecommendations;

    /**
     * Full FastAPI response JSON — kept for debugging and model versioning.
     */
    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    private Instant analyzedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    // ── Domain helpers

    public boolean isCompleted() {
        return status == AIAnalysisStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == AIAnalysisStatus.FAILED;
    }

    public boolean isDegraded() {
        return status == AIAnalysisStatus.DEGRADED;
    }

    public boolean hasAnomaly() {
        return anomalyType != null
                && anomalyType != AnomalyType.NONE
                && anomalyType != AnomalyType.UNKNOWN;
    }
}