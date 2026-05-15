CREATE TABLE conflict_checks (
    id                   BIGSERIAL    PRIMARY KEY,
    search_name          VARCHAR(255) NOT NULL,
    result               VARCHAR(20)  NOT NULL,
    cleared_note         TEXT,
    performed_by_user_id BIGINT       NOT NULL,
    cleared_by_user_id   BIGINT,
    version              BIGINT       NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_conflict_check_performed_by FOREIGN KEY (performed_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_conflict_check_cleared_by   FOREIGN KEY (cleared_by_user_id)   REFERENCES users(id),
    CONSTRAINT chk_conflict_check_result CHECK (result IN ('CLEAR','CONFLICT_FOUND'))
);

CREATE INDEX idx_conflict_checks_search_name   ON conflict_checks(LOWER(search_name));
CREATE INDEX idx_conflict_checks_performed_by  ON conflict_checks(performed_by_user_id);
