-- V38: Migrate single lawyer to case_lawyers join table, then enhance cases table

-- 1. Migrate existing single lawyer assignment to join table
INSERT INTO case_lawyers (case_id, lawyer_id)
SELECT id, lawyer_id
FROM   cases
WHERE  lawyer_id IS NOT NULL;

-- 2. Drop old single-lawyer FK column
ALTER TABLE cases DROP COLUMN lawyer_id;

-- 3. Add new columns (one per statement for H2 compatibility)
ALTER TABLE cases ADD COLUMN opposing_party       VARCHAR(255);
ALTER TABLE cases ADD COLUMN outcome              VARCHAR(20);
ALTER TABLE cases ADD COLUMN outcome_notes        TEXT;
ALTER TABLE cases ADD COLUMN priority             VARCHAR(20) NOT NULL DEFAULT 'NORMAL';
ALTER TABLE cases ADD COLUMN initial_payment_date DATE;
ALTER TABLE cases ADD COLUMN fiscal_year          SMALLINT;
ALTER TABLE cases ADD COLUMN parent_case_id       BIGINT;

-- 4. Add FK for parent_case_id (self-referential)
ALTER TABLE cases
    ADD CONSTRAINT fk_cases_parent FOREIGN KEY (parent_case_id) REFERENCES cases(id);

-- 5. Indexes
CREATE INDEX idx_cases_priority   ON cases(priority);
CREATE INDEX idx_cases_parent     ON cases(parent_case_id);
