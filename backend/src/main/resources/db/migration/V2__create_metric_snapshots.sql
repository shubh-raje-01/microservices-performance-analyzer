CREATE TABLE metric_snapshots (
                                  id BIGSERIAL PRIMARY KEY,
                                  simulation_id BIGINT NOT NULL
                                      REFERENCES simulations(id) ON DELETE CASCADE,
                                  service_name VARCHAR(100) NOT NULL,
                                  metric_type VARCHAR(30) NOT NULL,
                                  value DOUBLE PRECISION NOT NULL,
                                  unit VARCHAR(20),

    -- Latency percentiles (populated for LATENCY rows only)
                                  avg_ms DOUBLE PRECISION,
                                  p50_ms DOUBLE PRECISION,
                                  p95_ms DOUBLE PRECISION,
                                  p99_ms DOUBLE PRECISION,
                                  max_ms DOUBLE PRECISION,

    -- Request counts (populated for ERROR_RATE and AVAILABILITY rows)
                                  total_requests BIGINT,
                                  failed_requests BIGINT,

                                  severity VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
                                  notes VARCHAR(500),
                                  recorded_at TIMESTAMPTZ NOT NULL,
                                  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_metric_simulation_id ON metric_snapshots(simulation_id);
CREATE INDEX idx_metric_type ON metric_snapshots(metric_type);
CREATE INDEX idx_metric_service_recorded ON metric_snapshots(service_name, recorded_at DESC);
CREATE INDEX idx_metric_severity ON metric_snapshots(severity);
CREATE INDEX idx_metric_sim_type ON metric_snapshots(simulation_id, metric_type);