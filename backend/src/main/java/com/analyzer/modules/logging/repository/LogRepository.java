package com.analyzer.modules.logging.repository;

import com.analyzer.modules.logging.model.LogCategory;
import com.analyzer.modules.logging.model.LogEntry;
import com.analyzer.modules.logging.model.LogLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<LogEntry, Long>, JpaSpecificationExecutor<LogEntry> {

    // ── Basic finders =>

    Page<LogEntry> findBySimulationIdOrderByOccurredAtDesc(
            Long simulationId, Pageable pageable);

    List<LogEntry> findBySimulationIdOrderByOccurredAtAsc(Long simulationId);

    List<LogEntry> findBySimulationIdAndLevel(Long simulationId, LogLevel level);

    List<LogEntry> findBySimulationIdAndLevelIn(Long simulationId, List<LogLevel> levels);

    List<LogEntry> findBySimulationIdAndCategory(Long simulationId, LogCategory category);

    // ── Count queries — used to build LogSummaryDto =>

    long countBySimulationId(Long simulationId);

    long countBySimulationIdAndLevel(Long simulationId, LogLevel level);

    long countBySimulationIdAndCategory(Long simulationId, LogCategory category);

    @Query("""
        SELECT l.level, COUNT(l) FROM LogEntry l
        WHERE l.simulationId = :simId
        GROUP BY l.level
    """)
    List<Object[]> countGroupedByLevel(@Param("simId") Long simulationId);

    @Query("""
        SELECT l.category, COUNT(l) FROM LogEntry l
        WHERE l.simulationId = :simId
        GROUP BY l.category
    """)
    List<Object[]> countGroupedByCategory(@Param("simId") Long simulationId);

    // ── Problematic entries =>

    @Query("""
        SELECT l FROM LogEntry l
        WHERE l.simulationId = :simId
          AND l.level IN ('WARN', 'ERROR', 'FATAL')
        ORDER BY l.level DESC, l.occurredAt DESC
    """)
    List<LogEntry> findProblematicBySimulation(@Param("simId") Long simulationId);

    @Query("""
        SELECT l FROM LogEntry l
        WHERE l.simulationId = :simId
          AND l.level IN ('ERROR', 'FATAL')
        ORDER BY l.occurredAt DESC
    """)
    List<LogEntry> findErrorsBySimulation(@Param("simId") Long simulationId);

    // ── Time-range queries =>

    @Query("""
        SELECT l FROM LogEntry l
        WHERE l.serviceName = :service
          AND l.occurredAt >= :since
        ORDER BY l.occurredAt DESC
    """)
    List<LogEntry> findRecentByService(
            @Param("service") String serviceName,
            @Param("since")   Instant since);

    @Query("""
        SELECT l FROM LogEntry l
        WHERE l.occurredAt >= :since
        ORDER BY l.occurredAt DESC
    """)
    List<LogEntry> findRecentSystemLogs(
            @Param("since") Instant since,
            Pageable pageable);

    // ── Cross-simulation analysis =>

    @Query("""
        SELECT DISTINCT l.serviceName FROM LogEntry l
        ORDER BY l.serviceName ASC
    """)
    List<String> findDistinctServiceNames();

    @Query("""
        SELECT COUNT(l) FROM LogEntry l
        WHERE l.level IN ('ERROR', 'FATAL')
          AND l.occurredAt >= :since
    """)
    long countErrorsSince(@Param("since") Instant since);

    // ── Deletion =>

    void deleteBySimulationId(Long simulationId);
}