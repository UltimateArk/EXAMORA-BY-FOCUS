#!/usr/bin/env python3
"""EXMORA schedule server — SQLite-backed exam scheduling (port 8082).

Replaces the exam-server /api/exams endpoints for the admin portal.
Stdlib only (http.server + sqlite3).

    JWT_SECRET=... python3 schedule-server.py   # port 8082, db ./data/scheduled_tests.db

Auth: Bearer HS256 JWT (shared JWT_SECRET, checks exp; accepts `role`
(singular string, as issued by auth-server) or `roles` (list/string)).
Reads (GET) require any valid token; writes (POST/PUT/publish/attach)
require ADMIN. 401/403 JSON errors.

Routes (JSON shapes mirror exam-server ExamResponse):
  POST /api/exams                201 (validates name, end>start, duration>0)
  GET  /api/exams[?status=]      list
  GET  /api/exams/upcoming       SCHEDULED/PUBLISHED, future, sorted
  GET  /api/exams/{id}           one
  PUT  /api/exams/{id}           DRAFT/SCHEDULED only else 409
  POST /api/exams/{id}/publish   ->PUBLISHED else 409
  POST /api/exams/{id}/papers    {paperId} -> {examId, added}
  GET  /api/exams/{id}/questions concatenated exam-server paper questions
                                 (exam-server unreachable -> 502)

Field aliases: the React admin form posts frontend-style keys
(class/date/durationMin/...); exam-server clients post ExamRequest keys
(className/examDate/durationMinutes/...). Both are accepted; responses
carry both spellings so existing components keep working.

Env: SCHEDULE_PORT (default 8082), SCHEDULE_DB (default
./data/scheduled_tests.db), JWT_SECRET, EXAM_API (default
http://localhost:8080, used only by the questions proxy).
"""

import base64
import hashlib
import hmac
import json
import os
import re
import sqlite3
import time
import urllib.request
import urllib.error
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse, parse_qs

PORT = int(os.environ.get("SCHEDULE_PORT", "8082"))
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DB_PATH = os.environ.get(
    "SCHEDULE_DB", os.path.join(BASE_DIR, "data", "scheduled_tests.db"))
SECRET = os.environ.get("JWT_SECRET", "dev-secret-change-me-please-32bytes!")
EXAM_API = os.environ.get("EXAM_API", "http://localhost:8080").rstrip("/")

STATUSES = ("DRAFT", "SCHEDULED", "PUBLISHED", "COMPLETED", "CANCELLED")
ALLOWED_ORIGINS = ("http://localhost:5173", "http://127.0.0.1:5173")


def b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).decode().rstrip("=")


def b64url_decode(s: str) -> bytes:
    return base64.urlsafe_b64decode(s + "=" * (-len(s) % 4))


def make_jwt(sub: str, role: str, ttl: int = 120) -> str:
    header = b64url(json.dumps({"alg": "HS256", "typ": "JWT"}).encode())
    now = int(time.time())
    payload = b64url(json.dumps(
        {"sub": sub, "role": role, "iat": now, "exp": now + ttl}).encode())
    sig = b64url(hmac.new(SECRET.encode(),
                           f"{header}.{payload}".encode(),
                           hashlib.sha256).digest())
    return f"{header}.{payload}.{sig}"


def check_jwt(token: str):
    try:
        header, payload, sig = token.split(".")
        expect = b64url(hmac.new(SECRET.encode(),
                                 f"{header}.{payload}".encode(),
                                 hashlib.sha256).digest())
        if not hmac.compare_digest(expect, sig):
            return None
        claims = json.loads(b64url_decode(payload))
        if claims.get("exp", 0) < time.time():
            return None
        return claims
    except Exception:
        return None


def roles_of(claims: dict) -> set:
    """Accept `role` (singular string, auth-server style) and `roles`."""
    out = set()
    single = claims.get("role")
    if isinstance(single, str) and single.strip():
        out.add(single.strip().upper())
    multi = claims.get("roles")
    if isinstance(multi, (list, tuple)):
        out.update(str(r).strip().upper() for r in multi if str(r).strip())
    elif isinstance(multi, str) and multi.strip():
        out.update(t.strip().upper()
                   for t in re.split(r"[,\s]+", multi) if t.strip())
    return out


