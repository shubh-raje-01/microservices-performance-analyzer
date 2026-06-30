package com.analyzer.modules.recommendation.model;

import com.analyzer.modules.simulation.model.Simulation;

import jakarta.persistence.*;

import lombok.*;

import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "recommendations",
        indexes = {
                @Index(name = "idx_rec_simulation_id",
                        columnList = "simulation_id"),
                @Index(name = "idx_rec_priority",
                        columnList = "priority"),
                @Index(name = "idx_rec_category",
                        columnList = "category"),
                @Index(name = "idx_rec_sim_rank",
                        columnList = "simulation_id, rank"),
                @Index(name = "idx_rec_sim_score",
                        columnList = "simulation_id, composite_score DESC")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "simulation_id", nullable = false)
    private Simulation simulation;

    @Column(name = "simulation_id", insertable = false, updatable = false)
    private Long simulationId;

    /** Set when the recommendation came from an AI insight. */
    @Column(name = "ai_insight_id")
    private Long aiInsightId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecommendationCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RecommendationPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private RecommendationSource source = RecommendationSource.AI_MODEL;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * A concrete, actionable step — not advice, but an instruction.
     * e.g. "Set HikariCP maximumPoolSize to (core_count × 2) + 1"
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String action;

    /** [0.0, 1.0] — model certainty from FastAPI. Null for rule-based entries. */
    private Double confidenceScore;

    /** Expected percentage improvement if the action is applied. */
    private Double estimatedImpact;

    /**
     * Composite score computed by RecommendationScorer.
     * Higher = more important. Used to derive rank.
     */
    @Column(name = "composite_score", nullable = false)
    private Double compositeScore;

    /**
     * 1-based rank within this simulation's recommendation set.
     * 1 = most important.
     */
    @Column(nullable = false)
    private Integer rank;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    // ── Domain helpers

    public boolean isHighPriority() {
        return priority == RecommendationPriority.HIGH;
    }

    public boolean isAiGenerated() {
        return source == RecommendationSource.AI_MODEL;
    }
}