-- V7__subtopics.sql — PRD syllabus level Subject -> Topic -> Subtopic.
-- Mirrors shared db/V7__subtopics.sql (identical; verified by diff).
CREATE TABLE IF NOT EXISTS subtopics (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  topic_id BIGINT NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
  CONSTRAINT uq_subtopic_topic_name UNIQUE (topic_id, name)
);
CREATE INDEX IF NOT EXISTS idx_subtopics_topic ON subtopics(topic_id);

ALTER TABLE questions ADD COLUMN IF NOT EXISTS subtopic_id BIGINT REFERENCES subtopics(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_questions_subtopic ON questions(subtopic_id);
