CREATE TABLE case_type_statuses (
    case_type_id BIGINT NOT NULL,
    status_id BIGINT NOT NULL,
    PRIMARY KEY (case_type_id, status_id),
    FOREIGN KEY (case_type_id) REFERENCES case_types(id) ON DELETE CASCADE,
    FOREIGN KEY (status_id) REFERENCES case_statuses(id) ON DELETE CASCADE
);

CREATE INDEX idx_case_type_statuses_case_type ON case_type_statuses(case_type_id);
CREATE INDEX idx_case_type_statuses_status ON case_type_statuses(status_id);
