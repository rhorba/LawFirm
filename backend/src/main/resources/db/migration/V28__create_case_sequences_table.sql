CREATE TABLE case_sequences (
    id BIGSERIAL PRIMARY KEY,
    "year" INT NOT NULL,
    case_type_code VARCHAR(20) NOT NULL,
    last_sequence INT NOT NULL DEFAULT 0,
    UNIQUE("year", case_type_code)
);

CREATE INDEX idx_case_sequences_year_type ON case_sequences("year", case_type_code);
