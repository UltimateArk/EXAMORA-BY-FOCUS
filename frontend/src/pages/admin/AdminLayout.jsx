import { useState } from 'react';
import { Navigate, NavLink, Outlet, useNavigate } from 'react-router-dom';
import './Admin.css';

export function RequireAdmin({ children }) {
  const token = localStorage.getItem('token');
  const role = String(localStorage.getItem('role') || '').toUpperCase();
  if (!token || role !== 'ADMIN') return <Navigate to="/login" replace />;
  return children;
}

const LINKS = [
  { to: '/admin', end: true, label: 'Schedule Exam' },
  { to: '/admin/upload', end: false, label: 'Upload Paper' },
  { to: '/admin/review', end: false, label: 'Review Questions' },
  { to: '/admin/exams', end: false, label: 'Exams' }
];

export default function AdminLayout() {
  const [drawer, setDrawer] = useState(false);
  const navigate = useNavigate();

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    navigate('/login');
  };

  const close = () => setDrawer(false);

  return (
    <div className="admin-shell">
      <header className="admin-topbar">
        <button
          className="admin-burger"
          onClick={() => setDrawer((d) => !d)}
          aria-label="Toggle admin menu"
          aria-expanded={drawer}
        >
          <span /><span /><span />
        </button>
        <span className="admin-brand">Admin Portal</span>
        <span className="pill">ADMIN</span>
      </header>

      <div className="admin-body">
        <aside className="admin-sidebar">
          <nav>
            {LINKS.map((l) => (
              <NavLink
                key={l.to}
                to={l.to}
                end={l.end}
                className={({ isActive }) => `admin-link${isActive ? ' active' : ''}`}
              >
                {l.label}
              </NavLink>
            ))}
          </nav>
          <button className="btn btn-danger-ghost admin-logout" onClick={logout}>Logout</button>
        </aside>

        <main className="admin-main">
          <Outlet />
        </main>
      </div>

      {drawer && <div className="admin-drawer-backdrop" onClick={close} />}
      <aside className={`admin-drawer${drawer ? ' open' : ''}`} aria-hidden={!drawer}>
        <nav>
          {LINKS.map((l) => (
            <NavLink
              key={l.to}
              to={l.to}
              end={l.end}
              onClick={close}
              className={({ isActive }) => `admin-link${isActive ? ' active' : ''}`}
            >
              {l.label}
            </NavLink>
          ))}
        </nav>
        <button className="btn btn-danger-ghost admin-logout" onClick={logout}>Logout</button>
      </aside>

      <nav className="admin-bottomtabs" aria-label="Admin sections">
        {LINKS.map((l) => (
          <NavLink
            key={l.to}
            to={l.to}
            end={l.end}
            className={({ isActive }) => `admin-bottomtab${isActive ? ' active' : ''}`}
          >
            {l.label}
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
