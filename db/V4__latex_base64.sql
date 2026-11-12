-- V4__latex_base64.sql — shared PostgreSQL DDL (UUID keys, see db/V1__init.sql).
-- NOTE: differs from exam-server's V4: this questions table already has
-- statement_latex (canonical LaTeX), so only the base64 TEXT columns are added.
-- Images are dual-stored: file path (image_path) + raw base64 TEXT (no data: prefix).
ALTER TABLE questions ADD COLUMN IF NOT EXISTS image_base64 TEXT;
ALTER TABLE questions ADD COLUMN IF NOT EXISTS diagram_base64 TEXT;
ALTER TABLE question_options ADD COLUMN IF NOT EXISTS option_latex TEXT;
