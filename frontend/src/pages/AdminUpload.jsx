import { Navigate } from 'react-router-dom';

/** Legacy route /upload — the admin upload flow now lives at /admin/upload. */
export default function AdminUpload() {
  return <Navigate to="/admin/upload" replace />;
}
