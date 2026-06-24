INSERT INTO permissions (name, description, resource, action, created_at, updated_at, version)
VALUES
    ('DOCUMENT_READ',   'View case documents',               'DOCUMENT', 'READ',   NOW(), NOW(), 0),
    ('DOCUMENT_CREATE', 'Upload documents to cases',         'DOCUMENT', 'CREATE', NOW(), NOW(), 0),
    ('DOCUMENT_DELETE', 'Delete own documents',              'DOCUMENT', 'DELETE', NOW(), NOW(), 0),
    ('DOCUMENT_MANAGE', 'Manage all documents (any user)',   'DOCUMENT', 'MANAGE', NOW(), NOW(), 0);

-- ADMIN gets all four
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('DOCUMENT_READ','DOCUMENT_CREATE','DOCUMENT_DELETE','DOCUMENT_MANAGE');

-- MODERATOR gets read + create
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'MODERATOR'
  AND p.name IN ('DOCUMENT_READ','DOCUMENT_CREATE');
