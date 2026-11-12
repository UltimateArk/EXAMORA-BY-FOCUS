-- V4__latex_base64.sql — exam-server (JPA bigint DDL): canonical LaTeX + dual-stored images.
-- Images are dual-stored: file path (image_path) + raw base64 TEXT (no data: prefix).
ALTER TABLE questions ADD COLUMN IF NOT EXISTS latex TEXT;
ALTER TABLE questions ADD COLUMN IF NOT EXISTS image_base64 TEXT;
ALTER TABLE questions ADD COLUMN IF NOT EXISTS diagram_base64 TEXT;
ALTER TABLE question_options ADD COLUMN IF NOT EXISTS option_latex TEXT;
