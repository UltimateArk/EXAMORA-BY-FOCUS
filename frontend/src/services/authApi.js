import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';

const AUTH_BASE = import.meta.env.VITE_AUTH_API || 'http://localhost:8081';

export const authApi = createApi({
  reducerPath: 'authApi',
  baseQuery: fetchBaseQuery({
    baseUrl: `${AUTH_BASE}/api/auth`,
    prepareHeaders: (headers) => {
      const token = localStorage.getItem('token');
      if (token) headers.set('Authorization', `Bearer ${token}`);
      return headers;
    }
  }),
  endpoints: (builder) => ({
    register: builder.mutation({
      query: (body) => ({ url: '/register', method: 'POST', body })
    }),
    login: builder.mutation({
      query: (body) => ({ url: '/login', method: 'POST', body })
    }),
    me: builder.query({
      query: () => '/me'
    })
  })
});

export const { useRegisterMutation, useLoginMutation, useMeQuery } = authApi;
