package com.analyzer.backend.modules.metrics.model;

import com.analyzer.backend.modules.simulation.model.Simulation;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "metric_snapshots",
        indexes = {
                @Index(name = "idx_metric_simulation_id",
                        columnList = "simulation_id"),
                @Index(name = "idx_metric_type",
                        columnList = "metric_type"),
                @Index(name = "idx_metric_service_recorded",
                        columnList = "service_name, recorded_at DESC"),
                @Index(name = "idx_metric_severity",
                        columnList = "severity")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricSnapshot {

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
    @Column(name = "metric_type", nullable = false, length = 30)
    private MetricType metricType;

    @Column(nullable = false)
    private Double value;

    @Column(length = 20)
    private String unit;          // "ms", "rps", "percent", "count"

    // For latency snapshots — all percentiles stored on the same row
    private Double avgMs;
    private Double p50Ms;
    private Double p95Ms;
    private Double p99Ms;
    private Double maxMs;

    // For error rate and availability snapshots
    private Long totalRequests;
    private Long failedRequests;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MetricSeverity severity = MetricSeverity.NORMAL;

    @Column(length = 500)
    private String notes;         // human-readable context, e.g. "p95 exceeded 500ms threshold"

    @Column(nullable = false)
    private Instant recordedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    // ── Domain helpers =>

    public boolean isLatency() {
        return metricType == MetricType.LATENCY;
    }

    public boolean isCritical() {
        return severity == MetricSeverity.CRITICAL;
    }

    public boolean exceedsThreshold(double threshold) {
        return value != null && value > threshold;
    }
}
