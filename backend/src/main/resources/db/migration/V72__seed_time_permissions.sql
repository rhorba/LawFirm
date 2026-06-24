INSERT INTO permissions (name, description, resource, action, created_at, updated_at, version) VALUES
    ('TIME_READ',   'View time entries and billing summaries',      'TIME', 'READ',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('TIME_CREATE', 'Log time entries on cases',                    'TIME', 'CREATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('TIME_UPDATE', 'Edit existing time entries',                   'TIME', 'UPDATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('TIME_DELETE', 'Delete time entries',                          'TIME', 'DELETE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('TIME_MANAGE', 'Mark entries as billed and manage all time data', 'TIME', 'MANAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('TIME_READ', 'TIME_CREATE', 'TIME_UPDATE', 'TIME_DELETE', 'TIME_MANAGE');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'MODERATOR'
  AND p.name = 'TIME_READ';
