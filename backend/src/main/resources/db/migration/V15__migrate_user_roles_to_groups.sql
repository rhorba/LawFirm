-- Create personal groups for each user
INSERT INTO groups (name, description, created_at, updated_at, version)
SELECT
    CONCAT('personal_', u.username) AS name,
    CONCAT('Personal group for ', u.username) AS description,
    CURRENT_TIMESTAMP AS created_at,
    CURRENT_TIMESTAMP AS updated_at,
    0 AS version
FROM users u
WHERE u.deleted_at IS NULL;

-- Link users to their personal groups
INSERT INTO user_groups (user_id, group_id)
SELECT
    u.id AS user_id,
    g.id AS group_id
FROM users u
INNER JOIN groups g ON g.name = CONCAT('personal_', u.username)
WHERE u.deleted_at IS NULL;

-- Copy user roles to group roles
INSERT INTO group_roles (group_id, role_id)
SELECT DISTINCT
    g.id AS group_id,
    ur.role_id
FROM user_roles ur
INNER JOIN users u ON ur.user_id = u.id
INNER JOIN groups g ON g.name = CONCAT('personal_', u.username)
WHERE u.deleted_at IS NULL;

-- Drop the old user_roles table
DROP TABLE user_roles;
