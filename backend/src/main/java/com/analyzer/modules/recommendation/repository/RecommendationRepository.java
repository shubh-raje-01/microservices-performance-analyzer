package com.analyzer.modules.recommendation.repository;

import com.analyzer.modules.recommendation.model.Recommendation;
import com.analyzer.modules.recommendation.model.RecommendationCategory;
import com.analyzer.modules.recommendation.model.RecommendationPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findBySimulationIdOrderByRankAsc(Long simulationId);

    List<Recommendation> findBySimulationIdAndPriorityOrderByRankAsc(
            Long simulationId, RecommendationPriority priority);

    List<Recommendation> findBySimulationIdAndCategoryOrderByRankAsc(
            Long simulationId, RecommendationCategory category);

    @Query("""
        SELECT r FROM Recommendation r
        WHERE r.simulationId = :simId
          AND r.priority     = 'HIGH'
        ORDER BY r.compositeScore DESC
    """)
    List<Recommendation> findHighPriorityBySimulation(@Param("simId") Long simulationId);

    @Query("""
        SELECT r FROM Recommendation r
        WHERE r.simulationId = :simId
        ORDER BY r.compositeScore DESC
    """)
    List<Recommendation> findByScoreDesc(@Param("simId") Long simulationId);

    long countBySimulationId(Long simulationId);

    long countBySimulationIdAndPriority(Long simulationId, RecommendationPriority priority);

    boolean existsBySimulationId(Long simulationId);

    void deleteBySimulationId(Long simulationId);
}