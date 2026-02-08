CREATE TABLE case_types (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name_fr VARCHAR(100) NOT NULL,
    name_ar VARCHAR(100),
    number_format_template VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_case_types_code ON case_types(code);
CREATE INDEX idx_case_types_active ON case_types(active);
