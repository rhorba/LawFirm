-- V41: Create clients table
CREATE TABLE clients (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    version          BIGINT NOT NULL DEFAULT 0,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW(),

    client_type      VARCHAR(20)  NOT NULL,

    -- Common fields
    first_name       VARCHAR(100),
    last_name        VARCHAR(100),
    phone            VARCHAR(20),
    email            VARCHAR(100) UNIQUE,
    address          TEXT,
    notes            TEXT,
    active           BOOLEAN NOT NULL DEFAULT TRUE,

    -- INDIVIDUAL only
    cin              VARCHAR(20) UNIQUE,
    gender           VARCHAR(10),
    date_of_birth    DATE,

    -- CORPORATE / GOVERNMENT only
    company_name     VARCHAR(200),
    tax_number       VARCHAR(50) UNIQUE
);

CREATE INDEX idx_clients_type   ON clients(client_type);
CREATE INDEX idx_clients_active ON clients(active);
CREATE INDEX idx_clients_cin    ON clients(cin);
CREATE INDEX idx_clients_name   ON clients(last_name, first_name);
