#!/usr/bin/env python3
"""EXAMORA dev auth server — DEV/LOCAL ONLY, not for production.

Why this exists: the real auth-server is Spring Boot + PostgreSQL, which
isn't runnable on this machine yet (no Maven/Postgres). This stub implements
the SAME contract (same port 8081, same routes, same JSON shapes, real
HS256 JWTs, real BCrypt hashes) backed by SQLite, so the React admin portal
login works end-to-end right now.

    JWT_SECRET=... python3 dev-auth-server.py   # port 8081, db ./data/auth.db

Swap-back plan: boot the Spring auth-server with the same JWT_SECRET and the
same user rows (see db/V3__seed_test_admin.sql) — the frontend needs no changes.
JWTs here use sub=email + role claim, verifiable by the Java jjwt code.

Endpoints:
  POST /api/auth/register  {fullName,email,mobile,password,role?}
  POST /api/auth/login     {email,password} -> {token,role,userId,email}
  GET  /api/auth/me        (Bearer) -> {userId,email,role,fullName}
"""

import base64
import hashlib
import hmac
import json
import os
import sqlite3
import time
import uuid
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse

import bcrypt

PORT = int(os.environ.get("AUTH_PORT", "8081"))
DB_PATH = os.environ.get("AUTH_DB", os.path.join(os.path.dirname(os.path.abspath(__file__)), "data", "auth.db"))
SECRET = os.environ.get("JWT_SECRET", "dev-secret-change-me-please-32bytes!")
TOKEN_TTL = 24 * 3600
ROLES = ("STUDENT", "ADMIN", "CBT_CENTRE_ADMIN")


def b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).decode().rstrip("=")


def b64url_decode(s: str) -> bytes:
    return base64.urlsafe_b64decode(s + "=" * (-len(s) % 4))


def make_jwt(email: str, role: str, user_id: str) -> str:
    header = b64url(json.dumps({"alg": "HS256", "typ": "JWT"}).encode())
    now = int(time.time())
    payload = b64url(json.dumps(
        {"sub": email, "role": role, "userId": user_id, "iat": now, "exp": now + TOKEN_TTL}
    ).encode())
    sig = b64url(hmac.new(SECRET.encode(), f"{header}.{payload}".encode(), hashlib.sha256).digest())
    return f"{header}.{payload}.{sig}"


def check_jwt(token: str):
    try:
        header, payload, sig = token.split(".")
        expect = b64url(hmac.new(SECRET.encode(), f"{header}.{payload}".encode(), hashlib.sha256).digest())
        if not hmac.compare_digest(expect, sig):
            return None
        claims = json.loads(b64url_decode(payload))
        if claims.get("exp", 0) < time.time():
            return None
        return claims
    except Exception:
        return None


def db() -> sqlite3.Connection:
    os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)
    con = sqlite3.connect(DB_PATH)
    con.row_factory = sqlite3.Row
    con.execute(
        """CREATE TABLE IF NOT EXISTS users (
             id TEXT PRIMARY KEY,
             email TEXT NOT NULL UNIQUE,
             password_hash TEXT NOT NULL,
             full_name TEXT,
             mobile TEXT,
             role TEXT NOT NULL DEFAULT 'STUDENT'
               CHECK (role IN ('STUDENT','ADMIN','CBT_CENTRE_ADMIN')),
             active INTEGER NOT NULL DEFAULT 1,
             created_at TEXT NOT NULL DEFAULT (datetime('now'))
           )"""
    )
    # Seed test admin: test@email.com / 12345678 (same hash as db/V3__seed_test_admin.sql)
    seed_hash = "$2b$10$UinKq7wwHvFt7q2QMqqb9.VAX2tiHjwpfUhrPkgsDN2FUDhtfeOcG"
    con.execute(
        """INSERT INTO users (id, email, password_hash, full_name, role)
           VALUES (?, ?, ?, ?, 'ADMIN')
           ON CONFLICT(email) DO UPDATE SET
             password_hash=excluded.password_hash, full_name=excluded.full_name,
             role='ADMIN', active=1""",
        (str(uuid.uuid4()), "test@email.com", seed_hash, "Test Admin"),
    )
    con.commit()
    return con


class Handler(BaseHTTPRequestHandler):
    server_version = "ExamoraDevAuth/0.1"

    def _cors(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")

    def _send(self, status: int, obj):
        body = json.dumps(obj).encode()
        self.send_response(status)
        self._cors()
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_OPTIONS(self):
        self.send_response(204)
        self._cors()
        self.end_headers()

    def _body(self):
        try:
            length = int(self.headers.get("Content-Length") or 0)
        except ValueError:
            length = 0
        if not length:
            return {}
        try:
            return json.loads(self.rfile.read(length) or b"{}")
        except Exception:
            return {}

    def do_POST(self):
        path = urlparse(self.path).path
        data = self._body()
        con = db()
        if path == "/api/auth/register":
            email = str(data.get("email", "")).strip().lower()
            password = str(data.get("password", ""))
            role = str(data.get("role", "STUDENT")).upper()
            if "@" not in email or len(password) < 8:
                return self._send(400, {"message": "Valid email and min 8-char password required."})
            if role not in ROLES:
                return self._send(400, {"message": f"Role must be one of {list(ROLES)}."})
            if con.execute("SELECT 1 FROM users WHERE email=?", (email,)).fetchone():
                return self._send(409, {"message": "Email already registered."})
            uid = str(uuid.uuid4())
            pw = bcrypt.hashpw(password.encode(), bcrypt.gensalt(rounds=10)).decode()
            con.execute(
                "INSERT INTO users (id, email, password_hash, full_name, mobile, role) VALUES (?,?,?,?,?,?)",
                (uid, email, pw, data.get("fullName"), data.get("mobile"), role),
            )
            con.commit()
            return self._send(201, {"token": make_jwt(email, role, uid), "role": role, "userId": uid, "email": email})
        if path == "/api/auth/login":
            email = str(data.get("email", "")).strip().lower()
            password = str(data.get("password", ""))
            row = con.execute("SELECT * FROM users WHERE email=?", (email,)).fetchone()
            if not row or not bcrypt.checkpw(password.encode(), row["password_hash"].encode()):
                return self._send(401, {"message": "Invalid email or password."})
            if not row["active"]:
                return self._send(403, {"message": "Account deactivated."})
            return self._send(200, {
                "token": make_jwt(row["email"], row["role"], row["id"]),
                "role": row["role"], "userId": row["id"], "email": row["email"],
            })
        return self._send(404, {"message": "Not found."})

    def do_GET(self):
        path = urlparse(self.path).path
        if path == "/api/auth/me":
            auth = self.headers.get("Authorization", "")
            claims = check_jwt(auth[7:] if auth.startswith("Bearer ") else "")
            if not claims:
                return self._send(401, {"message": "Missing or invalid token."})
            row = db().execute("SELECT * FROM users WHERE email=?", (claims["sub"],)).fetchone()
            if not row:
                return self._send(401, {"message": "User not found."})
            return self._send(200, {
                "userId": row["id"], "email": row["email"],
                "role": row["role"], "fullName": row["full_name"],
            })
        return self._send(404, {"message": "Not found."})

    def log_message(self, fmt, *args):
        print(f"[auth] {self.command} {self.path} -> {fmt % args}")


if __name__ == "__main__":
    db().close()  # ensure schema + seed exist
    print(f"[auth] SQLite: {DB_PATH}")
    print(f"[auth] Listening on http://127.0.0.1:{PORT} (DEV ONLY)")
    HTTPServer(("127.0.0.1", PORT), Handler).serve_forever()
