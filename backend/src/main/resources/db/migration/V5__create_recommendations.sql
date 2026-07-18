CREATE TABLE recommendations (
                                 id BIGSERIAL PRIMARY KEY,
                                 simulation_id BIGINT NOT NULL
                                     REFERENCES simulations(id) ON DELETE CASCADE,
                                 ai_insight_id BIGINT,
                                 category VARCHAR(20) NOT NULL,
                                 priority VARCHAR(10) NOT NULL,
                                 source VARCHAR(15) NOT NULL DEFAULT 'AI_MODEL',
                                 title VARCHAR(200) NOT NULL,
                                 description TEXT NOT NULL,
                                 action TEXT NOT NULL,
                                 confidence_score DOUBLE PRECISION,
                                 estimated_impact DOUBLE PRECISION,
                                 composite_score  DOUBLE PRECISION  NOT NULL DEFAULT 0.0,
                                 rank INT NOT NULL DEFAULT 0,
                                 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_rec_simulation_id ON recommendations(simulation_id);
CREATE INDEX idx_rec_priority ON recommendations(priority);
CREATE INDEX idx_rec_category ON recommendations(category);
CREATE INDEX idx_rec_sim_rank ON recommendations(simulation_id, rank);
CREATE INDEX idx_rec_sim_score  ON recommendations(simulation_id, composite_score DESC);

-- Fast retrieval of high-priority recommendations for the dashboard
CREATE INDEX idx_rec_high_priority
    ON recommendations(simulation_id, composite_score DESC)
    WHERE priority = 'HIGH';