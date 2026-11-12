import { configureStore } from '@reduxjs/toolkit';
import { authApi } from '../services/authApi.js';
import { examApi } from '../services/examApi.js';
import { scheduleApi } from '../services/scheduleApi.js';

export const store = configureStore({
  reducer: {
    [authApi.reducerPath]: authApi.reducer,
    [examApi.reducerPath]: examApi.reducer,
    [scheduleApi.reducerPath]: scheduleApi.reducer
  },
  middleware: (getDefault) => getDefault().concat(authApi.middleware, examApi.middleware, scheduleApi.middleware)
});
