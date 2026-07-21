CREATE TABLE trace_spans (
    id              BIGSERIAL PRIMARY KEY,
    trace_id        VARCHAR(64)  NOT NULL,
    span_id         VARCHAR(32)  NOT NULL,
    parent_span_id  VARCHAR(32),
    service_name    VARCHAR(255) NOT NULL,
    operation_name  VARCHAR(512) NOT NULL,
    span_kind       VARCHAR(32)  NOT NULL DEFAULT 'INTERNAL',
    start_time      TIMESTAMPTZ  NOT NULL,
    end_time        TIMESTAMPTZ  NOT NULL,
    duration_ms     BIGINT       NOT NULL,
    status_code     VARCHAR(16)  NOT NULL DEFAULT 'OK',
    status_message  TEXT,
    attributes      JSONB,
    environment     VARCHAR(64)
);

CREATE INDEX idx_ts_trace_id  ON trace_spans(trace_id);
CREATE INDEX idx_ts_service   ON trace_spans(service_name);
CREATE INDEX idx_ts_start     ON trace_spans(start_time DESC);
CREATE INDEX idx_ts_parent    ON trace_spans(parent_span_id);
CREATE INDEX idx_ts_status    ON trace_spans(status_code);
