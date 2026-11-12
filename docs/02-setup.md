# EXMORA V2 — Local Setup (from zero to login)

Prereqs: everything in `01-dependencies.md` installed.

## 1. Database

```bash
brew services start postgresql@16
createdb examora
psql postgresql://localhost:5432/examora -f db/V1__init.sql
psql postgresql://localhost:5432/examora -f db/V2__exams.sql
psql postgresql://localhost:5432/examora -f db/V3__seed_test_admin.sql
psql postgresql://localhost:5432/examora -f db/V4__latex_base64.sql
```

## 2. Environment

Both Java servers share the JWT secret. Pick a 32+ byte value:

```bash
export JWT_SECRET='change-me-to-32-plus-random-bytes!!'
export DB_URL='jdbc:postgresql://localhost:5432/examora'
export DB_USER='<your-mac-username>'   # postgres peer default; empty password locally
export DB_PASS=''
export EXTRACTOR_URL='http://localhost:8001'
```

## 3. Boot order

```bash
# terminal 1 — extractor (Nougat, TEXT only)
cd extractor && NOUGAT_ENABLED=1 python3 app.py        # :8001

# terminal 2 — auth server
cd auth-server && mvn spring-boot:run                  # :8081

# terminal 3 — exam server
cd exam-server && mvn spring-boot:run                  # :8080

# terminal 4 — frontend
cd frontend && npm install && npm run dev              # :5173 (dev) — or npm run preview
```

Health checks: `:8001/health`, frontend `:5173`, login below proves `:8081`.

## 4. Test login (admin portal)

- URL: `http://127.0.0.1:5173/login` → **Admin** tab
- Email: `test@email.com` — Password: `12345678`
- Create more users: `POST :8081/api/auth/register`
  `{"fullName":"…","email":"…","mobile":"…","password":"…","role":"ADMIN|STUDENT|CBT_CENTRE_ADMIN"}`

## 5. First extraction (admin portal)

`/admin` → schedule exam → Continue to upload → upload PDF →
Extract text (Nougat, ~30–60s/page CPU) → Preview images (PDFBox) →
Save → Continue to review (one-by-one preview, edit, approve).

## 6. Dev shortcut (no Java/Postgres)

Running just the UI + login: `python3 dev-auth-server.py` (`:8081`, SQLite
`data/auth.db`, same API + JWT shape, same test login). Exam/extraction
features need the real stack above.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `mvn: command not found` | `brew install maven`, restart shell |
| Spring `role` insert fails | Apply `V1__init.sql` from this repo (allows `CBT_CENTRE_ADMIN`) |
| Extractor `EncoderDecoderCache` error | `pip install "transformers>=4.44,<5"` (never v5) |
| First `/extract` slow | Model downloading (~350MB) + CPU inference; later pages reuse cache |
| Frontend 401s | `JWT_SECRET` must match on both Java servers; token in localStorage |
| Port clash | `lsof -ti:PORT \| xargs kill` |
