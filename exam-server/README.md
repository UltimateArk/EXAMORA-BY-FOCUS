# exam-server

Spring Boot 3.5.x · Java 21 · PostgreSQL · Spring Security (shared-secret JWT) · PDFBox 2.0.x (images only) · WebClient → Python Nougat extractor (text).

## Run

```bash
createdb examora
export JWT_SECRET='<>=32 chars shared with auth-server>'
export EXTRACTOR_URL=http://localhost:8001
export STORAGE_DIR=./storage
mvn spring-boot:run
# or: mvn -q package -DskipTests && java -jar target/exam-server-0.1.0.jar
```

DB DDL auto-updates (`spring.jpa.hibernate.ddl-auto=update`); canonical SQL is
`src/main/resources/db/V1__init.sql` + `src/main/resources/db/V2__exams.sql`
(mirrored under `/db/`).

## Endpoints

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/api/admin/upload-pdf` (multipart `file`) | JWT | → `{paperId,fileName,message}` |
| GET | `/api/admin/extract-text/{paperId}` | JWT | Calls extractor `/extract`, → `{paperId,fileName,pages:[{pageNumber,extractedText}],images:[],questions:[]}` |
| GET | `/api/admin/extract-question-images/{paperId}` | JWT | PDFBox render → `{paperId,fileName,images:[{pageNumber,imagePath}],questions:[{questionNumber,pageNumber,x,y,width,height,questionText}]}` |
| POST | `/api/admin/save-extracted-questions/{paperId}?examType=&subjectId=&topicId=&difficulty=` | JWT | → `{savedCount}` (idempotent: clears this paper's rows first) |
| POST | `/api/admin/process-paper/{paperId}?examType=&subjectId=&topicId=&difficulty=` | JWT | one-shot: read pages → find questions → save + answer-key pass → `{paperId,pages,questionsFound,savedCount,answerKeyApplied}` (status EXTRACTING→EXTRACTED, FAILED+errorLog on error) |
| POST | `/api/admin/papers/{paperId}/submit` | JWT | 409 `{paperId,pending}` if any question still PENDING, else status SUBMITTED → `{paperId,submitted}` |
| GET | `/api/admin/images/{paperId}/page-{page}.png` | JWT | PNG resource |
| GET | `/api/admin/question-images/{paperId}/{file}` | JWT | PNG resource (`question-1.png` …) |
| GET | `/api/questions/paper/{paperId}` | JWT | saved questions + options |
| GET | `/api/questions/generate?subjectId=&topicId=&difficulty=&examType=&count=` | JWT | random paper |
| PATCH | `/api/questions/{id}/verify?status=APPROVED\|REJECTED` | JWT | |
| PUT | `/api/questions/{id}` | JWT | `{questionText,markdown,latex,explanation,difficulty,questionType,tolerance,correctIntegerAnswer,correctNumericAnswer,correctAnswer,removeImage,imageUrl,options:[{label,optionText,optionLatex,correct}]}` — `correctAnswer` string parsed per type (INTEGER→Long, NUMERIC→Double); `removeImage:true` or `imageUrl:null` clears image |
| POST | `/api/questions/{id}/image` (multipart `file`) | JWT | png/jpg ≤10MB → stored at `storage/papers/{paperId}/question-{n}-custom.png`, updates `imagePath` + `imageBase64` (raw base64, no prefix) |
| POST | `/api/questions/{qid}/options/{label}/image` (multipart `file`) | JWT | png/jpg ≤10MB → stored at `storage/papers/{paperId}/question-{n}-opt-{LABEL}.png`, updates `optionImagePath` + `optionImageBase64` |
| DELETE | `/api/questions/{id}/image` | JWT | clears `imagePath` + `imageBase64` (file left on disk) |
| POST | `/api/exams` | ADMIN | schedule exam → 201 `{name*,examType,className,board,examDate,startTime,endTime,durationMinutes,fee,regDeadline,maxAttempts,instructions,marksCorrect,negativeMarks,marksUnanswered,resultDate,answerReview,centres}` |
| GET | `/api/exams` / `/api/exams?status=SCHEDULED` | ADMIN | list all or by status |
| GET | `/api/exams/upcoming` | ADMIN | SCHEDULED/PUBLISHED with future (or null) date |
| GET | `/api/exams/{id}` | ADMIN | |
| PUT | `/api/exams/{id}` | ADMIN | full update, DRAFT/SCHEDULED only (else 409) |
| POST | `/api/exams/{id}/publish` | ADMIN | DRAFT/SCHEDULED → PUBLISHED (else 409) |
| POST | `/api/exams/{id}/papers` | ADMIN | `{paperId}` — copy APPROVED (or all) questions ordered by `questionNumber`, with text+options snapshot |
| POST | `/api/exams/{id}/questions` | ADMIN | `{questionIds:[...]}` — attach individual questions |
| GET | `/api/exams/{id}/questions` | ADMIN | saved questions + options |
| DELETE | `/api/exams/{id}/questions/{questionId}` | ADMIN | detach → 204 |
| GET | `/api/subjects` | public | |
| GET | `/api/topics/subject/{subjectId}` | public | |
| POST | `/api/subjects/seed`, `/api/topics/seed` | public (bootstrap) | |

## curl

```bash
TOKEN=<jwt-from-auth-server>
curl -H "Authorization: Bearer $TOKEN" -F file=@paper.pdf localhost:8080/api/admin/upload-pdf
curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/admin/extract-text/1
curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/admin/extract-question-images/1
curl -X POST -H "Authorization: Bearer $TOKEN" \
  'localhost:8080/api/admin/save-extracted-questions/1?examType=JEE&subjectId=1&difficulty=MEDIUM'
