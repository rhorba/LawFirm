-- User permissions
INSERT INTO permissions (name, description, resource, action, created_at, updated_at, version) VALUES
('USER_READ', 'Read user information', 'USER', 'READ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('USER_CREATE', 'Create new users', 'USER', 'CREATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('USER_UPDATE', 'Update existing users', 'USER', 'UPDATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('USER_DELETE', 'Delete users', 'USER', 'DELETE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('USER_MANAGE', 'Full user management', 'USER', 'MANAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Role permissions
INSERT INTO permissions (name, description, resource, action, created_at, updated_at, version) VALUES
('ROLE_READ', 'Read role information', 'ROLE', 'READ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('ROLE_CREATE', 'Create new roles', 'ROLE', 'CREATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('ROLE_UPDATE', 'Update existing roles', 'ROLE', 'UPDATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('ROLE_DELETE', 'Delete roles', 'ROLE', 'DELETE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('ROLE_MANAGE', 'Full role management', 'ROLE', 'MANAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Permission permissions
INSERT INTO permissions (name, description, resource, action, created_at, updated_at, version) VALUES
('PERMISSION_READ', 'Read permission information', 'PERMISSION', 'READ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('PERMISSION_MANAGE', 'Full permission management', 'PERMISSION', 'MANAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- System permissions
INSERT INTO permissions (name, description, resource, action, created_at, updated_at, version) VALUES
('SYSTEM_MANAGE', 'Full system administration', 'SYSTEM', 'MANAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
