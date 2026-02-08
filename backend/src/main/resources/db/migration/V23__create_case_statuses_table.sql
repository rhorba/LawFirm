CREATE TABLE case_statuses (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name_fr VARCHAR(100) NOT NULL,
    name_ar VARCHAR(100),
    sort_order INT NOT NULL DEFAULT 0,
    is_terminal BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_case_statuses_code ON case_statuses(code);
CREATE INDEX idx_case_statuses_sort_order ON case_statuses(sort_order);
