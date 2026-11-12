-- V6__submit_status.sql — add SUBMITTED paper status (+ option figure storage).
-- The status CHECK was created inline, so PostgreSQL auto-named it
-- question_papers_status_check in both the exam-server V1 and the shared V5
-- DDL; the shared V1 has no status column at all. DROP IF EXISTS covers
-- every history, then the CHECK is re-added under an explicit name with
-- SUBMITTED included.
ALTER TABLE question_papers DROP CONSTRAINT IF EXISTS question_papers_status_check;
ALTER TABLE question_papers
  ADD CONSTRAINT question_papers_status_check
  CHECK (status IN ('UPLOADED','EXTRACTING','EXTRACTED','FAILED','SUBMITTED'));

-- Dual-stored option figures (mirrors question image_path + image_base64):
-- file path (option_image_path, pre-existing) + raw base64 TEXT (no prefix).
ALTER TABLE question_options ADD COLUMN IF NOT EXISTS option_image_base64 TEXT;
