-- V42: Seed client permissions
INSERT INTO permissions (name, description, resource, action, created_at, updated_at, version) VALUES
('CLIENT_READ',   'Can view clients',                  'CLIENT', 'READ',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('CLIENT_CREATE', 'Can create clients',                'CLIENT', 'CREATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('CLIENT_UPDATE', 'Can update clients',                'CLIENT', 'UPDATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('CLIENT_DELETE', 'Can deactivate clients',            'CLIENT', 'DELETE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('CLIENT_MANAGE', 'Full client management access',     'CLIENT', 'MANAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Assign all to ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('CLIENT_READ','CLIENT_CREATE','CLIENT_UPDATE','CLIENT_DELETE','CLIENT_MANAGE');
