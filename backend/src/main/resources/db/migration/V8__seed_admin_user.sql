-- Password: admin123 (BCrypt hashed)
INSERT INTO users (username, email, password, enabled, account_non_expired, account_non_locked, credentials_non_expired, created_at, updated_at, version)
VALUES ('admin', 'admin@boilerplate.com', '$2a$10$WBaZbCmFrHMCFZZXL4y1XuHAVACE.WkXViZUIIukFcZufhWAt/6hu', TRUE, TRUE, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Assign ADMIN role to admin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.username = 'admin'
AND r.name = 'ADMIN';
