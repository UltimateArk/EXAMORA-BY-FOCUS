import { Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar.jsx';
import Dashboard from './pages/Dashboard.jsx';
import AdminUpload from './pages/AdminUpload.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import Exam from './pages/Exam.jsx';
import AdminLayout, { RequireAdmin } from './pages/admin/AdminLayout.jsx';
import ScheduleExam from './pages/admin/ScheduleExam.jsx';
import UploadPaper from './pages/admin/UploadPaper.jsx';
import ReviewQuestions from './pages/admin/ReviewQuestions.jsx';
import Exams from './pages/admin/Exams.jsx';

function ComingSoon({ title, body }) {
  return (
    <div className="page" style={{ maxWidth: 640 }}>
      <div className="card" style={{ textAlign: 'center', padding: '40px 28px' }}>
        <span className="pill">Coming soon</span>
        <h2 style={{ fontFamily: "'Outfit', sans-serif" }}>{title}</h2>
        <p style={{ color: '#6b7280' }}>{body}</p>
      </div>
    </div>
  );
}

export default function App() {
  return (
    <>
      <Navbar />
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/upload" element={<AdminUpload />} />
        <Route path="/exam" element={<Exam />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route
          path="/centre"
          element={<ComingSoon title="Centre Admin Portal" body="Centre portal coming soon — access opens after admin portal launch." />}
        />
        <Route
          path="/student"
          element={<ComingSoon title="Student Portal" body="Student portal coming soon — access opens after admin portal launch." />}
        />
        <Route
          path="/admin"
          element={
            <RequireAdmin>
              <AdminLayout />
            </RequireAdmin>
          }
        >
          <Route index element={<ScheduleExam />} />
          <Route path="upload" element={<UploadPaper />} />
          <Route path="review" element={<ReviewQuestions />} />
          <Route path="exams" element={<Exams />} />
        </Route>
      </Routes>
    </>
  );
}
