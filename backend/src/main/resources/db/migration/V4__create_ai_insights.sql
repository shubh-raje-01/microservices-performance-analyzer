CREATE TABLE ai_insights (
                             id BIGSERIAL PRIMARY KEY,
                             simulation_id BIGINT NOT NULL
                                 REFERENCES simulations(id) ON DELETE CASCADE,
                             service_name VARCHAR(100) NOT NULL,
                             status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

    -- FastAPI response fields
                             summary TEXT,
                             anomaly_type VARCHAR(30),
                             anomaly_score DOUBLE PRECISION,
                             predicted_trend VARCHAR(20),
                             predicted_p95_ms DOUBLE PRECISION,
                             detected_patterns TEXT,           -- JSON array of strings
                             feature_importance TEXT,           -- JSON map of feature → score
                             raw_recommendations TEXT,           -- JSON array read by recommendation module
                             raw_response TEXT,           -- full FastAPI JSON for debugging

                             failure_reason TEXT,
                             model_version VARCHAR(50),

                             analyzed_at TIMESTAMPTZ,
                             created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_simulation_id ON ai_insights(simulation_id);
CREATE INDEX idx_ai_status ON ai_insights(status);
CREATE INDEX idx_ai_anomaly_type ON ai_insights(anomaly_type);
CREATE INDEX idx_ai_service_analyzed ON ai_insights(service_name, analyzed_at DESC);

-- Partial index for anomaly dashboard queries
CREATE INDEX idx_ai_completed_anomalies
    ON ai_insights(analyzed_at DESC)
    WHERE status = 'COMPLETED'
      AND anomaly_type NOT IN ('NONE', 'UNKNOWN');