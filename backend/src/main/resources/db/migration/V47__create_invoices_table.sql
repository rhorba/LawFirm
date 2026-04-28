-- V47__create_invoices_table.sql
CREATE SEQUENCE invoice_number_seq START 1;

CREATE TABLE invoices (
  id             BIGSERIAL PRIMARY KEY,
  case_id        BIGINT NOT NULL REFERENCES cases(id),
  invoice_number VARCHAR(50) NOT NULL UNIQUE,
  issue_date     DATE NOT NULL,
  due_date       DATE,
  status         VARCHAR(15) NOT NULL DEFAULT 'DRAFT'
                   CHECK (status IN ('DRAFT', 'SENT', 'PAID', 'CANCELLED')),
  subtotal       DECIMAL(15,2) NOT NULL DEFAULT 0,
  tax_amount     DECIMAL(15,2) NOT NULL DEFAULT 0,
  total_amount   DECIMAL(15,2) NOT NULL DEFAULT 0,
  notes          TEXT,
  created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  version        BIGINT NOT NULL DEFAULT 0,
  deleted_at     TIMESTAMP
);

CREATE INDEX idx_invoices_case   ON invoices(case_id);
CREATE INDEX idx_invoices_status ON invoices(status);
