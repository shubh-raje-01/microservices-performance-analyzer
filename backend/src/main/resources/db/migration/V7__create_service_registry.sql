CREATE TABLE services (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    base_url VARCHAR(255) NOT NULL,
    health_endpoint VARCHAR(100) NOT NULL,
    metrics_endpoint VARCHAR(100),
    description VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    last_heartbeat TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    disabled_at TIMESTAMPTZ
);

CREATE TABLE service_tags (
    service_id VARCHAR(36) NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    tag VARCHAR(50) NOT NULL,
    PRIMARY KEY (service_id, tag)
);

CREATE TABLE service_health_history (
    id BIGSERIAL PRIMARY KEY,
    service_id VARCHAR(36) NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    latency_ms BIGINT,
    response_code INTEGER,
    check_time TIMESTAMPTZ NOT NULL,
    error_message VARCHAR(500)
);

CREATE TABLE service_metrics (
    id BIGSERIAL PRIMARY KEY,
    service_id VARCHAR(36) NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    metric_type VARCHAR(50),
    metric_labels VARCHAR(500),
    timestamp TIMESTAMPTZ NOT NULL
);

CREATE TABLE service_benchmarks (
    id BIGSERIAL PRIMARY KEY,
    service_id VARCHAR(36) NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    endpoint VARCHAR(200) NOT NULL,
    method VARCHAR(10),
    http_status INTEGER,
    latency_ms BIGINT NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    user_agent VARCHAR(100),
    success BOOLEAN
);

CREATE INDEX idx_service_name ON services(name);
CREATE INDEX idx_service_status ON services(status);
CREATE INDEX idx_service_tags_tag ON service_tags(tag);

CREATE INDEX idx_shh_service_id ON service_health_history(service_id);
CREATE INDEX idx_shh_check_time ON service_health_history(check_time DESC);
CREATE INDEX idx_shh_service_time ON service_health_history(service_id, check_time DESC);

CREATE INDEX idx_sm_service_id ON service_metrics(service_id);
CREATE INDEX idx_sm_timestamp ON service_metrics(timestamp DESC);
CREATE INDEX idx_sm_service_name ON service_metrics(service_id, metric_name);
CREATE INDEX idx_sm_service_time ON service_metrics(service_id, timestamp DESC);

CREATE INDEX idx_sb_service_id ON service_benchmarks(service_id);
CREATE INDEX idx_sb_endpoint ON service_benchmarks(endpoint);
CREATE INDEX idx_sb_service_endpoint ON service_benchmarks(service_id, endpoint);
