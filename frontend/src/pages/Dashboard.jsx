import { useState } from 'react';
import { useGenerateMutation, useSubjectsQuery } from '../services/examApi.js';

const SUBJECTS = [
  { key: 'physics', name: 'Physics', desc: 'Mechanics, E&M, optics & modern physics', cls: 'physics', accent: 'subject-accent-physics' },
  { key: 'chemistry', name: 'Chemistry', desc: 'Physical, organic & inorganic', cls: 'chemistry', accent: 'subject-accent-chemistry' },
  { key: 'maths', name: 'Maths', desc: 'Algebra, calculus, vectors & probability', cls: 'maths', accent: 'subject-accent-maths' }
];

export default function Dashboard() {
  const [form, setForm] = useState({ examType: 'JEE', subject: 'physics', topic: '', difficulty: 'medium', count: 5 });
  const [generate, { data, isLoading, error }] = useGenerateMutation();
  const { data: subjectsData } = useSubjectsQuery();

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const resolveSubjectId = () => {
    const list = Array.isArray(subjectsData) ? subjectsData : (subjectsData?.items || []);
    const hit = list.find((s) => String(s.name || '').toLowerCase() === String(form.subject).toLowerCase());
    return hit?.id;
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    try {
      const params = { examType: form.examType, difficulty: String(form.difficulty).toUpperCase(), count: Number(form.count) };
      const sid = resolveSubjectId();
      if (sid != null) params.subjectId = sid;
      await generate(params).unwrap();
    } catch {
      // error rendered below
    }
  };

  return (
    <div className="page">
      <section className="hero">
        <span className="pill" style={{ background: 'rgba(255,255,255,0.2)', color: '#fff' }}>JEE • NEET • Practice</span>
        <h1>Generate exams in seconds.</h1>
        <p>Pick an exam type, subject and topic. EXAMORA builds a balanced paper with the right difficulty mix.</p>
      </section>

      <div className="grid2" style={{ marginTop: 20 }}>
        <form className="card" onSubmit={onSubmit}>
          <h3 style={{ marginTop: 0 }}>Generator</h3>
          <div className="grid2">
            <label className="field">Exam type
              <select className="select" value={form.examType} onChange={set('examType')}>
                <option>JEE</option>
                <option>NEET</option>
                <option>Practice</option>
              </select>
            </label>
            <label className="field">Subject
              <select className="select" value={form.subject} onChange={set('subject')}>
                <option value="physics">Physics</option>
                <option value="chemistry">Chemistry</option>
                <option value="maths">Maths</option>
              </select>
            </label>
            <label className="field">Topic
              <input className="input" placeholder="e.g. Rotational Mechanics" value={form.topic} onChange={set('topic')} />
            </label>
            <label className="field">Difficulty
              <select className="select" value={form.difficulty} onChange={set('difficulty')}>
                <option value="easy">Easy</option>
                <option value="medium">Medium</option>
                <option value="hard">Hard</option>
              </select>
            </label>
            <label className="field">Count
              <input className="input" type="number" min="1" max="50" value={form.count} onChange={set('count')} />
            </label>
          </div>
          <div style={{ marginTop: 16 }}>
            <button className="btn" disabled={isLoading}>{isLoading ? 'Generating…' : 'Generate paper'}</button>
          </div>
          {error && <div className="banner error">Generate failed. Check the exam server.</div>}
          {data && <div className="banner success">Generated {Array.isArray(data) ? data.length : (data?.questions?.length ?? data?.count ?? '')} questions.</div>}
        </form>

        <div className="card">
          <h3 style={{ marginTop: 0 }}>How it works</h3>
          <p>1. Upload a PDF → 2. Extract text + images → 3. Save & verify → 4. Generate exams.</p>
          <div className="form-row">
            <span className="pill">Nougat OCR</span>
            <span className="pill">PDFBox images</span>
            <span className="pill">RTK Query</span>
          </div>
        </div>
      </div>

      <div className="grid3" style={{ marginTop: 18 }}>
        {SUBJECTS.map((s) => (
          <div key={s.key} className={`subject-card ${s.cls}`}>
            <span className="pill">{s.name}</span>
            <h3 className={s.accent}>{s.name}</h3>
            <p>{s.desc}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
