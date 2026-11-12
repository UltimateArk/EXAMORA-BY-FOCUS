-- V2__exams.sql — Exam scheduling tables (PostgreSQL)

CREATE TABLE IF NOT EXISTS exams (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  exam_type VARCHAR(20)
    CHECK (exam_type IN ('MAIN','ADVANCED','NSEJS','NSEP','IOQM','OTHER')),
  class_name VARCHAR(50),
  board VARCHAR(100),
  exam_date DATE,
  start_time TIME,
  end_time TIME,
  duration_minutes INT NOT NULL DEFAULT 0,
  fee DECIMAL(10, 2),
  reg_deadline TIMESTAMP,
  max_attempts INT NOT NULL DEFAULT 1,
  instructions TEXT,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
    CHECK (status IN ('DRAFT','SCHEDULED','PUBLISHED','COMPLETED','CANCELLED')),
  marks_correct DOUBLE PRECISION,
  negative_marks DOUBLE PRECISION,
  marks_unanswered DOUBLE PRECISION NOT NULL DEFAULT 0,
  result_date DATE,
  answer_review BOOLEAN NOT NULL DEFAULT FALSE,
  centres TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_exams_status ON exams(status);
CREATE INDEX IF NOT EXISTS idx_exams_date ON exams(exam_date);

CREATE TABLE IF NOT EXISTS exam_questions (
  id BIGSERIAL PRIMARY KEY,
  exam_id BIGINT NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
  question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
  question_order INT NOT NULL,
  marks_override DOUBLE PRECISION,
  snapshot_json TEXT,
  CONSTRAINT uq_exam_question UNIQUE (exam_id, question_id)
);
CREATE INDEX IF NOT EXISTS idx_exam_questions_exam ON exam_questions(exam_id);
