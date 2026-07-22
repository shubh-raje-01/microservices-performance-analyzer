package com.analyzer.modules.ai.repository;

import com.analyzer.modules.ai.model.RcaFindingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface RcaFindingRepository extends JpaRepository<RcaFindingEntity, Long> {

    Page<RcaFindingEntity> findByServiceIdOrderByAnalyzedAtDesc(String serviceId, Pageable pageable);

    List<RcaFindingEntity> findTop10ByServiceIdOrderByAnalyzedAtDesc(String serviceId);

    @Query("""
        SELECT r FROM RcaFindingEntity r
        WHERE r.serviceId = :serviceId
          AND r.analyzedAt >= :since
        ORDER BY r.analyzedAt DESC
    """)
    List<RcaFindingEntity> findByServiceAndSince(
            @Param("serviceId") String serviceId,
            @Param("since") Instant since
    );

    @Query("""
        SELECT r FROM RcaFindingEntity r
        WHERE r.analyzedAt >= :since
        ORDER BY r.confidence DESC
    """)
    List<RcaFindingEntity> findRecentAll(@Param("since") Instant since);

    @Query("""
        SELECT r.bottleneckType, COUNT(r)
        FROM RcaFindingEntity r
        WHERE r.serviceId = :serviceId
          AND r.analyzedAt >= :since
        GROUP BY r.bottleneckType
        ORDER BY COUNT(r) DESC
    """)
    List<Object[]> countByBottleneckType(
            @Param("serviceId") String serviceId,
            @Param("since") Instant since
    );

    void deleteByServiceId(String serviceId);
}
