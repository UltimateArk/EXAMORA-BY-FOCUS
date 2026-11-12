import { useState } from 'react';

// Palette states per PRD: white = unattempted, grey = seen, green = answered, purple = marked for review
const STATE_COLORS = {
  unattempted: '#ffffff',
  seen: '#9ca3af',
  answered: '#15803d',
  marked: '#7652c5'
};

const MOCK = [
  { id: 1, text: 'A projectile is launched at 45°. Range formula?', options: ['v²/g', 'v²sin2θ/g', 'v²/2g', '2v²/g'] },
  { id: 2, text: 'pH of 0.01M HCl?', options: ['1', '2', '3', '4'] },
  { id: 3, text: '∫ x dx = ?', options: ['x²/2 + C', 'x² + C', '2x + C', 'ln x + C'] }
];

export default function Exam() {
  const [states, setStates] = useState({ 1: 'unattempted', 2: 'unattempted', 3: 'unattempted' });
  const [answers, setAnswers] = useState({});

  const mark = (id, s) => setStates((prev) => ({ ...prev, [id]: s }));

  return (
    <div className="page">
      <div className="hero">
        <h1>Exam hall</h1>
        <p>Palette: white unattempted · grey seen · green answered · purple marked.</p>
      </div>
      <div className="grid2" style={{ marginTop: 18 }}>
        <div style={{ display: 'grid', gap: 14 }}>
          {MOCK.map((q) => (
            <div key={q.id} className="card">
              <span className="pill">Q{q.id}</span>
              <p><b>{q.text}</b></p>
              {q.options.map((o, i) => (
                <label key={i} style={{ display: 'block', margin: '6px 0' }}>
                  <input
                    type="radio"
                    name={`q-${q.id}`}
                    checked={answers[q.id] === i}
                    onChange={() => { setAnswers({ ...answers, [q.id]: i }); mark(q.id, 'answered'); }}
                  /> {'ABCD'[i]}. {o}
                </label>
              ))}
              <div className="form-row" style={{ marginTop: 10 }}>
                <button className="btn btn-ghost" onClick={() => mark(q.id, 'seen')}>Mark seen</button>
                <button className="btn btn-ghost" onClick={() => mark(q.id, 'marked')}>Mark for review</button>
                <button className="btn btn-danger-ghost" onClick={() => { setAnswers(({ [q.id]: _drop, ...rest }) => rest); mark(q.id, 'unattempted'); }}>Clear</button>
              </div>
            </div>
          ))}
        </div>
        <div className="card" style={{ alignSelf: 'start' }}>
          <h3>Palette</h3>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            {MOCK.map((q) => (
              <div
                key={q.id}
                title={`${q.id}: ${states[q.id]}`}
                style={{
                  width: 40, height: 40, borderRadius: 12, display: 'grid', placeItems: 'center',
                  fontWeight: 800, border: '1px solid #dfe3f0',
                  background: STATE_COLORS[states[q.id]],
                  color: states[q.id] === 'unattempted' ? '#1e1b4b' : '#fff'
                }}
              >{q.id}</div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
