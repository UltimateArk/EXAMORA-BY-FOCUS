import { Link, useLocation, useNavigate } from 'react-router-dom';
import './Navbar.css';

export default function Navbar() {
  const navigate = useNavigate();
  const location = useLocation();
  const token = localStorage.getItem('token');
  const role = String(localStorage.getItem('role') || '').toUpperCase();
  const isAdmin = token && role === 'ADMIN';

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    navigate('/login');
  };

  return (
    <div className="nav-wrap">
      <nav className="glass-nav">
        <Link to="/" className="brand">
          <span className="brand-name">EXAMORA</span>
          <span className="brand-sub">Powered by Focus</span>
        </Link>
        <div className="nav-links">
          <Link to="/">Dashboard</Link>
          {isAdmin && <Link to="/admin">Admin</Link>}
          <Link to="/upload">Upload</Link>
          <Link to="/exam">Exam</Link>
        </div>
        <div className="nav-cta">
          {token ? (
            <button key={location.pathname} className="btn btn-ghost" onClick={logout}>Logout</button>
          ) : (
            <Link to="/login" className="btn">Login</Link>
          )}
        </div>
      </nav>
    </div>
  );
}
