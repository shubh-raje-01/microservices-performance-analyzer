package com.analyzer.modules.tracing.model;

import com.analyzer.infrastructure.database.JsonMapConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "trace_spans", indexes = {
        @Index(name = "idx_ts_trace_id", columnList = "traceId"),
        @Index(name = "idx_ts_service", columnList = "serviceName"),
        @Index(name = "idx_ts_start", columnList = "startTime DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TraceSpan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trace_id", nullable = false, length = 64)
    private String traceId;

    @Column(name = "span_id", nullable = false, length = 32)
    private String spanId;

    @Column(name = "parent_span_id", length = 32)
    private String parentSpanId;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "operation_name", nullable = false, length = 512)
    private String operationName;

    @Column(name = "span_kind", nullable = false, length = 32)
    @Builder.Default
    private String spanKind = "INTERNAL";

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    @Column(name = "status_code", nullable = false, length = 16)
    @Builder.Default
    private String statusCode = "OK";

    @Column(name = "status_message")
    private String statusMessage;

    @Convert(converter = JsonMapConverter.class)
    @Column(name = "attributes", columnDefinition = "jsonb")
    private Map<String, Object> attributes;

    @Column(name = "environment", length = 64)
    private String environment;
}
