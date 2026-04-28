-- V55: Migrate role-based group permissions to direct group permissions.
-- After this migration, permission resolution goes exclusively through
-- user -> user_groups -> groups -> group_permissions -> permissions.
-- group_roles / role_permissions remain for UI convenience only.

INSERT INTO group_permissions (group_id, permission_id)
SELECT DISTINCT gr.group_id, rp.permission_id
FROM group_roles gr
JOIN role_permissions rp ON rp.role_id = gr.role_id
WHERE NOT EXISTS (
    SELECT 1 FROM group_permissions gp
    WHERE gp.group_id = gr.group_id AND gp.permission_id = rp.permission_id
);
