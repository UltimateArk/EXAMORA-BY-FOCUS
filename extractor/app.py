"""
Nougat TEXT-only extractor microservice.
Image/diagram cropping stays in Java (PDFBox) — this service ONLY renders
pages to PNG as input to Nougat and returns markdown text per page.
"""
import io
import logging
import os

import fitz  # PyMuPDF
from fastapi import FastAPI, File, Form, UploadFile
from fastapi.responses import JSONResponse

logging.basicConfig(level=logging.INFO)
log = logging.getLogger("extractor")

app = FastAPI(title="Examora Nougat Extractor (TEXT only)")

MODEL_ID = os.getenv("NOUGAT_MODEL_ID", "facebook/nougat-base")
NOUGAT_ENABLED = os.getenv("NOUGAT_ENABLED", "0") == "1"

# Lazy singletons
_processor = None
_model = None
_device = None
_load_error: str | None = None


def get_device() -> str:
    global _device
    if _device is not None:
        return _device
    try:
        import torch

        if torch.backends.mps.is_available():
            _device = "mps"
        elif torch.cuda.is_available():
            _device = "cuda"
        else:
            _device = "cpu"
    except Exception:
        _device = "cpu"
    log.info("Using device: %s", _device)
    return _device


def load_nougat():
    """Lazy-load Nougat processor + model. Raises on failure."""
    global _processor, _model, _load_error
    if _model is not None and _processor is not None:
        return _processor, _model
    from transformers import NougatProcessor, VisionEncoderDecoderModel

    import torch

    device = get_device()
    log.info("Loading Nougat model %s on %s ...", MODEL_ID, device)
    _processor = NougatProcessor.from_pretrained(MODEL_ID)
    _model = VisionEncoderDecoderModel.from_pretrained(MODEL_ID)
    try:
        _model.to(device)
    except Exception as e:  # MPS fallback to CPU
        log.warning("Could not move model to %s (%s); falling back to CPU", device, e)
        _model.to("cpu")
    _model.eval()
    log.info("Nougat model loaded.")
    return _processor, _model


def stub_response(paper_id: str, reason: str):
    return {
        "paperId": paper_id,
        "status": "stub",
        "message": reason,
        "pages": [],
    }


@app.get("/health")
def health():
    return {
        "status": "ok",
        "service": "nougat-extractor",
        "nougatEnabled": NOUGAT_ENABLED,
        "model": MODEL_ID,
        "loadError": _load_error,
    }


@app.post("/extract")
async def extract(
    pdf: UploadFile = File(..., description="Question paper PDF"),
    paperId: str = Form("unknown"),
):
    raw = await pdf.read()
    if not raw:
        return JSONResponse({"paperId": paperId, "status": "error", "message": "Empty PDF upload", "pages": []}, status_code=400)

    # --- Nougat disabled → stub so Java can proceed with PDFBox text ---
    if not NOUGAT_ENABLED:
        msg = "NOUGAT_ENABLED=0 (or unset): Nougat skipped. Java should proceed with PDFBox text extraction."
        log.info(msg)
        return stub_response(paperId, msg)

    # --- Lazy model load (model ~350MB, downloaded on first use) ---
    try:
        processor, model = load_nougat()
    except Exception as e:
        reason = (
            f"Nougat model '{MODEL_ID}' not available ({type(e).__name__}: {e}). "
            "Run with model downloaded / network access, else Java falls back to PDFBox text."
        )
        log.warning(reason)
        global _load_error
        _load_error = str(e)
        return stub_response(paperId, reason)

    import torch

    device = get_device()

    # --- Render pages to PNG via PyMuPDF at 300 DPI (Nougat input only) ---
    try:
        doc = fitz.open(stream=raw, filetype="pdf")
    except Exception as e:
        log.exception("Failed to open PDF for paper %s", paperId)
        return JSONResponse(
            {"paperId": paperId, "status": "error", "message": f"Invalid PDF: {e}", "pages": []},
            status_code=400,
        )

    zoom = 300 / 72  # 300 DPI
    mat = fitz.Matrix(zoom, zoom)
    pages: list[dict] = []

    with doc:
        for i, page in enumerate(doc):
            page_number = i + 1
            try:
                pix = page.get_pixmap(matrix=mat)
                img_bytes = pix.tobytes("png")
                from PIL import Image

                image = Image.open(io.BytesIO(img_bytes)).convert("RGB")
                pixel_values = processor(image, return_tensors="pt").pixel_values.to(
                    next(model.parameters()).device
                )
                with torch.no_grad():
                    outputs = model.generate(
                        pixel_values,
                        min_length=1,
                        max_length=3584,
                        bad_words_ids=[[processor.tokenizer.unk_token_id]],
                        return_dict_in_generate=True,
                    )
                markdown = processor.batch_decode(outputs.sequences, skip_special_tokens=True)[0]
                # Nougat repeat-hallucination guard: truncate pathological repetitions
                if len(markdown) > 20000:
                    log.warning("Page %d markdown unusually long (%d chars), truncating", page_number, len(markdown))
                    markdown = markdown[:20000]
                pages.append({"pageNumber": page_number, "markdown": markdown.strip()})
                log.info("Paper %s page %d extracted (%d chars)", paperId, page_number, len(markdown))
            except Exception as e:
                log.exception("Nougat failed on paper %s page %d", paperId, page_number)
                pages.append({"pageNumber": page_number, "markdown": "", "error": f"{type(e).__name__}: {e}"})

    return {"paperId": paperId, "status": "ok", "pages": pages}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8001)
