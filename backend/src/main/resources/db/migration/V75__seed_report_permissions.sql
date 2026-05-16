INSERT INTO permissions (name, description, created_at, updated_at, version)
VALUES ('REPORT_READ', 'View reports and analytics', NOW(), NOW(), 0);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name IN ('ADMIN', 'MODERATOR')
  AND p.name = 'REPORT_READ';
