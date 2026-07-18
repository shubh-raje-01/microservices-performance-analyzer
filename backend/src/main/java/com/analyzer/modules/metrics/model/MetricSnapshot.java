package com.analyzer.modules.metrics.model;

import com.analyzer.modules.simulation.model.Simulation;
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
    @Column(name = "avg_ms")
    private Double avgMs;

    @Column(name = "p50_ms")
    private Double p50Ms;

    @Column(name = "p95_ms")
    private Double p95Ms;

    @Column(name = "p99_ms")
    private Double p99Ms;

    @Column(name = "max_ms")
    private Double maxMs;

    // For error rate and availability snapshots
    @Column(name = "total_requests")
    private Long totalRequests;
    @Column(name = "failed_requests")
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
