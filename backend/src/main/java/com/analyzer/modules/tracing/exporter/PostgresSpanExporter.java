package com.analyzer.modules.tracing.exporter;

import com.analyzer.modules.tracing.model.TraceSpan;
import com.analyzer.modules.tracing.repository.TraceSpanRepository;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Custom OTel SpanExporter that persists finished spans into PostgreSQL.
 * <p>
 * Spans are buffered in an in-memory queue and flushed in batches
 * via a scheduled task.  This avoids blocking the OTel export pipeline
 * on synchronous DB writes.
 */
@Slf4j
public class PostgresSpanExporter implements SpanExporter {

    private final TraceSpanRepository repository;
    private final ConcurrentLinkedQueue<SpanData> buffer = new ConcurrentLinkedQueue<>();

    public PostgresSpanExporter(TraceSpanRepository repository) {
        this.repository = repository;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        buffer.addAll(spans);
        flush();
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        List<SpanData> drained = new ArrayList<>();
        SpanData item;
        while ((item = buffer.poll()) != null) {
            drained.add(item);
        }
        if (drained.isEmpty()) {
            return CompletableResultCode.ofSuccess();
        }

        try {
            List<TraceSpan> entities = drained.stream()
                    .map(this::toEntity)
                    .toList();
            repository.saveAll(entities);
            log.debug("Flushed {} trace spans to PostgreSQL", entities.size());
        } catch (Exception e) {
            log.error("Failed to flush trace spans to PostgreSQL", e);
            return CompletableResultCode.ofFailure();
        }
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return flush();
    }

    private TraceSpan toEntity(SpanData span) {
        return TraceSpan.builder()
                .traceId(span.getTraceId())
                .spanId(span.getSpanId())
                .parentSpanId(span.getParentSpanContext().isValid()
                        ? span.getParentSpanId() : null)
                .serviceName(extractServiceName(span))
                .operationName(span.getName())
                .spanKind(span.getKind().name())
                .startTime(span.getStartEpochNanos() > 0
                        ? Instant.ofEpochSecond(0, span.getStartEpochNanos())
                        : Instant.now())
                .endTime(span.getEndEpochNanos() > 0
                        ? Instant.ofEpochSecond(0, span.getEndEpochNanos())
                        : Instant.now())
                .durationMs(Math.max(0,
                        (span.getEndEpochNanos() - span.getStartEpochNanos()) / 1_000_000))
                .statusCode(span.getStatus().getStatusCode().name())
                .statusMessage(span.getStatus().getDescription())
                .attributes(extractAttributes(span))
                .environment(System.getenv().getOrDefault("OTEL_ENVIRONMENT", "local"))
                .build();
    }

    private String extractServiceName(SpanData span) {
        Object name = span.getResource()
                .getAttribute(io.opentelemetry.api.common.AttributeKey
                        .stringKey("service.name"));
        return name != null ? name.toString() : "unknown";
    }

    private Map<String, Object> extractAttributes(SpanData span) {
        if (span.getAttributes().isEmpty()) {
            return null;
        }
        Map<String, Object> attrs = new LinkedHashMap<>();
        span.getAttributes().forEach((key, value) -> {
            attrs.put(key.getKey(), value.toString());
        });
        return attrs;
    }
}
