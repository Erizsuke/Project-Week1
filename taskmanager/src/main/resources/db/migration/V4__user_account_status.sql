ALTER TABLE users
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_users_active_role ON users(active, system_role);