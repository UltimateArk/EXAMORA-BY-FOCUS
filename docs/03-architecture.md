# EXMORA V2 — Architecture

```
                    +------------------+
                    |  React (Vite+JS)  |  Redux Toolkit + RTK Query
                    |      :5173       |  Admin portal (/admin), login (/login)
                    +--------+---------+
                             |  JWT (Bearer, HS256, shared JWT_SECRET)
              +--------------+--------------+
              |                             |
   +----------v----------+        +-----------v----------+
   | exam-server :8080   |        | auth-server :8081    |
   | Spring Boot 4.1.2   |        | Spring Boot 4.1.2    |
   | papers, questions,  |        | register/login/me    |
   | exams, PDFBox imgs  |        | users + roles        |
   +----------+----------+        +-----------+----------+
              |                               |
              | multipart pdf                 | same shared DB
              | POST /extract -> markdown     |
   +----------v----------+                    |
   | extractor :8001     |          +---------v----------+
   | FastAPI + Nougat    |          |   postgres :5432   |
   | TEXT ONLY, per page |          |   db `examora`     |
   +---------------------+          +--------------------+
```

## Extraction pipeline (the core)

1. `POST /api/admin/upload-pdf` — stores `storage/papers/{id}/{id}.pdf`, row `UPLOADED`.
2. `GET /api/admin/extract-text/{paperId}` — forwards PDF bytes to `:8001/extract`.
   Nougat renders each page at 300 DPI and returns **markdown with LaTeX**.
3. `GET /api/admin/extract-question-images/{paperId}` — PDFBox renders pages at
   150 DPI, slices question regions (`question-N.png`). Images only, no text.
4. `POST /api/admin/save-extracted-questions/{paperId}` — `ExtractionService`
   splits markdown into questions (Q-number headings, A–D options, `Answer:`
   integers), converts each stem/option to **LaTeX** (`LatexConverter`), attaches
   crop `N` as `imagePath` + **base64**, writes `questions` + `question_options`.
5. Admin reviews one-by-one (`GET /api/questions/paper/{paperId}` returns LaTeX +
   base64 inline), edits, `PATCH …/verify?status=APPROVED|REJECTED`.

## Storage conventions

- `Question.latex` — canonical LaTeX (`\(…\)`, `$$…$$`, `tabular`, `\textbf{}`…).
  `questionText` = readable stem, `markdown` = raw Nougat source.
- Images dual-stored: `imagePath` (`papers/{paperId}/question-N.png`) for direct
  serving + `imageBase64` (raw, no `data:` prefix) + `imageMime` for single-fetch
  render as `data:{mime};base64,…`. `diagramBase64` reserved (no detector yet).
- `ExamQuestion` freezes a JSON snapshot on attach so later bank edits never
  rewrite published exams.

## Auth model

Roles: `ADMIN`, `STUDENT`, `CBT_CENTRE_ADMIN`. Spring Security, stateless,
BCrypt hashes, 24h HS256 tokens (`sub=email`, `role` claim). `/api/exams/**`
requires `ADMIN`. Dev shortcut `dev-auth-server.py` mirrors this contract on
SQLite — kill it when Spring is up.
