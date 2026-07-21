ALTER TABLE trace_spans ALTER COLUMN attributes TYPE TEXT USING attributes::text;
