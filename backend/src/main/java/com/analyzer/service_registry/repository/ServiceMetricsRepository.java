package com.analyzer.service_registry.repository;

import com.analyzer.service_registry.model.ServiceMetrics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ServiceMetricsRepository extends JpaRepository<ServiceMetrics, Long> {

    Page<ServiceMetrics> findByServiceIdOrderByTimestampDesc(String serviceId, Pageable pageable);

    List<ServiceMetrics> findTop100ByServiceIdAndMetricNameOrderByTimestampDesc(
            String serviceId, String metricName);

    @Query("""
        SELECT m FROM ServiceMetrics m
        WHERE m.service.id = :serviceId
          AND m.metricName = :metricName
          AND m.timestamp >= :since
        ORDER BY m.timestamp DESC
    """)
    List<ServiceMetrics> findMetricTrend(
            @Param("serviceId") String serviceId,
            @Param("metricName") String metricName,
            @Param("since") Instant since
    );

    @Query("""
        SELECT m FROM ServiceMetrics m
        WHERE m.service.id = :serviceId
          AND m.timestamp >= :since
        ORDER BY m.timestamp DESC
    """)
    List<ServiceMetrics> findRecentByService(
            @Param("serviceId") String serviceId,
            @Param("since") Instant since
    );

    @Query("""
        SELECT m.metricName, AVG(m.metricValue) as avgVal
        FROM ServiceMetrics m
        WHERE m.service.id = :serviceId
          AND m.timestamp >= :since
        GROUP BY m.metricName
    """)
    List<Object[]> findMetricAverages(
            @Param("serviceId") String serviceId,
            @Param("since") Instant since
    );

    @Query("""
        SELECT m.service.id, m.metricName, AVG(m.metricValue) as avgVal
        FROM ServiceMetrics m
        WHERE m.timestamp >= :since
        GROUP BY m.service.id, m.metricName
    """)
    List<Object[]> findAllServiceMetricAverages(@Param("since") Instant since);

    void deleteByTimestampBefore(Instant cutoff);
}
