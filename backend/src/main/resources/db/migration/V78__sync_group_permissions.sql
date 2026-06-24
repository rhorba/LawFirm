-- V78: Sync permissions added after V55 into group_permissions.
-- V55 migrated role_permissions to group_permissions at that point-in-time.
-- Migrations V65-V77 added new permissions only to role_permissions.
-- This migration re-runs the same idempotent sync so every group that holds
-- a role automatically inherits any permissions added since V55.

INSERT INTO group_permissions (group_id, permission_id)
SELECT DISTINCT gr.group_id, rp.permission_id
FROM group_roles gr
JOIN role_permissions rp ON rp.role_id = gr.role_id
WHERE NOT EXISTS (
    SELECT 1 FROM group_permissions gp
    WHERE gp.group_id = gr.group_id AND gp.permission_id = rp.permission_id
);
