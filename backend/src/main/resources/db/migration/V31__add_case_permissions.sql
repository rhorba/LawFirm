-- Case permissions
INSERT INTO permissions (name, description, resource, action, created_at, updated_at, version) VALUES
('CASE_READ', 'Can view cases', 'CASE', 'READ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('CASE_CREATE', 'Can create cases', 'CASE', 'CREATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('CASE_UPDATE', 'Can update cases', 'CASE', 'UPDATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('CASE_DELETE', 'Can delete cases', 'CASE', 'DELETE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('CASE_MANAGE', 'Full case management access', 'CASE', 'MANAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Lawyer permissions
INSERT INTO permissions (name, description, resource, action, created_at, updated_at, version) VALUES
('LAWYER_READ', 'Can view lawyers', 'LAWYER', 'READ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('LAWYER_CREATE', 'Can create lawyers', 'LAWYER', 'CREATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('LAWYER_UPDATE', 'Can update lawyers', 'LAWYER', 'UPDATE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('LAWYER_DELETE', 'Can delete lawyers', 'LAWYER', 'DELETE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('LAWYER_MANAGE', 'Full lawyer management access', 'LAWYER', 'MANAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Tribunal permissions
INSERT INTO permissions (name, description, resource, action, created_at, updated_at, version) VALUES
('TRIBUNAL_READ', 'Can view tribunals', 'TRIBUNAL', 'READ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('TRIBUNAL_MANAGE', 'Can manage tribunal reference data', 'TRIBUNAL', 'MANAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Case Type permissions
INSERT INTO permissions (name, description, resource, action, created_at, updated_at, version) VALUES
('CASETYPE_READ', 'Can view case types', 'CASETYPE', 'READ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('CASETYPE_MANAGE', 'Can manage case type reference data', 'CASETYPE', 'MANAGE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Assign to ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
AND p.name IN (
    'CASE_READ', 'CASE_CREATE', 'CASE_UPDATE', 'CASE_DELETE', 'CASE_MANAGE',
    'LAWYER_READ', 'LAWYER_CREATE', 'LAWYER_UPDATE', 'LAWYER_DELETE', 'LAWYER_MANAGE',
    'TRIBUNAL_READ', 'TRIBUNAL_MANAGE',
    'CASETYPE_READ', 'CASETYPE_MANAGE'
);
