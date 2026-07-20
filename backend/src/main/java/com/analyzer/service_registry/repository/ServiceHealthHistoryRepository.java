package com.analyzer.service_registry.repository;

import com.analyzer.service_registry.model.ServiceHealthHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ServiceHealthHistoryRepository extends JpaRepository<ServiceHealthHistory, Long> {

    Page<ServiceHealthHistory> findByServiceIdOrderByCheckTimeDesc(String serviceId, Pageable pageable);

    List<ServiceHealthHistory> findTop20ByServiceIdOrderByCheckTimeDesc(String serviceId);

    @Query("""
        SELECT h FROM ServiceHealthHistory h
        WHERE h.service.id = :serviceId
          AND h.checkTime >= :since
        ORDER BY h.checkTime DESC
    """)
    List<ServiceHealthHistory> findRecentByService(
            @Param("serviceId") String serviceId,
            @Param("since") Instant since
    );

    @Query("""
        SELECT h FROM ServiceHealthHistory h
        WHERE h.checkTime >= :since
        ORDER BY h.checkTime DESC
    """)
    List<ServiceHealthHistory> findRecentAll(@Param("since") Instant since);

    @Query("""
        SELECT AVG(h.latencyMs) FROM ServiceHealthHistory h
        WHERE h.service.id = :serviceId
          AND h.checkTime >= :since
          AND h.latencyMs IS NOT NULL
    """)
    Double averageLatencyByService(
            @Param("serviceId") String serviceId,
            @Param("since") Instant since
    );

    @Query("""
        SELECT h.service.id, AVG(h.latencyMs) as avgLatency
        FROM ServiceHealthHistory h
        WHERE h.checkTime >= :since
          AND h.latencyMs IS NOT NULL
        GROUP BY h.service.id
        ORDER BY avgLatency DESC
    """)
    List<Object[]> findSlowestServices(@Param("since") Instant since);

    @Query("""
        SELECT COUNT(h) FROM ServiceHealthHistory h
        WHERE h.service.id = :serviceId
          AND h.status = 'OFFLINE'
          AND h.checkTime >= :since
    """)
    long countFailuresByService(
            @Param("serviceId") String serviceId,
            @Param("since") Instant since
    );
}
