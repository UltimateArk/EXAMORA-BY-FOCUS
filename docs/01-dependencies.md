# EXMORA V2 — Dependencies

Single checklist of everything needed to run the project locally (macOS ARM).
Versions pinned where a wrong version is known to break things.

## System

| Tool | Version used | Install |
|---|---|---|
| Homebrew | 4.x | `/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"` |
| Java (JDK) | 25 LTS (any 21+ works; Boot 4 needs 17+) | `brew install openjdk` |
| Maven | 3.9.x | `brew install maven` |
| PostgreSQL | 16 | `brew install postgresql@16` |
| Node.js | 25 (any 20+) | `brew install node` |
| Python | 3.14 (3.10+ works) | `brew install python` |
| Git + GitHub CLI | 2.x / 2.x | `brew install git gh` |

Verify: `java -version && mvn -version && psql --version && node --version && python3 --version`

## Frontend (`frontend/` — React 18 + Vite 5 + Redux Toolkit)

```bash
cd frontend && npm install
```

Key packages (`package.json`): `react`, `react-dom`, `react-router-dom@6`,
`@reduxjs/toolkit`, `react-redux`, `vite`, `@vitejs/plugin-react`.

## Auth + Exam servers (Java — Spring Boot 4.1.2)

No manual installs — Maven resolves everything from `pom.xml`:

- `spring-boot-starter-web / security / data-jpa / validation`
- `postgresql` (JDBC driver, runtime)
- `io.jsonwebtoken:jjwt-{api,impl,jackson}:0.12.6`
- exam-server only: `org.apache.pdfbox:pdfbox:2.0.32`, `spring-boot-starter-webflux`

```bash
cd auth-server && mvn compile
cd ../exam-server && mvn compile
```

## Extractor (`extractor/` — Python + Meta Nougat)

```bash
pip3 install -r extractor/requirements.txt
# requirements: fastapi, uvicorn, transformers>=4.44,<5 (!), torch,
#               pymupdf, pillow, python-multipart
```

Critical pins:
- **`transformers>=4.44,<5`** — v5 breaks Nougat `generate()`
  (`EncoderDecoderCache` has no `layers`). Do not upgrade past 4.x.
- torch CPU wheel is large (~800MB with deps). macOS ARM uses MPS automatically.
- Model `facebook/nougat-base` (~350MB) downloads from Hugging Face on first
  `/extract` call into `~/.cache/huggingface`.

Dev-auth stub extra (already installed here): `bcrypt`.

## Database

- PostgreSQL 16, database `examora`, migrations in `db/`:
  `V1__init.sql` → `V2__exams.sql` → `V3__seed_test_admin.sql` → `V4__latex_base64.sql`

## Ports

| Service | Port |
|---|---|
| frontend (vite preview) | 5173 |
| exam-server (Spring) | 8080 |
| auth-server (Spring) / dev-auth stub | 8081 |
| extractor (Nougat) | 8001 |
| postgres | 5432 |