SCHEMA = """CREATE TABLE IF NOT EXISTS scheduled_tests (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  exam_type TEXT,
  class_name TEXT,
  board TEXT,
  exam_date TEXT,
  start_time TEXT,
  end_time TEXT,
  duration_minutes INT DEFAULT 0,
  fee REAL,
  reg_deadline TEXT,
  max_attempts INT DEFAULT 1,
  instructions TEXT,
  status TEXT DEFAULT 'DRAFT'
    CHECK (status IN ('DRAFT','SCHEDULED','PUBLISHED','COMPLETED','CANCELLED')),
  marks_correct REAL,
  negative_marks REAL,
  marks_unanswered REAL DEFAULT 0,
  result_date TEXT,
  answer_review INT DEFAULT 0,
  centres TEXT,
  paper_ids TEXT DEFAULT '',
  created_at TEXT DEFAULT (datetime('now'))
)"""


def db() -> sqlite3.Connection:
    os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)
    con = sqlite3.connect(DB_PATH)
    con.row_factory = sqlite3.Row
    con.execute(SCHEMA)
    con.commit()
    return con


def _num(v, default=None):
    if v is None or v == "":
        return default
    try:
        return float(v)
    except (TypeError, ValueError):
        return default


def _int(v, default=0):
    n = _num(v, None)
    return default if n is None else int(n)


def _bool(v) -> int:
    if isinstance(v, bool):
        return 1 if v else 0
    if isinstance(v, (int, float)):
        return 1 if v else 0
    return 1 if str(v).strip().lower() in ("1", "true", "yes", "on") else 0


def _str(v):
    if v is None:
        return None
    s = str(v).strip()
    return s if s != "" else None


def normalize(body: dict) -> dict:
    """Accept frontend-style and ExamRequest-style keys -> DB columns."""
    g = lambda *ks: next((body[k] for k in ks if body.get(k) not in (None, "")), None)
    cls = g("className", "class_name", "class")
    date = g("examDate", "exam_date", "date")
    dur = g("durationMinutes", "duration_minutes", "durationMin", "duration_min")
    return {
        "name": _str(body.get("name")),
        "exam_type": _str(g("examType", "exam_type")),
        "class_name": _str(cls),
        "board": _str(body.get("board")),
        "exam_date": _str(date),
        "start_time": _str(g("startTime", "start_time")),
        "end_time": _str(g("endTime", "end_time")),
        "duration_minutes": _int(dur, 0),
        "fee": _num(body.get("fee")),
        "reg_deadline": _str(g("regDeadline", "reg_deadline")),
        "max_attempts": _int(g("maxAttempts", "max_attempts"), 1) or 1,
        "instructions": _str(body.get("instructions")),
        "status": (str(body.get("status") or "DRAFT").strip().upper()),
        "marks_correct": _num(g("marksCorrect", "marks_correct")),
        "negative_marks": _num(g("negativeMarks", "negative_marks")),
        "marks_unanswered": _num(g("marksUnanswered", "marks_unanswered"), 0) or 0,
        "result_date": _str(g("resultDate", "result_date")),
        "answer_review": _bool(g("answerReview", "answer_review")),
        "centres": _str(body.get("centres")),
    }


def to_json(row: sqlite3.Row) -> dict:
    pids = [p for p in (row["paper_ids"] or "").split(",") if p.strip()]
    first = pids[0] if pids else None
    return {
        "id": row["id"],
        "name": row["name"],
        "examType": row["exam_type"],
        "className": row["class_name"],
        "class": row["class_name"],          # frontend alias
        "board": row["board"],
        "examDate": row["exam_date"],
        "date": row["exam_date"],            # frontend alias
        "startTime": row["start_time"],
        "endTime": row["end_time"],
        "durationMinutes": row["duration_minutes"],
        "durationMin": row["duration_minutes"],  # frontend alias
        "fee": row["fee"],
        "regDeadline": row["reg_deadline"],
        "maxAttempts": row["max_attempts"],
        "instructions": row["instructions"],
        "status": row["status"],
        "marksCorrect": row["marks_correct"],
        "negativeMarks": row["negative_marks"],
        "marksUnanswered": row["marks_unanswered"],
        "resultDate": row["result_date"],
        "answerReview": bool(row["answer_review"]),
        "centres": row["centres"],
        "paperIds": pids,
        "paperId": first,
        "papers": [{"id": p, "paperId": p} for p in pids],
        "createdAt": row["created_at"],
    }


