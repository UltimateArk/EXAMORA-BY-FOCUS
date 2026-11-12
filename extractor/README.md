# Examora Nougat Extractor (TEXT only)

FastAPI microservice on **port 8001**. Renders PDF pages to PNG (PyMuPDF, 300 DPI)
**only as input to Nougat** and returns per-page markdown text.

> Image/diagram cropping stays in **Java (PDFBox)** in `exam-server`.
> This service never crops or stores question images.

## Run

```bash
cd extractor
pip install -r requirements.txt
# CPU-only torch (recommended on Mac / CPU boxes):
# pip install --index-url https://download.pytorch.org/whl/cpu torch

# Stub mode (default) — no model needed, Java proceeds with PDFBox text:
python app.py
# or
uvicorn app:app --port 8001

# Full Nougat mode:
NOUGAT_ENABLED=1 python app.py
# or
NOUGAT_ENABLED=1 uvicorn app:app --port 8001
```

## Model note

- Model: `facebook/nougat-base` (~350 MB, downloaded from Hugging Face on first use).
- Override with `NOUGAT_MODEL_ID=facebook/nougat-small ...`.
- CPU speed: roughly **30–60 s/page** (MPS on Apple Silicon is faster).
- Without network / without the model cached, `/extract` returns a `stub`
  response so Java can still proceed with PDFBox text — this is by design.

## API

- `GET /health` → `{status, service, nougatEnabled, model, loadError}`
- `POST /extract` (multipart: `pdf` file + `paperId` text field)
  - Success: `{paperId, status:"ok", pages:[{pageNumber, markdown}]}`
  - Stub: `{paperId, status:"stub", message, pages:[]}`
  - Per-page failures never abort the whole paper; failed pages carry `error`.

## Test

```bash
curl localhost:8001/health
curl -F "pdf=@sample.pdf" -F "paperId=jee-2024-p1" localhost:8001/extract
```
