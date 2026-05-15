CREATE TABLE conflict_parties (
    id          BIGSERIAL    PRIMARY KEY,
    case_id     BIGINT       NOT NULL,
    party_type  VARCHAR(30)  NOT NULL,
    name        VARCHAR(255) NOT NULL,
    notes       TEXT,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_conflict_party_case FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE,
    CONSTRAINT chk_conflict_party_type CHECK (party_type IN ('CLIENT','OPPOSING_PARTY','OPPOSING_COUNSEL','WITNESS','RELATED_ENTITY'))
);

CREATE INDEX idx_conflict_parties_case_id ON conflict_parties(case_id);
CREATE INDEX idx_conflict_parties_name    ON conflict_parties(LOWER(name));
