ALTER TABLE job RENAME COLUMN scheme TO trigger_source;
ALTER TABLE job RENAME COLUMN process_a_required TO is_a1_enabled;

ALTER TABLE job ADD is_a2_enabled BOOLEAN DEFAULT false;
ALTER TABLE job ADD is_b_enabled BOOLEAN DEFAULT false;
