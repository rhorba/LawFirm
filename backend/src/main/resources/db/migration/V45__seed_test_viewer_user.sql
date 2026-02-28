-- V45: Seed a test user with CLIENT_READ only (for TC-12 permission testing)

-- 1. Create a minimal role for client read-only access
INSERT INTO roles (name, description, created_at, updated_at, version)
VALUES ('CLIENT_VIEWER', 'Read-only access to clients', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- 2. Assign only CLIENT_READ to that role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'CLIENT_VIEWER'
  AND p.name = 'CLIENT_READ';

-- 3. Create the test user (password: admin123, same BCrypt hash as admin)
INSERT INTO users (username, email, password, enabled, account_non_expired, account_non_locked, credentials_non_expired, created_at, updated_at, version)
VALUES ('viewer', 'viewer@lawfirm.ma', '$2a$10$WBaZbCmFrHMCFZZXL4y1XuHAVACE.WkXViZUIIukFcZufhWAt/6hu', TRUE, TRUE, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- 4. Create personal group for viewer
INSERT INTO groups (name, description, created_at, updated_at, version)
VALUES ('personal_viewer', 'Personal group for viewer', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- 5. Assign CLIENT_VIEWER role to the personal group
INSERT INTO group_roles (group_id, role_id)
SELECT g.id, r.id
FROM groups g
CROSS JOIN roles r
WHERE g.name = 'personal_viewer'
  AND r.name = 'CLIENT_VIEWER';

-- 6. Add viewer to their personal group
INSERT INTO user_groups (user_id, group_id)
SELECT u.id, g.id
FROM users u
CROSS JOIN groups g
WHERE u.username = 'viewer'
  AND g.name = 'personal_viewer';
