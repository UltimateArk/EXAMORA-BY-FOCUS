# EXMORA V2 — API Reference (backend truth)

Base: exam-server `http://localhost:8080`, auth-server `http://localhost:8081`,
extractor `http://localhost:8001`. Auth: `Authorization: Bearer <JWT>`
(except register/login/health). All JSON unless noted.

## Auth (`:8081`)

| Method | Path | Body / Notes | Returns |
|---|---|---|---|
| POST | `/api/auth/register` | `{fullName,email,mobile,password,role?}` | 201 `{token,role,userId,email}` |
| POST | `/api/auth/login` | `{email,password}` | 200 `{token,role,userId,email}` |
| GET | `/api/auth/me` | Bearer | `{userId,email,role,fullName}` |
| GET | `/api/auth/validate?token=` | for exam-server | `{email,role,userId}` |

## Admin — upload & extraction (`:8080/api/admin`, JWT)

| Method | Path | Returns |
|---|---|---|
| POST | `/upload-pdf` (multipart `file`) | `{paperId,fileName,message}` |
| GET | `/extract-text/{paperId}` | `{paperId,fileName,pages:[{pageNumber,extractedText}],images:[],questions:[]}` |
| GET | `/extract-question-images/{paperId}` | `{paperId,fileName,images:[{pageNumber,imagePath}],questions:[regions]}` |
| POST | `/save-extracted-questions/{paperId}?examType&subjectId&topicId&difficulty` | `{savedCount}` |
| GET | `/images/{paperId}/page-{n}.png` | PNG bytes |
| GET | `/question-images/{paperId}/{file}` | PNG bytes |

## Questions (`:8080/api/questions`, JWT)

| Method | Path | Returns |
|---|---|---|
| GET | `/paper/{paperId}` | `SavedQuestionDto[]` |
| GET | `/generate?subjectId&topicId&difficulty&examType&count=10` | APPROVED only |
| GET | `/search?query&subjectId&topicId&subtopicId&difficulty&verificationStatus&examType&limit=50` | filtered, newest first |
| PATCH | `/{id}/verify?status=APPROVED\|REJECTED` | updated DTO |
| PUT | `/{id}` (JSON map: `questionText,latex,explanation,difficulty,questionType,correctAnswer,correctIntegerAnswer,correctNumericAnswer,tolerance,options[],removeImage`) | updated DTO |
| POST | `/{id}/image` (multipart `file`, ≤10MB png/jpg) | updated DTO |
| DELETE | `/{id}/image` | updated DTO |

`SavedQuestionDto`: `{id,paperId,questionNumber,pageNumber,questionText,markdown,latex,questionType,subjectId,topicId,difficulty,examType,imagePath,imageBase64,imageMime,diagramBase64,verificationStatus,options:[{label,optionText,optionLatex,correct}],correctAnswer}`.
Render images as `data:{imageMime};base64,{imageBase64}`.

## Exams (`:8080/api/exams`, ADMIN)

> Note: the admin portal now schedules via the `:8082` schedule store
> (next section); these Java endpoints remain for service/student use.

| Method | Path | Returns |
|---|---|---|
| POST | `/` (ExamRequest JSON) | 201 Exam |
| GET | `/` / `?status=` | list |
| GET | `/upcoming` | SCHEDULED/PUBLISHED, future |
| GET | `/{id}` | one |
| PUT | `/{id}` | DRAFT/SCHEDULED only, else 409 |
| POST | `/{id}/publish` | →PUBLISHED |
| POST | `/{id}/papers` `{paperId}` | `{examId,added}` — 409 when paper has zero APPROVED |
| POST | `/{id}/questions` `{questionIds:[...]}` | `{examId,added}` — 409 if any id not APPROVED |
| GET | `/{id}/questions` | `SavedQuestionDto[]` |
| DELETE | `/{id}/questions/{qid}` | 204 |

## Schedule store (`:8082`, SQLite)

`schedule-server.py` (stdlib `http.server` + `sqlite3` only) owns exam
scheduling; the admin portal talks to it via `VITE_SCHEDULE_API`
(default `http://localhost:8082`). DB `./data/scheduled_tests.db`
(gitignored), table `scheduled_tests`. Auth: Bearer HS256 JWT (shared
`JWT_SECRET`, checks `exp`, accepts `role` or `roles`); reads need any
valid token, writes (POST/PUT/publish/attach) need ADMIN — else
401/403 JSON. Run: `JWT_SECRET=... python3 schedule-server.py`.

| Method | Path | Returns |
|---|---|---|
| POST | `/api/exams` (name*, end>start, duration>0 else 400) | 201 Exam |
| GET | `/api/exams` / `?status=` | list |
| GET | `/api/exams/upcoming` | SCHEDULED/PUBLISHED, future |
| GET | `/api/exams/{id}` | one |
| PUT | `/api/exams/{id}` | DRAFT/SCHEDULED only, else 409 |
| POST | `/api/exams/{id}/publish` | →PUBLISHED, else 409 |
| POST | `/api/exams/{id}/papers` `{paperId}` | `{examId,added}` |
| GET | `/api/exams/{id}/questions` | concatenated `:8080` paper questions; exam-server down → 502 |

Exam JSON mirrors `ExamResponse` (`className/examDate/durationMinutes/…`)
plus frontend aliases (`class/date/durationMin`) and paper linkage
(`paperIds`, `paperId`, `papers`). `PUT` is a full replace (status kept).
Questions proxy mints a short-lived `sub=schedule-service role=ADMIN`
JWT with the same secret for `:8080/api/questions/paper/{pid}`.

## Subjects / Topics (`:8080`, JWT)

`GET /api/subjects`, `GET /api/topics/subject/{subjectId}`,
`GET /api/subtopics/topic/{topicId}`
(+ `POST /api/subjects/seed`, `POST /api/topics/seed`, `POST /api/subtopics/seed`
body `[{topicId,name}]` for bootstrapping).

## Extractor (`:8001`, no auth — localhost only)

| Method | Path | Returns |
|---|---|---|
| GET | `/health` | `{status,service,nougatEnabled,model,loadError}` |
| POST | `/extract` (multipart `pdf` + `paperId`) | `{paperId,status:ok\|stub\|error,pages:[{pageNumber,markdown}]}` |
