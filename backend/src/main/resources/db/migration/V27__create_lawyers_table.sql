CREATE TABLE lawyers (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    tax_id VARCHAR(50) UNIQUE,
    email VARCHAR(100),
    phone VARCHAR(20),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_lawyers_tax_id ON lawyers(tax_id);
CREATE INDEX idx_lawyers_active ON lawyers(active);
CREATE INDEX idx_lawyers_full_name ON lawyers(first_name, last_name);
