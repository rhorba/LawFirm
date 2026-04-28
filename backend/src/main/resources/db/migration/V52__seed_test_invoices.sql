-- V52: Seed test invoices for scenario testing
-- Written in plain SQL (no PL/pgSQL) to stay compatible with H2 (dev) and PostgreSQL (prod).
-- Uses (SELECT MAX(id) FROM invoices) to reference the last inserted invoice.

-- ── Invoice 1: PAID ──────────────────────────────────────────────────────────
INSERT INTO invoices (case_id, invoice_number, issue_date, due_date, status, subtotal, tax_amount, total_amount, notes, created_at, updated_at, version)
SELECT
  (SELECT id FROM cases ORDER BY id LIMIT 1),
  'FAC-2026-' || LPAD(CAST(NEXTVAL('invoice_number_seq') AS VARCHAR), 4, '0'),
  '2026-01-15', '2026-02-15', 'PAID',
  1700.00, 100.00, 1800.00, NULL,
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0;

INSERT INTO invoice_items (invoice_id, description, operation_type, quantity, unit_price, line_total, created_at, updated_at, version)
VALUES ((SELECT MAX(id) FROM invoices), 'Consultation juridique', 'OTHER', 2, 750.00, 1500.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO invoice_items (invoice_id, description, operation_type, quantity, unit_price, line_total, created_at, updated_at, version)
VALUES ((SELECT MAX(id) FROM invoices), 'Frais de dossier', 'DOCUMENT_FEE', 1, 200.00, 200.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ── Invoice 2: SENT ──────────────────────────────────────────────────────────
INSERT INTO invoices (case_id, invoice_number, issue_date, due_date, status, subtotal, tax_amount, total_amount, notes, created_at, updated_at, version)
SELECT
  (SELECT id FROM cases ORDER BY id LIMIT 1),
  'FAC-2026-' || LPAD(CAST(NEXTVAL('invoice_number_seq') AS VARCHAR), 4, '0'),
  '2026-02-01', '2026-03-01', 'SENT',
  3000.00, 0.00, 3000.00, NULL,
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0;

INSERT INTO invoice_items (invoice_id, description, operation_type, quantity, unit_price, line_total, created_at, updated_at, version)
VALUES ((SELECT MAX(id) FROM invoices), 'Honoraires de representation', 'INTERVENTION_FEE', 1, 2000.00, 2000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO invoice_items (invoice_id, description, operation_type, quantity, unit_price, line_total, created_at, updated_at, version)
VALUES ((SELECT MAX(id) FROM invoices), 'Frais de procedure', 'PROCEDURE_FEE', 2, 500.00, 1000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ── Invoice 3: DRAFT ─────────────────────────────────────────────────────────
INSERT INTO invoices (case_id, invoice_number, issue_date, due_date, status, subtotal, tax_amount, total_amount, notes, created_at, updated_at, version)
SELECT
  (SELECT id FROM cases ORDER BY id LIMIT 1),
  'FAC-2026-' || LPAD(CAST(NEXTVAL('invoice_number_seq') AS VARCHAR), 4, '0'),
  '2026-02-28', NULL, 'DRAFT',
  500.00, 0.00, 500.00, 'Brouillon a finaliser',
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0;

INSERT INTO invoice_items (invoice_id, description, operation_type, quantity, unit_price, line_total, created_at, updated_at, version)
VALUES ((SELECT MAX(id) FROM invoices), 'Taxe judiciaire', 'JUDICIAL_TAX', 1, 500.00, 500.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
