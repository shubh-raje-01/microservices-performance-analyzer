package com.analyzer.modules.ai.repository;

import com.analyzer.modules.ai.model.AIAnalysisStatus;
import com.analyzer.modules.ai.model.AIInsight;
import com.analyzer.modules.ai.model.AnomalyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AIInsightRepository extends JpaRepository<AIInsight, Long> {

    Optional<AIInsight> findBySimulationId(Long simulationId);

    Optional<AIInsight> findTopBySimulationIdOrderByCreatedAtDesc(Long simulationId);

    boolean existsBySimulationIdAndStatus(Long simulationId, AIAnalysisStatus status);

    boolean existsBySimulationId(Long simulationId);

    List<AIInsight> findByServiceNameOrderByAnalyzedAtDesc(String serviceName);

    List<AIInsight> findByStatus(AIAnalysisStatus status);

    List<AIInsight> findByAnomalyTypeOrderByAnalyzedAtDesc(AnomalyType anomalyType);

    @Query("""
        SELECT a FROM AIInsight a
        WHERE a.status      = 'COMPLETED'
          AND a.anomalyType != 'NONE'
          AND a.analyzedAt >= :since
        ORDER BY a.anomalyScore DESC
    """)
    List<AIInsight> findRecentAnomalies(@Param("since") Instant since);

    @Query("""
        SELECT COUNT(a) FROM AIInsight a
        WHERE a.simulationId = :simId
          AND a.status       = 'COMPLETED'
    """)
    long countCompletedBySimulation(@Param("simId") Long simulationId);

    void deleteBySimulationId(Long simulationId);
}