-- V50__grant_financial_read_to_user_role.sql
-- Grant read-only financial access to the base USER role
-- so that viewer accounts can access the ledger and invoices pages.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'USER'
  AND p.name IN ('FINANCIAL_READ', 'INVOICE_READ')
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
