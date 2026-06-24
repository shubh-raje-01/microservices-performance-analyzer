CREATE TABLE simulations (

                             id BIGSERIAL PRIMARY KEY,
                             scenario_name VARCHAR(100) NOT NULL,
                             target_service VARCHAR(100) NOT NULL,
                             duration_seconds INT NOT NULL,
                             concurrent_users INT NOT NULL,
                             error_rate_threshold DECIMAL(5, 4) NOT NULL DEFAULT 0.05,
                             status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

                             avg_latency_ms DECIMAL(10, 2),
                             p95_latency_ms DECIMAL(10, 2),
                             p99_latency_ms DECIMAL(10, 2),
                             throughput_rps DECIMAL(10, 2),
                             actual_error_rate DECIMAL(7, 4),
                             total_requests BIGINT,
                             failed_requests BIGINT,
                             failure_reason TEXT,

                             started_at TIMESTAMPTZ,
                             completed_at TIMESTAMPTZ,
                             created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                             updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE simulation_params (
                                   simulation_id BIGINT NOT NULL REFERENCES simulations(id) ON DELETE CASCADE,
                                   param_key VARCHAR(100) NOT NULL,
                                   param_value TEXT,
                                   PRIMARY KEY (simulation_id, param_key)
);

CREATE INDEX idx_simulations_status ON simulations(status);
CREATE INDEX idx_simulations_service ON simulations(target_service);
CREATE INDEX idx_simulations_created_at ON simulations(created_at DESC);