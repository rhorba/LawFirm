-- Create shared Default Users group with USER role
INSERT INTO groups (name, description, created_at, updated_at, version)
VALUES ('Default Users', 'Default group for new users', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Assign USER role to Default Users group
INSERT INTO group_roles (group_id, role_id)
SELECT g.id, r.id
FROM groups g, roles r
WHERE g.name = 'Default Users' AND r.name = 'USER';
