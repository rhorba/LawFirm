CREATE TABLE cases (
    id BIGSERIAL PRIMARY KEY,
    "year" INT NOT NULL,
    sequence_number INT NOT NULL,
    full_case_number VARCHAR(255) NOT NULL UNIQUE,
    registration_date DATE NOT NULL,
    case_description VARCHAR(500) NOT NULL,
    matter_description TEXT,
    tribunal_id BIGINT NOT NULL,
    case_type_id BIGINT NOT NULL,
    case_category_id BIGINT,
    lawyer_id BIGINT NOT NULL,
    status_id BIGINT NOT NULL,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (tribunal_id) REFERENCES tribunals(id),
    FOREIGN KEY (case_type_id) REFERENCES case_types(id),
    FOREIGN KEY (case_category_id) REFERENCES case_categories(id),
    FOREIGN KEY (lawyer_id) REFERENCES lawyers(id),
    FOREIGN KEY (status_id) REFERENCES case_statuses(id)
);

CREATE UNIQUE INDEX idx_cases_full_number ON cases(full_case_number);
CREATE INDEX idx_cases_year ON cases("year");
CREATE INDEX idx_cases_tribunal ON cases(tribunal_id);
CREATE INDEX idx_cases_case_type ON cases(case_type_id);
CREATE INDEX idx_cases_case_category ON cases(case_category_id);
CREATE INDEX idx_cases_lawyer ON cases(lawyer_id);
CREATE INDEX idx_cases_status ON cases(status_id);
CREATE INDEX idx_cases_registration_date ON cases(registration_date);
-- Partial index not supported in H2, but works in PostgreSQL production
-- CREATE INDEX idx_cases_deleted_at ON cases(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_cases_year_sequence ON cases("year", sequence_number);
