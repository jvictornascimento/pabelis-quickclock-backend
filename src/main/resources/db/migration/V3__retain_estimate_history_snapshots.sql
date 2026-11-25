ALTER TABLE estimate_history ADD COLUMN action VARCHAR(30);
ALTER TABLE estimate_history ADD COLUMN snapshot_date DATE;
ALTER TABLE estimate_history ADD COLUMN snapshot_description VARCHAR(255);
ALTER TABLE estimate_history ADD COLUMN snapshot_amount_cents BIGINT;
ALTER TABLE estimate_history ADD COLUMN snapshot_approved_at TIMESTAMP;
CREATE INDEX idx_estimate_history_estimate_order ON estimate_history(estimate_id, id);
