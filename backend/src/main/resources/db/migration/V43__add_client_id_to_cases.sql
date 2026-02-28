-- V43: Link cases to clients (nullable FK — existing cases have no client yet)
ALTER TABLE cases ADD COLUMN client_id BIGINT;
ALTER TABLE cases ADD CONSTRAINT fk_cases_client
    FOREIGN KEY (client_id) REFERENCES clients(id);
CREATE INDEX idx_cases_client ON cases(client_id);