COLUMNS = ("name", "exam_type", "class_name", "board", "exam_date",
           "start_time", "end_time", "duration_minutes", "fee",
           "reg_deadline", "max_attempts", "instructions", "status",
           "marks_correct", "negative_marks", "marks_unanswered",
           "result_date", "answer_review", "centres")


def validate(norm: dict):
    """Mirror ExamService rules. Returns an error message or None."""
    if not norm["name"]:
        return "name must not be blank"
    st, en = norm["start_time"], norm["end_time"]
    if st and en and not en > st:
        return "endTime must be after startTime"
    if (norm["duration_minutes"] or 0) <= 0:
        return "durationMinutes must be > 0"
    if norm["status"] not in STATUSES:
        return f"status must be one of {list(STATUSES)}"
    return None


class Handler(BaseHTTPRequestHandler):
    server_version = "ExamoraSchedule/0.1"

    def _cors(self):
        origin = self.headers.get("Origin", "")
        allow = origin if origin in ALLOWED_ORIGINS else "*"
        self.send_header("Access-Control-Allow-Origin", allow)
        self.send_header("Vary", "Origin")
        self.send_header("Access-Control-Allow-Headers",
                         "Content-Type, Authorization")
        self.send_header("Access-Control-Allow-Methods",
                         "GET, POST, PUT, OPTIONS")

    def _send(self, status: int, obj):
        body = json.dumps(obj).encode()
        self.send_response(status)
        self._cors()
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _err(self, status: int, message: str):
        self._send(status, {"message": message})

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

    def _auth(self, admin: bool = False):
        """Returns claims, or sends 401/403 and returns None."""
        auth = self.headers.get("Authorization", "")
        claims = check_jwt(auth[7:] if auth.startswith("Bearer ") else "")
        if not claims:
            self._err(401, "Missing or invalid token.")
            return None
        if admin and "ADMIN" not in roles_of(claims):
            self._err(403, "ADMIN role required.")
            return None
        return claims

    def _get_exam(self, con, eid: str):
        try:
            iid = int(eid)
        except (TypeError, ValueError):
            return None
        return con.execute(
            "SELECT * FROM scheduled_tests WHERE id=?", (iid,)).fetchone()

    # ---- reads (any valid token) ----
    def do_GET(self):
        if self._auth() is None:
            return
        parsed = urlparse(self.path)
        path, qs = parsed.path, parse_qs(parsed.query)
        con = db()
        if path == "/api/exams":
            status = (qs.get("status", [None])[0] or "").strip().upper()
            if status:
                rows = con.execute(
                    "SELECT * FROM scheduled_tests WHERE status=? ORDER BY id",
                    (status,)).fetchall()
            else:
                rows = con.execute(
                    "SELECT * FROM scheduled_tests ORDER BY id").fetchall()
            return self._send(200, [to_json(r) for r in rows])
        if path == "/api/exams/upcoming":
            today = time.strftime("%Y-%m-%d")
            rows = con.execute(
                """SELECT * FROM scheduled_tests
                   WHERE status IN ('SCHEDULED','PUBLISHED')
                     AND (exam_date IS NULL OR exam_date = '' OR exam_date >= ?)
                   ORDER BY CASE WHEN exam_date IS NULL OR exam_date = ''
                                 THEN 1 ELSE 0 END, exam_date""",
                (today,)).fetchall()
            return self._send(200, [to_json(r) for r in rows])
        m = re.fullmatch(r"/api/exams/(\d+)", path or "")
        if m:
            row = self._get_exam(con, m.group(1))
            if row is None:
                return self._err(404, f"Exam not found: {m.group(1)}")
            return self._send(200, to_json(row))
        m = re.fullmatch(r"/api/exams/(\d+)/questions", path or "")
        if m:
            row = self._get_exam(con, m.group(1))
            if row is None:
                return self._err(404, f"Exam not found: {m.group(1)}")
            return self._proxy_questions(row)
        return self._err(404, "Not found.")

    def _proxy_questions(self, row):
        pids = [p for p in (row["paper_ids"] or "").split(",") if p.strip()]
        if not pids:
            return self._send(200, [])
        out = []
        for pid in pids:
            url = f"{EXAM_API}/api/questions/paper/{pid}"
            req = urllib.request.Request(
                url, headers={"Authorization": f"Bearer {make_jwt('schedule-service', 'ADMIN')}",
                              "Accept": "application/json"})
            try:
                with urllib.request.urlopen(req, timeout=10) as resp:
                    data = json.loads(resp.read() or b"[]")
            except urllib.error.HTTPError as e:
                return self._err(502, f"exam-server error for paper {pid}: HTTP {e.code}")
            except Exception as e:
                return self._err(502, f"exam-server unreachable: {e}")
            if isinstance(data, dict):
                data = data.get("items") or data.get("questions") or data.get("content") or []
            if isinstance(data, list):
                out.extend(data)
        return self._send(200, out)

    # ---- writes (ADMIN only) ----
    def do_POST(self):
        parsed = urlparse(self.path)
        path = parsed.path
        if path == "/api/exams":
            if self._auth(admin=True) is None:
                return
            norm = normalize(self._body())
            if norm["status"] not in ("DRAFT", "SCHEDULED", "PUBLISHED"):
                norm["status"] = "DRAFT"
            bad = validate(norm)
            if bad:
                return self._err(400, bad)
            con = db()
            cur = con.execute(
                f"INSERT INTO scheduled_tests ({', '.join(COLUMNS)}) "
                f"VALUES ({', '.join('?' * len(COLUMNS))})",
                tuple(norm[c] for c in COLUMNS))
            con.commit()
            row = con.execute("SELECT * FROM scheduled_tests WHERE id=?",
                              (cur.lastrowid,)).fetchone()
            return self._send(201, to_json(row))
        m = re.fullmatch(r"/api/exams/(\d+)/publish", path or "")
        if m:
            if self._auth(admin=True) is None:
                return
            con = db()
            row = self._get_exam(con, m.group(1))
            if row is None:
                return self._err(404, f"Exam not found: {m.group(1)}")
            if row["status"] not in ("DRAFT", "SCHEDULED"):
                return self._err(409, "Only DRAFT/SCHEDULED exams can be published")
            con.execute("UPDATE scheduled_tests SET status='PUBLISHED' WHERE id=?",
                        (row["id"],))
            con.commit()
            fresh = con.execute("SELECT * FROM scheduled_tests WHERE id=?",
                                (row["id"],)).fetchone()
            return self._send(200, to_json(fresh))
        m = re.fullmatch(r"/api/exams/(\d+)/papers", path or "")
        if m:
            if self._auth(admin=True) is None:
                return
            con = db()
            row = self._get_exam(con, m.group(1))
            if row is None:
                return self._err(404, f"Exam not found: {m.group(1)}")
            pid = _str(self._body().get("paperId"))
            if not pid:
                return self._err(400, "paperId is required")
            pids = [p for p in (row["paper_ids"] or "").split(",") if p.strip()]
            added = 0
            if pid not in pids:
                pids.append(pid)
                added = 1
                con.execute("UPDATE scheduled_tests SET paper_ids=? WHERE id=?",
                            (",".join(pids), row["id"]))
                con.commit()
            return self._send(200, {"examId": row["id"], "added": added})
        return self._err(404, "Not found.")

    def do_PUT(self):
        m = re.fullmatch(r"/api/exams/(\d+)", urlparse(self.path).path or "")
        if not m:
            return self._err(404, "Not found.")
        if self._auth(admin=True) is None:
            return
        con = db()
        row = self._get_exam(con, m.group(1))
        if row is None:
            return self._err(404, f"Exam not found: {m.group(1)}")
        if row["status"] not in ("DRAFT", "SCHEDULED"):
            return self._err(409, "Exam can only be modified in DRAFT/SCHEDULED status")
        norm = normalize(self._body())
        norm["status"] = row["status"]  # updates must not flip status
        bad = validate(norm)
        if bad:
            return self._err(400, bad)
        con.execute(
            f"UPDATE scheduled_tests SET {', '.join(c + '=?' for c in COLUMNS)} WHERE id=?",
            tuple(norm[c] for c in COLUMNS) + (row["id"],))
        con.commit()
        fresh = con.execute("SELECT * FROM scheduled_tests WHERE id=?",
                            (row["id"],)).fetchone()
        return self._send(200, to_json(fresh))

    def log_message(self, fmt, *args):
        print(f"[schedule] {self.command} {self.path} -> {fmt % args}", flush=True)


if __name__ == "__main__":
    db().close()  # ensure schema exists
    print(f"[schedule] SQLite: {DB_PATH}", flush=True)
    print(f"[schedule] proxy exam-server: {EXAM_API}", flush=True)
    print(f"[schedule] Listening on http://127.0.0.1:{PORT}", flush=True)
    HTTPServer(("127.0.0.1", PORT), Handler).serve_forever()
