-- Add read-only permissions to USER role for case management features
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'USER'
AND p.name IN (
    'CASE_READ',
    'LAWYER_READ',
    'TRIBUNAL_READ',
    'CASETYPE_READ'
);

-- Create test user with read-only access
-- Password: admin123 (same hash as admin user for simplicity)
INSERT INTO users (username, password, email, enabled, account_non_expired, account_non_locked, credentials_non_expired, created_at, updated_at, version)
VALUES (
    'user',
    '$2a$10$WBaZbCmFrHMCFZZXL4y1XuHAVACE.WkXViZUIIukFcZufhWAt/6hu',  -- admin123 (BCrypt)
    'user@lawfirm.ma',
    true,
    true,
    true,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);

-- Create personal group for the user
INSERT INTO groups (name, description, created_at, updated_at, version)
VALUES (
    'personal_user',
    'Personal group for user',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);

-- Assign USER role to the personal group
INSERT INTO group_roles (group_id, role_id)
SELECT g.id, r.id
FROM groups g
CROSS JOIN roles r
WHERE g.name = 'personal_user'
AND r.name = 'USER';

-- Add user to their personal group
INSERT INTO user_groups (user_id, group_id)
SELECT u.id, g.id
FROM users u
CROSS JOIN groups g
WHERE u.username = 'user'
AND g.name = 'personal_user';