curl -X POST -H "Authorization: Bearer $TOKEN" \
  'localhost:8080/api/admin/process-paper/1?examType=JEE&subjectId=1&difficulty=MEDIUM'
curl -X POST -H "Authorization: Bearer $TOKEN" localhost:8080/api/admin/papers/1/submit
curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/questions/paper/1
curl -H "Authorization: Bearer $TOKEN" 'localhost:8080/api/questions/generate?subjectId=1&count=5'
curl -X PATCH -H "Authorization: Bearer $TOKEN" localhost:8080/api/questions/3/verify?status=APPROVED
curl localhost:8080/api/subjects

# Schedule an exam (ADMIN role required)
curl -X POST -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  localhost:8080/api/exams -d '{
    "name": "JEE Main Mock 1",
    "examType": "MAIN",
    "className": "12",
    "board": "CBSE",
    "examDate": "2026-12-01",
    "startTime": "09:00:00",
    "endTime": "12:00:00",
    "durationMinutes": 180,
    "fee": 199.00,
    "regDeadline": "2026-11-25T23:59:00",
    "maxAttempts": 1,
    "marksCorrect": 4,
    "negativeMarks": -1,
    "resultDate": "2026-12-05",
    "answerReview": true,
    "centres": "DEL,MUM"
  }'
curl -H "Authorization: Bearer $TOKEN" 'localhost:8080/api/exams?status=SCHEDULED'
curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/exams/upcoming
# Attach a whole paper's questions (APPROVED preferred, ordered by questionNumber)
curl -X POST -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  localhost:8080/api/exams/1/papers -d '{"paperId": 1}'
# Attach individual questions
curl -X POST -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  localhost:8080/api/exams/1/questions -d '{"questionIds": [3, 4]}'
curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/exams/1/questions
curl -X POST -H "Authorization: Bearer $TOKEN" localhost:8080/api/exams/1/publish
# Replace a question's image (png/jpg, max 10MB)
curl -X POST -H "Authorization: Bearer $TOKEN" \
  -F file=@diagram.png localhost:8080/api/questions/3/image
# Replace option B's figure (png/jpg, max 10MB)
curl -X POST -H "Authorization: Bearer $TOKEN" \
  -F file=@opt-b.png localhost:8080/api/questions/3/options/B/image
curl -X DELETE -H "Authorization: Bearer $TOKEN" localhost:8080/api/questions/3/image
```

## Extractor wiring

`NougatClient` POSTs the stored PDF as multipart `file` to `${EXTRACTOR_URL}/extract`
and accepts `{pages:[{pageNumber,extractedText}|{page_number,markdown|text}]}`.
Image extraction never calls the extractor — pure PDFBox (`PdfImageService`:
render page PNGs at 150 DPI → `storage/papers/{id}/page-{n}.png`, naive
question-region split by `Q.`/`N.` headings via `PDFTextStripper` → full-width
slices `question-{n}.png` with `{questionNumber,pageNumber,x,y,width,height}`).
`ExtractionService` merges both: text questions split by question numbers,
options via `A-D` regex, integer answers via `Answer: 42`, numeric via
`Numerical:`.

## LaTeX + image storage (V4)

- `latex` (questions) / `optionLatex` (options): canonical LaTeX converted from
  Nougat markdown by `LatexConverter.toLatex` at save time; editable via
  `PUT /api/questions/{id}` (`latex`, per-option `optionLatex`). Math spans
  (`\(...\)`, `\[...\]`, `$$...$$`, `\begin{...}...\end{...}`) pass through
  verbatim; headings/bold/italic/code/tables/lists/quotes convert; markdown
  images are dropped (stored separately); `& % $ # _ { } ~ ^ \` are escaped.
- Images are **dual-stored**: file path (`imagePath`, e.g.
  `papers/{paperId}/question-1.png` PNG crops rendered at ~150 DPI) **plus**
  raw base64 TEXT (`imageBase64`, no `data:` prefix). `diagramBase64` exists
  but stays null (no detector yet). Re-uploading replaces both; DELETE clears
  both.
- New `SavedQuestionDto` fields (appended, existing order unchanged):
  `latex, imageBase64, imageMime, diagramBase64`; options gain `optionLatex`.
  `imageMime` is sniffed from the stored file's magic bytes when `imagePath`
  exists (PNG → `image/png`, JPEG → `image/jpeg`), else defaults to
  `"image/png"`. Render as `data:{imageMime};base64,{imageBase64}` client-side.
