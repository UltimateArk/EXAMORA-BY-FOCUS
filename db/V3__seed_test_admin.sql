-- V3: test admin login (local/dev only — NOT for production).
-- Email:    test@email.com
-- Password: 12345678
-- Role:     ADMIN (can enter the admin portal)
-- Apply after V1 + V2:  psql $DB_URL -f V3__seed_test_admin.sql

INSERT INTO users (email, password_hash, full_name, role)
VALUES (
  'test@email.com',
  '$2b$10$UinKq7wwHvFt7q2QMqqb9.VAX2tiHjwpfUhrPkgsDN2FUDhtfeOcG',
  'Test Admin',
  'ADMIN'
)
ON CONFLICT (email) DO UPDATE
  SET password_hash = EXCLUDED.password_hash,
      full_name     = EXCLUDED.full_name,
      role          = EXCLUDED.role,
      updated_at    = now();
