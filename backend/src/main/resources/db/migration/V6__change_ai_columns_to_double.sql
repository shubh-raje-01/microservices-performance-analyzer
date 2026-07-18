ALTER TABLE ai_insights
ALTER COLUMN anomaly_score
TYPE DOUBLE PRECISION
USING anomaly_score::double precision;

ALTER TABLE ai_insights
ALTER COLUMN predicted_p95_ms
TYPE DOUBLE PRECISION
USING predicted_p95_ms::double precision;