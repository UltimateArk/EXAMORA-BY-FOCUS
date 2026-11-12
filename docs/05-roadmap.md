# EXMORA V2 — Status & Roadmap

Last updated: 2026-11-12. Honest ledger of what runs vs what is scaffolded.

## Live and proven on this machine

- [x] Frontend (`:5173`) — admin portal builds, all flows navigable
- [x] Dev auth (`:8081`, `dev-auth-server.py`, SQLite) — test login works
- [x] Extractor (`:8001`, Nougat 4.57.6) — real markdown+LaTeX out, verified
- [x] DB migrations V1→V4 + test-admin seed (files; applied when Postgres exists)

## Written, not yet compiled/run

- [ ] `auth-server` (Spring) — needs `mvn compile` + Postgres
- [ ] `exam-server` (Spring) — needs `mvn compile` + Postgres
- [ ] End-to-end: upload → extract → save → review (blocked on above)
- [ ] Retire `dev-auth-server.py` once Spring auth is up

## Known gaps / next work

1. **Student + CBT centre portals** — login tabs exist, portals are "coming soon".
   Needs: registration/payment flow, access-code (30-min single-use) system,
   exam engine (palette, server timer, autosave), evaluation + rankings.
2. **Diagram detection** — `diagramBase64` column exists but no detector fills it;
   crops land in `imageBase64`. Options: contour/CV split in PDFBox step.
3. **Answer keys** — options save with `correct=false`; correct-option extraction
   from answer-key pages is heuristic-only. Admin sets correct answers in review.
4. **ExamQuestion snapshots** cover text+options; extend if schema grows.
5. **Production hardening** — secrets management, S3/file storage, Redis, rate
   limits, Flyway (migrations are manual `psql` files today), CI.
6. **Spring Boot 4.1.2** — Java was never compiled here; expect minor Boot 3→4
   API fixes on first `mvn compile` (most likely `SecurityConfig`).
