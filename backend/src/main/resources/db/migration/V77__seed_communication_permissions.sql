INSERT INTO permissions (name, description, resource, action, created_at, updated_at, version)
VALUES
    ('COMMUNICATION_READ',   'View case communications',         'COMMUNICATION', 'READ',   NOW(), NOW(), 0),
    ('COMMUNICATION_CREATE', 'Log and send communications',      'COMMUNICATION', 'CREATE', NOW(), NOW(), 0),
    ('COMMUNICATION_DELETE', 'Delete communication entries',     'COMMUNICATION', 'DELETE', NOW(), NOW(), 0),
    ('COMMUNICATION_MANAGE', 'Full communication management',    'COMMUNICATION', 'MANAGE', NOW(), NOW(), 0);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('COMMUNICATION_READ','COMMUNICATION_CREATE','COMMUNICATION_DELETE','COMMUNICATION_MANAGE');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'MODERATOR'
  AND p.name IN ('COMMUNICATION_READ','COMMUNICATION_CREATE');
