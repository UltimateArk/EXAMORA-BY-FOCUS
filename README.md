# EXAMORA-BY-FOCUS
This is the repository of the codebase of EXAMORA, a one stop competitive mock examination platform.
# Examora V2

Microservices split of the V1 monolith. Image extraction stays in Java (PDFBox);
the Python service does **Nougat TEXT extraction only**.

## Architecture

```
                        +------------------+
                        |      React       |
                        |  (Vite + JS)      |
                        +--------+---------+
                                 |
                 +---------------+----------------+
                 |                                |
        +--------v---------+            +---------v--------+
        |   exam-server    |            |   auth-server    |
        |    Java :8080    |            |   Java :8081     |
        | PDFBox (images)  |            | users + JWT      |
        +--------+---------+            +--------+---------+
                 |                               |
                 | Nougat TEXT only              | shared DB
                 | POST pdf -> markdown          |
        +--------v---------+                     |
        |   extractor      |            +--------v--------+
        | Python FastAPI   |            |   postgres:16   |
        |     :8001        |            |  db `examora`   |
        +------------------+            +-----------------+

JWT: exam-server and auth-server share the same secret
(`JWT_SECRET` env) so exam-server can validate tokens issued at :8081.
```

- `exam-server :8080` — questions, papers, options, PDFBox image/diagram crops.
- `auth-server :8081` — register/login, users, student_profiles, issues JWT.
- `extractor :8001` — Nougat markdown per page (TEXT only, stub fallback).
- `postgres :5432` — single shared DB (`examora` / `examora` / `examora`).

## Quickstart

```bash
# 1. Database
docker compose up -d postgres
# applies db/V1__init.sql automatically on first start

# 2. Extractor (TEXT only; stub mode works with no model)
cd extractor
pip install -r requirements.txt
python app.py            # stub mode (NOUGAT_ENABLED unset)
# full mode:
NOUGAT_ENABLED=1 python app.py

# 3. auth-server (Java, :8081)
cd auth-server
./mvnw spring-boot:run      # needs JWT_SECRET + DATABASE_URL (postgres)

# 4. exam-server (Java, :8080)
cd ../exam-server
./mvnw spring-boot:run      # needs JWT_SECRET + DATABASE_URL + EXTRACTOR_URL=http://localhost:8001
```

Flow for PDF ingest: `exam-server` sends the PDF to `extractor :8001 /extract`
for Nougat markdown, then does its own PDFBox crops for `image_path /
diagram_path / option_image_path`. If the extractor returns `status:stub`,
exam-server proceeds with PDFBox text.

## API (V1-compatible)

| Server | Method + Path | Auth | Description |
|---|---|---|---|
| auth | `POST /api/auth/register` | no | Create user + student profile |
| auth | `POST /api/auth/login` | no | Returns JWT |
| auth | `GET /api/users/me` | JWT | Current user |
| exam | `GET /api/questions?subject=&topic=&type=` | JWT | List/filter questions |
| exam | `GET /api/questions/{id}` | JWT | Question + options |
| exam | `POST /api/papers/upload` (multipart pdf) | JWT | Nougat text via extractor + PDFBox images |
| exam | `POST /api/questions` | JWT | Create question (verification=PENDING) |
| exam | `PATCH /api/questions/{id}/verify` | ADMIN | APPROVED / REJECTED |
| extractor | `GET /health` | no | Service + model status |
| extractor | `POST /extract` (multipart `pdf` + `paperId`) | no | `{pages:[{pageNumber, markdown}]}` or stub |

## DB formats

### MCQ (options rows carry the answer)

```sql
INSERT INTO questions (subject_id, topic_id, question_type, statement_text, verification)
VALUES (1, 1, 'MCQ_SINGLE', 'A ball is thrown...', 'PENDING')
RETURNING id;  -- <qid>

INSERT INTO question_options (question_id, label, option_text, is_correct, sort_order) VALUES
  ('<qid>', 'A', '9.8 m/s^2', false, 0),
  ('<qid>', 'B', '10 m/s^2',  true,  1),
  ('<qid>', 'C', '0 m/s^2',   false, 2),
  ('<qid>', 'D', '5 m/s^2',   false, 3);
```

```json
{
  "id": "<qid>",
  "question_type": "MCQ_SINGLE",
  "statement_text": "A ball is thrown...",
  "verification": "PENDING",
  "options": [
    {"label": "A", "option_text": "9.8 m/s^2", "is_correct": false},
    {"label": "B", "option_text": "10 m/s^2",  "is_correct": true}
  ]
}
```

### INTEGER (no options; answer on the question row)

```sql
INSERT INTO questions (subject_id, topic_id, question_type, statement_text,
                       correct_integer_answer, verification)
VALUES (3, 7, 'INTEGER', 'Find the sum of roots of x^2 - 5x + 6 = 0.', 5, 'PENDING');
```

```json
{
  "question_type": "INTEGER",
  "statement_text": "Find the sum of roots of x^2 - 5x + 6 = 0.",
  "correct_integer_answer": 5,
  "options": []
}
```

### NUMERIC (float + tolerance)

```json
{
  "question_type": "NUMERIC",
  "statement_text": "Value of g to two decimals...",
  "correct_numeric_answer": 9.81,
  "tolerance": 0.01,
  "options": []
}
```
