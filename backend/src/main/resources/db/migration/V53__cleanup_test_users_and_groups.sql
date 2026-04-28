-- V53: Remove test users and extra groups, keep only admin + personal_admin + Default Users

-- 1. Remove user_groups associations for test users
DELETE FROM user_groups WHERE user_id IN (
    SELECT id FROM users WHERE username IN ('user', 'viewer')
);

-- 2. Delete test users
DELETE FROM users WHERE username IN ('user', 'viewer');

-- 3. Remove group_roles for test personal groups
DELETE FROM group_roles WHERE group_id IN (
    SELECT id FROM groups WHERE name IN ('personal_user', 'personal_viewer')
);

-- 4. Delete test personal groups
DELETE FROM groups WHERE name IN ('personal_user', 'personal_viewer');

-- 5. Remove role_permissions for CLIENT_VIEWER role
DELETE FROM role_permissions WHERE role_id = (
    SELECT id FROM roles WHERE name = 'CLIENT_VIEWER'
);

-- 6. Delete CLIENT_VIEWER role
DELETE FROM roles WHERE name = 'CLIENT_VIEWER';
