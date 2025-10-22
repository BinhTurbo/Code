INSERT INTO roles (code, name) VALUES
    ('ADMIN', 'Administrator')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO roles (code, name) VALUES
    ('USER',  'Normal User')
ON DUPLICATE KEY UPDATE name = VALUES(name);


INSERT INTO users (username, password_hash, full_name, role_id, status)
VALUES
    ('admin', '{bcrypt}$2b$10$5LiY8ay/IbZ7/BGI0id1..7UhxABrsxeoq8e53RaAJlhKJ2F/QWo6', 'System Admin',
     (SELECT id FROM roles WHERE code='ADMIN'), 'ACTIVE'),
    ('user1', '{bcrypt}$2b$10$uJmmYm.newqPyf.9RQTYcu6nQdc1plNoyQcAZ8q80EOe9aZdYoymC',  'Demo User',
     (SELECT id FROM roles WHERE code='USER'),  'ACTIVE');
