import { configureStore } from '@reduxjs/toolkit';
import { authApi } from '../services/authApi.js';
import { examApi } from '../services/examApi.js';

export const store = configureStore({
  reducer: {
    [authApi.reducerPath]: authApi.reducer,
    [examApi.reducerPath]: examApi.reducer
  },
  middleware: (getDefault) => getDefault().concat(authApi.middleware, examApi.middleware)
});
