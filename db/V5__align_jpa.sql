-- V5__align_jpa.sql — align live DB with the JPA entities.
-- Context: shared V1 created UUID/INT exam tables, but exam-server entities are
-- BIGINT-shaped (see exam-server/src/main/resources/db/). Auth User entity also
-- needs columns V1 users lacks. All exam-side tables are empty pre-launch, so
-- drop + recreate in canonical JPA shape. users rows are preserved.

-- ---- 1. users: columns required by com.examora.auth.entity.User ----
ALTER TABLE users ADD COLUMN IF NOT EXISTS mobile    TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS class_name TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS institute TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS board     TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS city      TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS state     TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS country   TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS active    BOOLEAN NOT NULL DEFAULT TRUE;

-- ---- 2. drop dead + UUID-shaped exam tables (empty pre-launch) ----
DROP TABLE IF EXISTS exam_question   CASCADE;  -- dead singular table from V1
DROP TABLE IF EXISTS exam_questions  CASCADE;  -- UUID question_id FK
DROP TABLE IF EXISTS question_options CASCADE;
DROP TABLE IF EXISTS questions       CASCADE;
DROP TABLE IF EXISTS question_papers CASCADE;
DROP TABLE IF EXISTS topics          CASCADE;
DROP TABLE IF EXISTS subjects        CASCADE;
-- exams (BIGSERIAL, JPA-shaped) is kept as-is.

-- ---- 3. recreate canonical BIGINT shape (mirrors exam-server resources db/) ----
CREATE TABLE IF NOT EXISTS subjects (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS topics (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  syllabus_type VARCHAR(50),
  subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
  CONSTRAINT uq_topic_subject_name UNIQUE (subject_id, name)
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
  latex TEXT,
  latex_json TEXT,
  question_type VARCHAR(20) DEFAULT 'MCQ_SINGLE'
    CHECK (question_type IN ('MCQ_SINGLE','MCQ_MULTI','INTEGER','NUMERIC')),
  subject_id BIGINT REFERENCES subjects(id),
  topic_id BIGINT REFERENCES topics(id),
  difficulty VARCHAR(10) DEFAULT 'MEDIUM'
    CHECK (difficulty IN ('EASY','MEDIUM','HARD')),
  exam_type VARCHAR(50),
  image_path VARCHAR(2048),
  image_base64 TEXT,
  diagram_path VARCHAR(2048),
  diagram_base64 TEXT,
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
  option_latex TEXT,
  option_image_path VARCHAR(2048),
  correct BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_options_question ON question_options(question_id);

CREATE TABLE IF NOT EXISTS exam_questions (
  id BIGSERIAL PRIMARY KEY,
  exam_id BIGINT NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
  question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
  question_order INT NOT NULL,
  marks_override DOUBLE PRECISION,
  snapshot_json TEXT,
  CONSTRAINT uq_exam_question UNIQUE (exam_id, question_id)
);
CREATE INDEX IF NOT EXISTS idx_exam_questions_exam ON exam_questions(exam_id);

-- ---- 4. seeds (fixed ids; frontend resolves subjects by name) ----
INSERT INTO subjects (id, name) VALUES
  (1, 'Physics'), (2, 'Chemistry'), (3, 'Maths')
ON CONFLICT (id) DO NOTHING;
SELECT setval('subjects_id_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM subjects), 3));

INSERT INTO topics (subject_id, name, syllabus_type) VALUES
  (1, 'Kinematics', 'JEE'), (1, 'Laws of Motion', 'JEE'), (1, 'Electrostatics', 'JEE'),
  (2, 'Chemical Bonding', 'JEE'), (2, 'Thermodynamics', 'JEE'), (2, 'Organic - GOC', 'JEE'),
  (3, 'Quadratic Equations', 'JEE'), (3, 'Probability', 'JEE'), (3, 'Definite Integration', 'JEE')
ON CONFLICT (subject_id, name) DO NOTHING;
