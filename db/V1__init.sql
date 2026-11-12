-- V1__init.sql — shared PostgreSQL DDL for auth-server + exam-server
-- Covers: users (auth), student_profiles, subjects, topics,
-- question_papers, questions (MCQ + INTEGER/NUMERIC), question_options,
-- exam_question (paper <-> question reuse).

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============ USERS (auth-server owns writes) ============
CREATE TABLE users (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email         VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  full_name     VARCHAR(255),
  role          VARCHAR(32) NOT NULL DEFAULT 'STUDENT'
                CHECK (role IN ('STUDENT','ADMIN','CBT_CENTRE_ADMIN')),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE student_profiles (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
  target_exam VARCHAR(64) DEFAULT 'JEE',
  class_level VARCHAR(32),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============ TAXONOMY ============
CREATE TABLE subjects (
  id         SERIAL PRIMARY KEY,
  code       VARCHAR(32) NOT NULL UNIQUE,   -- PHYSICS | CHEMISTRY | MATHS
  name       VARCHAR(128) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE topics (
  id         SERIAL PRIMARY KEY,
  subject_id INT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
  name       VARCHAR(255) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (subject_id, name)
);

-- ============ PAPERS ============
CREATE TABLE question_papers (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title        VARCHAR(255) NOT NULL,
  exam_type    VARCHAR(64) NOT NULL DEFAULT 'JEE_MAIN',  -- JEE_MAIN | JEE_ADV | MOCK
  year         INT,
  source_pdf   VARCHAR(1024),
  uploaded_by  UUID REFERENCES users(id) ON DELETE SET NULL,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============ QUESTIONS ============
-- Supports BOTH:
--   MCQ_SINGLE / MCQ_MULTI -> rows in question_options with is_correct
--   INTEGER               -> NO option rows, correct_integer_answer set
--   NUMERIC               -> NO option rows, correct_numeric_answer + tolerance set
CREATE TABLE questions (
  id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  paper_id               UUID REFERENCES question_papers(id) ON DELETE SET NULL,
  subject_id             INT REFERENCES subjects(id) ON DELETE SET NULL,
  topic_id               INT REFERENCES topics(id) ON DELETE SET NULL,
  question_number        INT,
  question_type          VARCHAR(16) NOT NULL DEFAULT 'MCQ_SINGLE'
                         CHECK (question_type IN ('MCQ_SINGLE','MCQ_MULTI','INTEGER','NUMERIC')),
  statement_text         TEXT NOT NULL DEFAULT '',
  statement_latex        TEXT,
  latex_json             JSONB,                       -- e.g. {"latex": "...", "display": true}
  image_path             VARCHAR(1024),               -- question figure (PDFBox crop, exam-server)
  diagram_path           VARCHAR(1024),               -- alias/extra diagram (PDFBox crop)
  explanation            TEXT,
  correct_integer_answer BIGINT,                      -- INTEGER type only
  correct_numeric_answer DOUBLE PRECISION,            -- NUMERIC type only
  tolerance              DOUBLE PRECISION,             -- NUMERIC grading tolerance
  verification           VARCHAR(16) NOT NULL DEFAULT 'PENDING'
                         CHECK (verification IN ('PENDING','APPROVED','REJECTED')),
  created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT chk_integer_answer CHECK (
    (question_type <> 'INTEGER') OR (correct_integer_answer IS NOT NULL)
  ),
  CONSTRAINT chk_numeric_answer CHECK (
    (question_type <> 'NUMERIC') OR (correct_numeric_answer IS NOT NULL)
  ),
  CONSTRAINT chk_mcq_no_direct_answer CHECK (
    (question_type NOT IN ('MCQ_SINGLE','MCQ_MULTI')) OR
    (correct_integer_answer IS NULL AND correct_numeric_answer IS NULL)
  )
);

CREATE TABLE question_options (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  question_id      UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
  label            VARCHAR(2) NOT NULL CHECK (label IN ('A','B','C','D','E')),
  option_text      TEXT NOT NULL DEFAULT '',
  option_image_path VARCHAR(1024),                    -- option figure (PDFBox crop)
  is_correct       BOOLEAN NOT NULL DEFAULT FALSE,
  sort_order       INT NOT NULL DEFAULT 0,
  UNIQUE (question_id, label)
);

-- ============ PAPER <-> QUESTION REUSE ============
-- A question can be reused across exams/papers (e.g. mock reuses PYQ).
CREATE TABLE exam_question (
  exam_label  VARCHAR(255) NOT NULL,                  -- e.g. 'MOCK-2026-01' or exam id string
  question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
  position    INT NOT NULL DEFAULT 0,
  PRIMARY KEY (exam_label, question_id)
);

-- ============ INDEXES ============
CREATE INDEX idx_questions_paper    ON questions(paper_id);
CREATE INDEX idx_questions_subject  ON questions(subject_id);
CREATE INDEX idx_questions_topic    ON questions(topic_id);
CREATE INDEX idx_questions_type     ON questions(question_type);
CREATE INDEX idx_questions_verify   ON questions(verification);
CREATE INDEX idx_options_question   ON question_options(question_id);
CREATE INDEX idx_topics_subject     ON topics(subject_id);
CREATE INDEX idx_examq_question     ON exam_question(question_id);

-- ============ SEED: subjects + topics ============
INSERT INTO subjects (code, name) VALUES
  ('PHYSICS','Physics'),
  ('CHEMISTRY','Chemistry'),
  ('MATHS','Mathematics')
ON CONFLICT (code) DO NOTHING;

INSERT INTO topics (subject_id, name)
SELECT s.id, t.name FROM subjects s
JOIN (VALUES
  ('PHYSICS','Kinematics'),
  ('PHYSICS','Laws of Motion'),
  ('PHYSICS','Electrostatics'),
  ('CHEMISTRY','Chemical Bonding'),
  ('CHEMISTRY','Thermodynamics'),
  ('CHEMISTRY','Organic - GOC'),
  ('MATHS','Quadratic Equations'),
  ('MATHS','Probability'),
  ('MATHS','Definite Integration')
) AS t(code, name) ON t.code = s.code
ON CONFLICT (subject_id, name) DO NOTHING;
