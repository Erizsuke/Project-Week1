-- IMPORTANT: Replace the password_hash value below before running this migration.
-- Generate a BCrypt hash by running the BCryptHashGenerator class in src/test/java
-- (see README.md "Seed Admin Account" section), then paste the result here.
-- Never commit a real production hash to a public repository.

INSERT INTO users (id, email, password_hash, full_name, system_role, created_at)
VALUES (
    gen_random_uuid(),
    'admin@yourcompany.com',
    '$2a$10$REPLACE_WITH_YOUR_BCRYPT_HASH',
    'System Administrator',
    'ADMIN',
    NOW()
);
