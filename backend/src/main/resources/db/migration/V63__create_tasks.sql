CREATE TABLE tasks (
    id                  BIGSERIAL    PRIMARY KEY,
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    due_date            DATE         NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'TODO',
    priority            VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    case_id             BIGINT       NOT NULL,
    assigned_lawyer_id  BIGINT,
    created_by_user_id  BIGINT       NOT NULL,
    version             BIGINT       NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_tasks_case     FOREIGN KEY (case_id)            REFERENCES cases(id),
    CONSTRAINT fk_tasks_lawyer   FOREIGN KEY (assigned_lawyer_id) REFERENCES lawyers(id),
    CONSTRAINT fk_tasks_user     FOREIGN KEY (created_by_user_id) REFERENCES users(id),
    CONSTRAINT chk_tasks_status  CHECK (status   IN ('TODO','IN_PROGRESS','DONE','CANCELLED')),
    CONSTRAINT chk_tasks_priority CHECK (priority IN ('LOW','MEDIUM','HIGH'))
);

CREATE INDEX idx_tasks_case_id            ON tasks(case_id);
CREATE INDEX idx_tasks_assigned_lawyer_id ON tasks(assigned_lawyer_id);
CREATE INDEX idx_tasks_due_date           ON tasks(due_date);
CREATE INDEX idx_tasks_status             ON tasks(status);
