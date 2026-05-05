package com.analyzer.backend.modules.simulation.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "simulations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Simulation {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String scenarioName;

    @Column(nullable = false, length = 100)
    private String targetService;

    @Column(nullable = false)
    private int durationSeconds;

    @Column(nullable = false)
    private int concurrentUsers;

    @Column(nullable = false)
    private double errorRateThreshold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SimulationStatus status = SimulationStatus.PENDING;

    private Double avgLatencyMs;
    private Double p95LatencyMs;
    private Double p99LatencyMs;
    private Double throughputRps;
    private Double actualErrorRate;
    private Long totalRequests;
    private Long failedRequests;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    private Instant startedAt;
    private Instant completedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "simulation_params",
            joinColumns = @JoinColumn(name = "simulation_id"))
    @MapKeyColumn(name = "param_key")
    @Column(name = "param_value")
    private Map<String, String> customParams;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public boolean isTerminal() {
        return status == SimulationStatus.COMPLETED
                || status == SimulationStatus.FAILED
                || status == SimulationStatus.CANCELLED;
    }

    public void markRunning() {
        this.status    = SimulationStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    public void markCompleted(SimulationResult result) {
        this.status = SimulationStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.avgLatencyMs = result.getAvgLatencyMs();
        this.p95LatencyMs = result.getP95LatencyMs();
        this.p99LatencyMs = result.getP99LatencyMs();
        this.throughputRps = result.getThroughputRps();
        this.actualErrorRate = result.getActualErrorRate();
        this.totalRequests = result.getTotalRequests();
        this.failedRequests = result.getFailedRequests();
    }

    public void markFailed(String reason) {
        this.status = SimulationStatus.FAILED;
        this.completedAt = Instant.now();
        this.failureReason = reason;
    }

}
