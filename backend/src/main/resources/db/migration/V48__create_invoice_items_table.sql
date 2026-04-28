-- V48__create_invoice_items_table.sql
CREATE TABLE invoice_items (
  id             BIGSERIAL PRIMARY KEY,
  invoice_id     BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
  description    VARCHAR(255) NOT NULL,
  operation_type VARCHAR(20) NOT NULL,
  quantity       INT NOT NULL DEFAULT 1,
  unit_price     DECIMAL(15,2) NOT NULL,
  line_total     DECIMAL(15,2) NOT NULL,
  created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  version        BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_invoice_items_invoice ON invoice_items(invoice_id);
