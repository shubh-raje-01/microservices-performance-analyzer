CREATE TABLE log_entries (
                             id BIGSERIAL PRIMARY KEY,
                             simulation_id BIGINT
                                 REFERENCES simulations(id) ON DELETE CASCADE,
                             level VARCHAR(10) NOT NULL,
                             category VARCHAR(30) NOT NULL,
                             service_name VARCHAR(100) NOT NULL,
                             source VARCHAR(100) NOT NULL,
                             message TEXT NOT NULL,
                             context TEXT,            -- JSON map stored as plain text
                             stack_trace TEXT,
                             duration_ms BIGINT,
                             occurred_at TIMESTAMPTZ  NOT NULL,
                             created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_log_simulation_id ON log_entries(simulation_id);
CREATE INDEX idx_log_level ON log_entries(level);
CREATE INDEX idx_log_category ON log_entries(category);
CREATE INDEX idx_log_service_occurred ON log_entries(service_name, occurred_at DESC);
CREATE INDEX idx_log_sim_level ON log_entries(simulation_id, level);
CREATE INDEX idx_log_occurred_at ON log_entries(occurred_at DESC);

-- Partial index for fast error dashboard queries
CREATE INDEX idx_log_errors
    ON log_entries(simulation_id, occurred_at DESC)
    WHERE level IN ('ERROR', 'FATAL');

-- Partial index for threshold breach detection
CREATE INDEX idx_log_performance_problematic
    ON log_entries(simulation_id)
    WHERE category = 'PERFORMANCE'
      AND level IN ('WARN', 'ERROR', 'FATAL');