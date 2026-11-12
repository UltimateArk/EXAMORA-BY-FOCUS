# Examora Auth Server

Spring Boot 3.5.6 (Java 21) — registration, login, JWT issuance/validation. Port **8081**.

## Prerequisites

- Java 21, Maven 3.9+
- PostgreSQL running locally

```bash
# default DB is examora_auth; for a single shared DB use examora via env
createdb examora_auth
```

## Run

```bash
cd auth-server

# PowerShell / cmd (Windows) — single shared DB example:
# set DB_URL=jdbc:postgresql://localhost:5432/examora

export DB_URL=jdbc:postgresql://localhost:5432/examora_auth
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=change-me-to-a-256-bit-minimum-secret-key-0123456789abcdef
export JWT_EXPIRATION_MS=86400000

mvn spring-boot:run
```

> `JWT_SECRET` must be ≥ 32 bytes (HS256) and **must be the same value** in `exam-server`.

## Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | public | Register (default role STUDENT), returns JWT |
| POST | `/api/auth/login` | public | Login, returns JWT |
| GET | `/api/auth/me` | Bearer JWT | Current user profile |
| GET | `/api/auth/validate?token=...` | public | Used by exam-server: returns `{valid, email, role, userId}` |

## curl examples

```bash
BASE=http://localhost:8081

# Register
curl -s -X POST $BASE/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"fullName":"Aarav Sharma","email":"aarav@example.com","password":"secret123","className":"10","board":"CBSE","city":"Delhi"}'

# Login
TOKEN=$(curl -s -X POST $BASE/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"aarav@example.com","password":"secret123"}' | python3 -c 'import sys,json; print(json.load(sys.stdin)["token"])')
echo "$TOKEN"

# Me
curl -s $BASE/api/auth/me -H "Authorization: Bearer $TOKEN"

# Validate (what exam-server calls)
curl -s "$BASE/api/auth/validate?token=$TOKEN"
```

## Env vars

| Var | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/examora_auth` | Set to `.../examora` for single-DB mode |
| `DB_USERNAME` | `postgres` | DB user |
| `DB_PASSWORD` | `postgres` | DB password |
| `JWT_SECRET` | dev fallback | Shared HS256 secret (same in exam-server) |
| `JWT_EXPIRATION_MS` | `86400000` | Token TTL (24h) |
