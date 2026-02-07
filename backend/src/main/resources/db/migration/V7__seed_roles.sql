-- Insert roles
INSERT INTO roles (name, description, created_at, updated_at, version) VALUES
('ADMIN', 'System administrator with full access', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('USER', 'Standard user with basic permissions', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('MODERATOR', 'Moderator with elevated permissions', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Assign all permissions to ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN';

-- Assign read permissions to USER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'USER'
AND p.name IN ('USER_READ', 'ROLE_READ', 'PERMISSION_READ');

-- Assign moderate permissions to MODERATOR role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'MODERATOR'
AND p.name IN ('USER_READ', 'USER_UPDATE', 'ROLE_READ', 'PERMISSION_READ');
