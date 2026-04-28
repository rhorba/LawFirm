-- V61__add_audit_snapshot_columns.sql
-- Add before/after JSON snapshot columns to audit_logs for full change history.
ALTER TABLE audit_logs ADD COLUMN old_values TEXT;
ALTER TABLE audit_logs ADD COLUMN new_values TEXT;
