-- Fix test user password to use working BCrypt hash
-- Password is admin123 (same as admin user)
UPDATE users
SET password = '$2a$10$WBaZbCmFrHMCFZZXL4y1XuHAVACE.WkXViZUIIukFcZufhWAt/6hu'
WHERE username = 'user';
