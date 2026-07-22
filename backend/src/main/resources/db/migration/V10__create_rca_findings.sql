CREATE TABLE rca_findings (
    id BIGSERIAL PRIMARY KEY,
    service_id VARCHAR(36) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    bottleneck_type VARCHAR(50) NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    reasoning TEXT NOT NULL,
    evidence_json TEXT,
    recommendations_json TEXT,
    summary TEXT,
    metrics_snapshot_json TEXT,
    model_version VARCHAR(20),
    analyzed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_rca_service_id ON rca_findings(service_id);
CREATE INDEX idx_rca_analyzed_at ON rca_findings(analyzed_at DESC);
CREATE INDEX idx_rca_service_time ON rca_findings(service_id, analyzed_at DESC);
CREATE INDEX idx_rca_bottleneck ON rca_findings(bottleneck_type);
