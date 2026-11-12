import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useLoginMutation } from '../services/authApi.js';
import './Auth.css';

const TABS = [
  {
    key: 'admin',
    label: 'Admin Portal',
    icon: 'A',
    heading: 'Admin sign in',
    hint: 'Schedule exams, upload papers and verify questions.'
  },
  {
    key: 'centre',
    label: 'Centre Admin',
    icon: 'C',
    heading: 'Centre admin sign in',
    hint: 'Manage venues, invigilators and seat plans per centre.'
  },
  {
    key: 'student',
    label: 'Student',
    icon: 'S',
    heading: 'Student sign in',
    hint: 'View admit cards, attempt mocks and check results.'
  }
];

export default function Login() {
  const [tab, setTab] = useState('admin');
  const [form, setForm] = useState({ email: '', password: '' });
  const [banner, setBanner] = useState(null);
  const [login, { isLoading }] = useLoginMutation();
  const navigate = useNavigate();
  const active = TABS.find((t) => t.key === tab);

  const onSubmit = async (e) => {
    e.preventDefault();
    setBanner(null);
    if (tab !== 'admin') {
      const who = tab === 'centre' ? 'Centre' : 'Student';
      setBanner({
        type: 'info',
        msg: `${who} portal coming soon — access opens after admin portal launch.`
      });
      return;
    }
    try {
      const res = await login(form).unwrap();
      const token = res.token || res.accessToken || res.access_token;
      const role = String(res.role || res.user?.role || res.data?.role || '').toUpperCase();
      if (!token) {
        setBanner({ type: 'error', msg: 'Login failed: no token returned.' });
        return;
      }
      localStorage.setItem('token', token);
      localStorage.setItem('role', role || 'ADMIN');
      if (role && role !== 'ADMIN') {
        localStorage.removeItem('token');
        localStorage.removeItem('role');
        setBanner({ type: 'error', msg: 'This account is not an admin.' });
        return;
      }
      navigate('/admin');
    } catch {
      setBanner({ type: 'error', msg: 'Login failed. Check credentials or auth service.' });
    }
  };

  return (
    <div className="page auth-page">
      <div className="auth-card">
        <h2 className="auth-title">Welcome to Examora</h2>
        <p className="auth-sub">Choose your portal to continue</p>

        <div className="auth-tabs" role="tablist" aria-label="Login portals">
          {TABS.map((t) => (
            <button
              key={t.key}
              role="tab"
              aria-selected={tab === t.key}
              className={`auth-tab${tab === t.key ? ' active' : ''}`}
              onClick={() => { setTab(t.key); setBanner(null); }}
            >
              <span className="auth-tab-icon">{t.icon}</span>
              {t.label}
            </button>
          ))}
          <span className={`auth-tab-glider pos-${tab}`} aria-hidden="true" />
        </div>
        <p className="auth-hint">{active.hint}</p>

        <div key={tab} className="auth-form-anim">
          <h3 className="auth-heading">
            <span className="auth-heading-icon">{active.icon}</span>
            {active.heading}
          </h3>
          <form onSubmit={onSubmit} className="auth-form">
            <label className="field">
              Email
              <input
                className="input"
                type="email"
                required
                placeholder="you@example.com"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />
            </label>
            <label className="field">
              Password
              <input
                className="input"
                type="password"
                required
                placeholder="••••••••"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
              />
            </label>
            <button className="btn auth-submit" disabled={isLoading}>
              {isLoading ? 'Signing in…' : `Sign in to ${active.label}`}
            </button>
          </form>
        </div>

        {banner && <div className={`banner ${banner.type}`}>{banner.msg}</div>}
        <p className="auth-foot">
          No account?{' '}
          <Link to="/register" style={{ color: '#5042d6', fontWeight: 700 }}>Register</Link>
        </p>
      </div>
    </div>
  );
}
