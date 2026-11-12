import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';

const EXAM_BASE = import.meta.env.VITE_EXAM_API || 'http://localhost:8080';

export const examApi = createApi({
  reducerPath: 'examApi',
  baseQuery: fetchBaseQuery({
    baseUrl: EXAM_BASE,
    prepareHeaders: (headers) => {
      const token = localStorage.getItem('token');
      if (token) headers.set('Authorization', `Bearer ${token}`);
      // Do NOT set Content-Type manually so FormData works.
      return headers;
    }
  }),
  tagTypes: ['Questions', 'Subjects', 'Topics'],
  endpoints: (builder) => ({
    // ---- Backend truth: exam-server AdminController (/api/admin/*) ----
    uploadPdf: builder.mutation({
      // file: File -> POST /api/admin/upload-pdf (multipart) -> {paperId, fileName, message}
      query: (file) => {
        const fd = new FormData();
        fd.append('file', file);
        return { url: '/api/admin/upload-pdf', method: 'POST', body: fd };
      }
    }),
    extractText: builder.mutation({
      // paperId -> GET /api/admin/extract-text/{paperId} (Nougat markdown per page)
      query: (paperId) => ({ url: `/api/admin/extract-text/${paperId}`, method: 'GET' })
    }),
    extractQuestionImages: builder.mutation({
      // paperId -> GET /api/admin/extract-question-images/{paperId} (PDFBox crops)
      query: (paperId) => ({ url: `/api/admin/extract-question-images/${paperId}`, method: 'GET' })
    }),
    saveExtracted: builder.mutation({
      // {paperId, examType?, subjectId?, topicId?, difficulty?}
      // -> POST /api/admin/save-extracted-questions/{paperId} -> {savedCount}
      query: ({ paperId, examType, subjectId, topicId, difficulty }) => {
        const params = {};
        if (examType) params.examType = examType;
        if (subjectId != null && subjectId !== '') params.subjectId = subjectId;
        if (topicId != null && topicId !== '') params.topicId = topicId;
        if (difficulty) params.difficulty = difficulty;
        return { url: `/api/admin/save-extracted-questions/${paperId}`, method: 'POST', params };
      },
      invalidatesTags: ['Questions']
    }),
    processPaper: builder.mutation({
      // One-shot pipeline: {paperId, examType?, subjectId?, topicId?, difficulty?}
      // -> POST /api/admin/process-paper/{paperId}
      // -> {paperId, pages, questionsFound, savedCount, answerKeyApplied}
      query: ({ paperId, examType, subjectId, topicId, difficulty }) => {
        const params = {};
        if (examType) params.examType = examType;
        if (subjectId != null && subjectId !== '') params.subjectId = subjectId;
        if (topicId != null && topicId !== '') params.topicId = topicId;
        if (difficulty) params.difficulty = difficulty;
        return { url: `/api/admin/process-paper/${paperId}`, method: 'POST', params };
      },
      invalidatesTags: ['Questions']
    }),
    submitPaper: builder.mutation({
      // paperId -> POST /api/admin/papers/{paperId}/submit
      // -> {paperId, submitted} or 409 {paperId, pending}
      query: (paperId) => ({ url: `/api/admin/papers/${paperId}/submit`, method: 'POST' })
    }),
    savedQuestions: builder.query({
      // paperId (or {paperId}) -> GET /api/questions/paper/{paperId} -> SavedQuestionDto[]
      query: (params) => {
        const paperId = typeof params === 'string' ? params : params?.paperId;
        return { url: `/api/questions/paper/${paperId}` };
      },
      providesTags: ['Questions']
    }),
    generate: builder.mutation({
      // {subjectId?, topicId?, difficulty?, examType?, count?} -> GET /api/questions/generate -> SavedQuestionDto[]
      query: (params = {}) => ({ url: '/api/questions/generate', params }),
      invalidatesTags: ['Questions']
    }),
    subjects: builder.query({
      query: () => '/api/subjects',
      providesTags: ['Subjects']
    }),
    topics: builder.query({
      // subjectId -> GET /api/topics/subject/{subjectId}
      query: (subjectId) => (subjectId ? `/api/topics/subject/${subjectId}` : '/api/topics/subject/0'),
      providesTags: ['Topics']
    }),
    verifyQuestion: builder.mutation({
      // { id, status: 'APPROVED' | 'REJECTED' } — backend: PATCH /api/questions/{id}/verify?status=
      query: ({ id, status }) => ({ url: `/api/questions/${id}/verify`, method: 'PATCH', params: { status } }),
      invalidatesTags: ['Questions']
    }),
    // ---- Admin portal: question review/edit ----
    updateQuestion: builder.mutation({
      query: ({ id, ...body }) => ({ url: `/api/questions/${id}`, method: 'PUT', body }),
      invalidatesTags: ['Questions']
    }),
    uploadQuestionImage: builder.mutation({
      // { id, file } — replaces the question image
      query: ({ id, file }) => {
        const fd = new FormData();
        fd.append('file', file);
        return { url: `/api/questions/${id}/image`, method: 'POST', body: fd };
      },
      invalidatesTags: ['Questions']
    }),
    uploadOptionImage: builder.mutation({
      // { id, label, file } — replaces one option's figure
      query: ({ id, label, file }) => {
        const fd = new FormData();
        fd.append('file', file);
        return { url: `/api/questions/${id}/options/${label}/image`, method: 'POST', body: fd };
      },
      invalidatesTags: ['Questions']
    })
  })
});

export const {
  useUploadPdfMutation,
  useExtractTextMutation,
  useExtractQuestionImagesMutation,
  useSaveExtractedMutation,
  useProcessPaperMutation,
  useSubmitPaperMutation,
  useSavedQuestionsQuery,
  useGenerateMutation,
  useSubjectsQuery,
  useTopicsQuery,
  useVerifyQuestionMutation,
  useUpdateQuestionMutation,
  useUploadQuestionImageMutation,
  useUploadOptionImageMutation
} = examApi;

/** Build a servable URL for a stored imagePath ("papers/{paperId}/{file}"). */
export function questionImageUrl(paperId, imagePath) {
  if (!imagePath || paperId == null) return null;
  const file = String(imagePath).split('/').pop();
  return `${EXAM_BASE}/api/admin/question-images/${paperId}/${encodeURIComponent(file)}`;
}

/** Page render URL: GET /api/admin/images/{paperId}/page-{n}.png */
export function pageImageUrl(paperId, pageNumber) {
  if (paperId == null || pageNumber == null) return null;
  return `${EXAM_BASE}/api/admin/images/${paperId}/page-${pageNumber}.png`;
}

/** Prefer embedded base64 (raw, no prefix) -> data URI; else fall back to served file URL. */
export function questionImageSrc(q) {
  if (!q) return null;
  if (q.imageBase64) {
    const mime = q.imageMime || 'image/png';
    return `data:${mime};base64,${q.imageBase64}`;
  }
  return questionImageUrl(q.paperId ?? q.paper_id, q.imagePath ?? q.image_path);
}

/** Option figure: embedded base64 -> data URI (options have no served-file fallback). */
export function optionImageSrc(o) {
  if (!o) return null;
  const b64 = o.optionImageBase64 ?? o.option_image_base64;
  if (!b64) return null;
  return `data:image/png;base64,${b64}`;
}
