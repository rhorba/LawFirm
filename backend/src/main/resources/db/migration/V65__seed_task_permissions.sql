-- V65__seed_task_permissions.sql
INSERT INTO permissions (name, description, resource, action, created_at, updated_at, version) VALUES
  ('TASK_READ',   'View tasks and deadlines',          'TASK', 'READ',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  ('TASK_CREATE', 'Create tasks and deadlines',        'TASK', 'CREATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  ('TASK_UPDATE', 'Update task details and status',    'TASK', 'UPDATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  ('TASK_DELETE', 'Delete tasks',                      'TASK', 'DELETE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  ('TASK_MANAGE', 'Full task management across cases', 'TASK', 'MANAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ADMIN: all task permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('TASK_READ','TASK_CREATE','TASK_UPDATE','TASK_DELETE','TASK_MANAGE');

-- MODERATOR: read-only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'MODERATOR'
  AND p.name = 'TASK_READ';
