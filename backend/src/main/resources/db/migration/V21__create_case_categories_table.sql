CREATE TABLE case_categories (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name_ar VARCHAR(255) NOT NULL,
    name_fr VARCHAR(255),
    case_type_id BIGINT,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (case_type_id) REFERENCES case_types(id)
);

CREATE INDEX idx_case_categories_code ON case_categories(code);
CREATE INDEX idx_case_categories_case_type ON case_categories(case_type_id);
CREATE INDEX idx_case_categories_active ON case_categories(active);
