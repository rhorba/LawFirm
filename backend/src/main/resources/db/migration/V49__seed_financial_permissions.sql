-- V49__seed_financial_permissions.sql
INSERT INTO permissions (name, description, resource, action, created_at, updated_at, version) VALUES
  ('FINANCIAL_READ',   'View financial transactions and summaries', 'FINANCIAL', 'READ',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  ('FINANCIAL_CREATE', 'Create financial transactions',             'FINANCIAL', 'CREATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  ('FINANCIAL_UPDATE', 'Soft-delete financial transactions',        'FINANCIAL', 'UPDATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  ('INVOICE_READ',     'View invoices',                            'INVOICE',   'READ',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  ('INVOICE_CREATE',   'Create invoices',                          'INVOICE',   'CREATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  ('INVOICE_MANAGE',   'Update invoice status and delete invoices', 'INVOICE',   'MANAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ADMIN: all 6
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN (
    'FINANCIAL_READ','FINANCIAL_CREATE','FINANCIAL_UPDATE',
    'INVOICE_READ','INVOICE_CREATE','INVOICE_MANAGE'
  );

-- MODERATOR: read-only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'MODERATOR'
  AND p.name IN ('FINANCIAL_READ', 'INVOICE_READ');
