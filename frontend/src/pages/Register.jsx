import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useRegisterMutation } from '../services/authApi.js';

export default function Register() {
  const [form, setForm] = useState({ name: '', email: '', password: '' });
  const [register, { isLoading, error }] = useRegisterMutation();
  const navigate = useNavigate();

  const onSubmit = async (e) => {
    e.preventDefault();
    try {
      const res = await register(form).unwrap();
      if (res.token) localStorage.setItem('token', res.token);
      navigate('/');
    } catch {
      // rendered below
    }
  };

  return (
    <div className="page" style={{ maxWidth: 480 }}>
      <div className="card">
        <h2>Register</h2>
        <form onSubmit={onSubmit} style={{ display: 'grid', gap: 12 }}>
          <input className="input" placeholder="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <input className="input" placeholder="Email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
          <input className="input" type="password" placeholder="Password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
          <button className="btn" disabled={isLoading}>{isLoading ? 'Creating…' : 'Create account'}</button>
        </form>
        {error && <div className="banner error">Registration failed.</div>}
        <p>Have an account? <Link to="/login" style={{ color: '#5042d6', fontWeight: 700 }}>Login</Link></p>
      </div>
    </div>
  );
}
