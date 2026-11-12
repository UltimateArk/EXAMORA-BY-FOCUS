-- V1__init.sql — PostgreSQL DDL for exam-server
CREATE TABLE IF NOT EXISTS subjects (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS topics (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  syllabus_type VARCHAR(50),
  subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_topics_subject ON topics(subject_id);

CREATE TABLE IF NOT EXISTS question_papers (
  id BIGSERIAL PRIMARY KEY,
  file_name VARCHAR(1024) NOT NULL,
  stored_path VARCHAR(2048) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'UPLOADED'
    CHECK (status IN ('UPLOADED','EXTRACTING','EXTRACTED','FAILED')),
  error_log TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS questions (
  id BIGSERIAL PRIMARY KEY,
  paper_id BIGINT NOT NULL REFERENCES question_papers(id) ON DELETE CASCADE,
  question_number INT,
  page_number INT,
  question_text TEXT,
  markdown TEXT,
  latex_json TEXT,
  question_type VARCHAR(20) DEFAULT 'MCQ_SINGLE'
    CHECK (question_type IN ('MCQ_SINGLE','MCQ_MULTI','INTEGER','NUMERIC')),
  subject_id BIGINT REFERENCES subjects(id),
  topic_id BIGINT REFERENCES topics(id),
  difficulty VARCHAR(10) DEFAULT 'MEDIUM'
    CHECK (difficulty IN ('EASY','MEDIUM','HARD')),
  exam_type VARCHAR(50),
  image_path VARCHAR(2048),
  diagram_path VARCHAR(2048),
  correct_integer_answer BIGINT,
  correct_numeric_answer DOUBLE PRECISION,
  tolerance DOUBLE PRECISION DEFAULT 0,
  verification_status VARCHAR(10) NOT NULL DEFAULT 'PENDING'
    CHECK (verification_status IN ('PENDING','APPROVED','REJECTED')),
  explanation TEXT
);
CREATE INDEX IF NOT EXISTS idx_questions_paper ON questions(paper_id);
CREATE INDEX IF NOT EXISTS idx_questions_subject ON questions(subject_id);
CREATE INDEX IF NOT EXISTS idx_questions_topic ON questions(topic_id);
CREATE INDEX IF NOT EXISTS idx_questions_verify ON questions(verification_status);

CREATE TABLE IF NOT EXISTS question_options (
  id BIGSERIAL PRIMARY KEY,
  question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
  label VARCHAR(2) NOT NULL,
  option_text TEXT,
  option_image_path VARCHAR(2048),
  correct BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_options_question ON question_options(question_id);
