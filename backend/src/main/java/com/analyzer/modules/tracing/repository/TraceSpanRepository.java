package com.analyzer.modules.tracing.repository;

import com.analyzer.modules.tracing.model.TraceSpan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface TraceSpanRepository extends JpaRepository<TraceSpan, Long> {

    List<TraceSpan> findByTraceIdOrderByStartTimeAsc(String traceId);

    Page<TraceSpan> findByStartTimeAfter(Instant since, Pageable pageable);

    Page<TraceSpan> findByServiceNameAndStartTimeAfter(String serviceName, Instant since, Pageable pageable);

    @Query("""
        SELECT t.traceId
        FROM TraceSpan t
        WHERE t.startTime >= :since
        GROUP BY t.traceId
        ORDER BY MAX(t.startTime) DESC
        """)
    Page<String> findDistinctTraceIds(@Param("since") Instant since, Pageable pageable);

    @Query("""
            SELECT t.traceId
            FROM TraceSpan t
            WHERE t.serviceName = :serviceName
            AND t.startTime >= :since
            GROUP BY t.traceId
            ORDER BY MAX(t.startTime) DESC
            """)
    Page<String> findDistinctTraceIdsByService(
            @Param("serviceName") String serviceName,
            @Param("since") Instant since,
            Pageable pageable);

    @Query("""
            SELECT t.serviceName FROM TraceSpan t
            WHERE t.traceId = :traceId
            GROUP BY t.serviceName
            ORDER BY MIN(t.startTime)
            """)
    List<String> findServiceNamesByTraceId(@Param("traceId") String traceId);

    @Query("""
            SELECT t FROM TraceSpan t
            WHERE t.startTime >= :since
            ORDER BY t.startTime DESC
            """)
    List<TraceSpan> findRecentSpans(@Param("since") Instant since);

    @Query(value = """
            SELECT * FROM trace_spans
            WHERE start_time >= :since
            ORDER BY start_time DESC
            LIMIT :maxResults
            """, nativeQuery = true)
    List<TraceSpan> findRecentSpansBounded(
            @Param("since") Instant since,
            @Param("maxResults") int maxResults);
}
