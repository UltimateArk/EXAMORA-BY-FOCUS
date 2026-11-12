# EXAMORA Frontend (V2)

React 18 + Vite + Redux Toolkit + RTK Query. Plain JS, no TypeScript.

## Run

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

App: http://localhost:5173
- Exam API: `VITE_EXAM_API` (default http://localhost:8080, also proxied via `/api`)
- Auth API: `VITE_AUTH_API` (default http://localhost:8081)

## Routes

- `/` Dashboard — hero + generator form + subject cards
- `/upload` Admin upload — 4-step flow: upload PDF → details → extract text (Nougat) + preview images (PDFBox) → save; saved list with approve/reject via verify endpoint
- `/exam` Exam stub with palette (white/grey/green/purple)
- `/login`, `/register` Auth via auth-server

## Notes

- RTK Query uses `fetchBaseQuery`; `uploadPdf` sends `FormData` without setting `Content-Type` manually.
- Auth token from `localStorage.token` is attached via `prepareHeaders` in both slices.
