-- V67__seed_calendar_permissions.sql
INSERT INTO permissions (name, description, resource, action, created_at, updated_at, version) VALUES
  ('CALENDAR_READ',   'View calendar events',              'CALENDAR', 'READ',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  ('CALENDAR_CREATE', 'Create calendar events',             'CALENDAR', 'CREATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  ('CALENDAR_UPDATE', 'Update calendar events',             'CALENDAR', 'UPDATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  ('CALENDAR_DELETE', 'Delete calendar events',             'CALENDAR', 'DELETE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  ('CALENDAR_MANAGE', 'Full calendar management all users', 'CALENDAR', 'MANAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- ADMIN: all calendar permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('CALENDAR_READ','CALENDAR_CREATE','CALENDAR_UPDATE','CALENDAR_DELETE','CALENDAR_MANAGE');

-- MODERATOR: read-only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'MODERATOR'
  AND p.name = 'CALENDAR_READ';
