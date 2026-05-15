CREATE TABLE time_entries (
    id           BIGSERIAL      PRIMARY KEY,
    case_id      BIGINT         NOT NULL,
    lawyer_id    BIGINT         NOT NULL,
    entry_date   DATE           NOT NULL,
    hours        NUMERIC(6,2)   NOT NULL,
    description  TEXT           NOT NULL,
    hourly_rate  NUMERIC(10,2)  NOT NULL DEFAULT 0,
    billable     BOOLEAN        NOT NULL DEFAULT TRUE,
    billed       BOOLEAN        NOT NULL DEFAULT FALSE,
    invoice_id   BIGINT,
    version      BIGINT         NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_time_entry_case    FOREIGN KEY (case_id)    REFERENCES cases(id),
    CONSTRAINT fk_time_entry_lawyer  FOREIGN KEY (lawyer_id)  REFERENCES lawyers(id),
    CONSTRAINT fk_time_entry_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE SET NULL,
    CONSTRAINT chk_time_entry_hours  CHECK (hours > 0),
    CONSTRAINT chk_time_entry_rate   CHECK (hourly_rate >= 0)
);

CREATE INDEX idx_time_entries_case_id   ON time_entries(case_id);
CREATE INDEX idx_time_entries_lawyer_id ON time_entries(lawyer_id);
CREATE INDEX idx_time_entries_date      ON time_entries(entry_date);
