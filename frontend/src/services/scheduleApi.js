import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';

const SCHEDULE_BASE = import.meta.env.VITE_SCHEDULE_API || 'http://localhost:8082';

export const scheduleApi = createApi({
  reducerPath: 'scheduleApi',
  baseQuery: fetchBaseQuery({
    baseUrl: SCHEDULE_BASE,
    prepareHeaders: (headers) => {
      const token = localStorage.getItem('token');
      if (token) headers.set('Authorization', `Bearer ${token}`);
      return headers;
    }
  }),
  tagTypes: ['Exams'],
  endpoints: (builder) => ({
    // Schedule store: SQLite-backed schedule-server (:8082)
    scheduleExam: builder.mutation({
      query: (body) => ({ url: '/api/exams', method: 'POST', body }),
      invalidatesTags: ['Exams']
    }),
    listExams: builder.query({
      query: (status) => (status ? { url: '/api/exams', params: { status } } : '/api/exams'),
      providesTags: ['Exams']
    }),
    getExam: builder.query({
      query: (id) => `/api/exams/${id}`,
      providesTags: (_r, _e, id) => [{ type: 'Exams', id }]
    }),
    updateExam: builder.mutation({
      // Backend: PUT /api/exams/{id} (DRAFT/SCHEDULED only)
      query: ({ id, ...body }) => ({ url: `/api/exams/${id}`, method: 'PUT', body }),
      invalidatesTags: ['Exams']
    }),
    publishExam: builder.mutation({
      // Backend: POST /api/exams/{id}/publish
      query: (id) => ({ url: `/api/exams/${id}/publish`, method: 'POST' }),
      invalidatesTags: ['Exams']
    }),
    attachPaper: builder.mutation({
      query: ({ examId, paperId }) => ({
        url: `/api/exams/${examId}/papers`,
        method: 'POST',
        body: { paperId }
      }),
      invalidatesTags: ['Exams']
    }),
    examQuestions: builder.query({
      query: (examId) => `/api/exams/${examId}/questions`,
      providesTags: ['Exams']
    })
  })
});

export const {
  useScheduleExamMutation,
  useListExamsQuery,
  useGetExamQuery,
  useUpdateExamMutation,
  usePublishExamMutation,
  useAttachPaperMutation,
  useExamQuestionsQuery
} = scheduleApi;
