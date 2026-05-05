package com.analyzer.backend.modules.simulation.repository;

import com.analyzer.backend.modules.simulation.model.Simulation;
import com.analyzer.backend.modules.simulation.model.SimulationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SimulationRepository extends JpaRepository<Simulation, Long> {

    Page<Simulation> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Simulation> findByStatus(SimulationStatus status);

    List<Simulation> findByTargetService(String targetService);

    Optional<Simulation> findTopByTargetServiceOrderByCreatedAtDesc(String targetService);

    @Query("""
        SELECT s FROM Simulation s
        WHERE s.status = :status
          AND s.createdAt >= :since
        ORDER BY s.createdAt DESC
    """)
    List<Simulation> findRecentByStatus(
            @Param("status")  SimulationStatus status,
            @Param("since") Instant since
    );

    @Query("""
        SELECT s FROM Simulation s
        WHERE s.status = 'COMPLETED'
          AND s.actualErrorRate > s.errorRateThreshold
        ORDER BY s.createdAt DESC
    """)
    List<Simulation> findCompletedExceedingErrorThreshold();

    @Query("""
        SELECT COUNT(s) FROM Simulation s
        WHERE s.status = :status
    """)
    long countByStatus(@Param("status") SimulationStatus status);

    boolean existsByScenarioNameAndStatus(String scenarioName, SimulationStatus status);
}
