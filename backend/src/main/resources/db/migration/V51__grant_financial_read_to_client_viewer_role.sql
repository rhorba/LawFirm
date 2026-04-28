-- V51: Grant financial read permissions to CLIENT_VIEWER role
-- The viewer test account uses CLIENT_VIEWER (not USER), so V50 had no effect on it.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'CLIENT_VIEWER'
  AND p.name IN ('FINANCIAL_READ', 'INVOICE_READ')
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
