ALTER TABLE simulations
    ADD COLUMN peak_throughput_rps DOUBLE PRECISION,
    ADD COLUMN throughput_variability DOUBLE PRECISION;