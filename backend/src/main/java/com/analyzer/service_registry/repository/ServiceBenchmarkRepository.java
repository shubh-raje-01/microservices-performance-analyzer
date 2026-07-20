package com.analyzer.service_registry.repository;

import com.analyzer.service_registry.model.ServiceBenchmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ServiceBenchmarkRepository extends JpaRepository<ServiceBenchmark, Long> {

    Page<ServiceBenchmark> findByServiceIdOrderByTimestampDesc(String serviceId, Pageable pageable);

    @Query("""
        SELECT b FROM ServiceBenchmark b
        WHERE b.service.id = :serviceId
          AND b.endpoint = :endpoint
        ORDER BY b.timestamp DESC
    """)
    List<ServiceBenchmark> findByServiceAndEndpoint(
            @Param("serviceId") String serviceId,
            @Param("endpoint") String endpoint
    );

    @Query("""
        SELECT AVG(b.latencyMs) FROM ServiceBenchmark b
        WHERE b.service.id = :serviceId
          AND b.endpoint = :endpoint
          AND b.success = true
          AND b.timestamp >= :since
    """)
    Double averageLatencyByEndpoint(
            @Param("serviceId") String serviceId,
            @Param("endpoint") String endpoint,
            @Param("since") Instant since
    );

    @Query("""
        SELECT b FROM ServiceBenchmark b
        WHERE b.service.id = :serviceId
          AND b.timestamp >= :since
        ORDER BY b.timestamp DESC
    """)
    List<ServiceBenchmark> findRecentByService(
            @Param("serviceId") String serviceId,
            @Param("since") Instant since
    );
}
