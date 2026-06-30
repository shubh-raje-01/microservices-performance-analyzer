package com.analyzer.modules.metrics.repository;

import com.analyzer.modules.metrics.model.MetricSeverity;
import com.analyzer.modules.metrics.model.MetricSnapshot;
import com.analyzer.modules.metrics.model.MetricType;
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
public interface MetricsRepository extends JpaRepository<MetricSnapshot, Long> {

    // ── Basic finders =>

    List<MetricSnapshot> findBySimulationIdOrderByRecordedAtAsc(Long simulationId);

    List<MetricSnapshot> findBySimulationIdAndMetricType(
            Long simulationId, MetricType type);

    Page<MetricSnapshot> findBySimulationId(Long simulationId, Pageable pageable);

    List<MetricSnapshot> findByServiceNameOrderByRecordedAtDesc(String serviceName);

    List<MetricSnapshot> findBySimulationIdAndSeverity(
            Long simulationId, MetricSeverity severity);

    // ── Aggregation queries =>

    @Query("""
        SELECT AVG(m.value) FROM MetricSnapshot m
        WHERE m.simulationId = :simId
          AND m.metricType   = :type
    """)
    Optional<Double> avgValueBySimulationAndType(
            @Param("simId") Long simulationId,
            @Param("type")  MetricType type);

    @Query("""
        SELECT MAX(m.value) FROM MetricSnapshot m
        WHERE m.simulationId = :simId
          AND m.metricType   = :type
    """)
    Optional<Double> maxValueBySimulationAndType(
            @Param("simId") Long simulationId,
            @Param("type")  MetricType type);

    @Query("""
        SELECT MIN(m.value) FROM MetricSnapshot m
        WHERE m.simulationId = :simId
          AND m.metricType   = :type
    """)
    Optional<Double> minValueBySimulationAndType(
            @Param("simId") Long simulationId,
            @Param("type")  MetricType type);

    @Query("""
        SELECT COUNT(m) FROM MetricSnapshot m
        WHERE m.simulationId = :simId
          AND m.severity     = :severity
    """)
    long countBySeverity(
            @Param("simId")    Long simulationId,
            @Param("severity") MetricSeverity severity);

    // ── Latency-specific aggregation ──────────────────────────

    @Query("""
        SELECT AVG(m.p95Ms) FROM MetricSnapshot m
        WHERE m.simulationId = :simId
          AND m.metricType   = 'LATENCY'
          AND m.p95Ms IS NOT NULL
    """)
    Optional<Double> avgP95BySimulation(@Param("simId") Long simulationId);

    @Query("""
        SELECT AVG(m.p99Ms) FROM MetricSnapshot m
        WHERE m.simulationId = :simId
          AND m.metricType   = 'LATENCY'
          AND m.p99Ms IS NOT NULL
    """)
    Optional<Double> avgP99BySimulation(@Param("simId") Long simulationId);

    @Query("""
        SELECT MAX(m.p99Ms) FROM MetricSnapshot m
        WHERE m.simulationId = :simId
          AND m.metricType   = 'LATENCY'
    """)
    Optional<Double> maxP99BySimulation(@Param("simId") Long simulationId);

    // ── Cross-simulation queries =>

    @Query("""
        SELECT m FROM MetricSnapshot m
        WHERE m.serviceName  = :service
          AND m.metricType   = :type
          AND m.recordedAt  >= :since
        ORDER BY m.recordedAt DESC
    """)
    List<MetricSnapshot> findRecentByServiceAndType(
            @Param("service") String serviceName,
            @Param("type")    MetricType type,
            @Param("since")   Instant since);

    @Query("""
        SELECT m FROM MetricSnapshot m
        WHERE m.simulationId = :simId
          AND m.severity     IN ('WARNING', 'CRITICAL')
        ORDER BY m.severity DESC, m.recordedAt DESC
    """)
    List<MetricSnapshot> findProblematicBySimulation(@Param("simId") Long simulationId);

    @Query("""
        SELECT DISTINCT m.serviceName FROM MetricSnapshot m
        ORDER BY m.serviceName ASC
    """)
    List<String> findDistinctServiceNames();

    @Query("""
        SELECT COUNT(m) FROM MetricSnapshot m
        WHERE m.simulationId = :simId
    """)
    long countBySimulationId(@Param("simId") Long simulationId);

    // ── Time-windowed queries for trend analysis =>

    @Query("""
        SELECT m FROM MetricSnapshot m
        WHERE m.simulationId = :simId
          AND m.recordedAt  >= :from
          AND m.recordedAt  <= :to
        ORDER BY m.recordedAt ASC
    """)
    List<MetricSnapshot> findBySimulationAndTimeWindow(
            @Param("simId") Long simulationId,
            @Param("from")  Instant from,
            @Param("to")    Instant to);

    // ── Deletion =>

    void deleteBySimulationId(Long simulationId);
}