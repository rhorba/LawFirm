INSERT INTO permissions (name, description) VALUES
    ('CONFLICT_READ',   'View conflict check results and party lists'),
    ('CONFLICT_CREATE', 'Perform conflict checks and manage case parties'),
    ('CONFLICT_MANAGE', 'Clear conflicts and manage all conflict data');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('CONFLICT_READ', 'CONFLICT_CREATE', 'CONFLICT_MANAGE');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'MODERATOR'
  AND p.name = 'CONFLICT_READ';
