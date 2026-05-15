INSERT INTO permissions (name, description) VALUES
    ('TIME_READ',   'View time entries and billing summaries'),
    ('TIME_CREATE', 'Log time entries on cases'),
    ('TIME_UPDATE', 'Edit existing time entries'),
    ('TIME_DELETE', 'Delete time entries'),
    ('TIME_MANAGE', 'Mark entries as billed and manage all time data');

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
