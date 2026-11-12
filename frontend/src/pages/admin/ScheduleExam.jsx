import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useScheduleExamMutation } from '../../services/examApi.js';

const EXAM_TYPES = ['MAIN', 'ADVANCED', 'NSEJS', 'NSEP', 'IOQM'];

const initial = {
  name: '',
  examType: 'MAIN',
  class: '',
  board: '',
  date: '',
  startTime: '',
  endTime: '',
  durationMin: 180,
  fee: 0,
  regDeadline: '',
  maxAttempts: 1,
  instructions: '',
  status: 'DRAFT',
  marksCorrect: 4,
  negativeMarks: 1,
  marksUnanswered: 0,
  resultDate: '',
  answerReview: true
};

export default function ScheduleExam() {
  const [form, setForm] = useState(initial);
  const [banner, setBanner] = useState(null);
  const [createdId, setCreatedId] = useState(null);
  const [schedule, { isLoading }] = useScheduleExamMutation();
  const navigate = useNavigate();

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const onSubmit = async (e) => {
    e.preventDefault();
    setBanner(null);
    if (!form.name.trim() || !form.date || !form.startTime || !form.endTime || !form.durationMin) {
      setBanner({ type: 'error', msg: 'Name, date, start/end time and duration are required.' });
      return;
    }
    if (form.endTime <= form.startTime) {
      setBanner({ type: 'error', msg: 'End time must be after start time.' });
      return;
    }
    try {
      const res = await schedule({
        ...form,
        durationMin: Number(form.durationMin),
        fee: Number(form.fee),
        maxAttempts: Number(form.maxAttempts),
        marksCorrect: Number(form.marksCorrect),
        negativeMarks: Number(form.negativeMarks),
        marksUnanswered: Number(form.marksUnanswered)
      }).unwrap();
      const id = res.id || res.examId || res.data?.id;
      setCreatedId(id || null);
      setBanner({ type: 'success', msg: `Exam scheduled${id ? ` (ID: ${id})` : ''}.` });
      if (id) setTimeout(() => navigate(`/admin/upload?examId=${id}`), 1500);
    } catch {
      setBanner({ type: 'error', msg: 'Could not schedule. Please try again.' });
    }
  };

  return (
    <div className="admin-page">
      <h2 className="admin-title">Schedule exam</h2>
      <p className="admin-sub">Create the exam shell first — you will upload the question paper next.</p>
      {banner && <div className={`banner ${banner.type}`}>{banner.msg}</div>}
      {createdId && (
        <button className="btn" onClick={() => navigate(`/admin/upload?examId=${createdId}`)}>
          Continue to upload question paper
        </button>
      )}

      <form onSubmit={onSubmit} className="admin-form">
        <section className="card form-section">
          <h3>Basic details</h3>
          <div className="form-grid">
            <label className="field">Exam name *
              <input className="input" value={form.name} onChange={(e) => set('name', e.target.value)} placeholder="JEE Main Mock 12" />
            </label>
            <label className="field">Exam type *
              <select className="select" value={form.examType} onChange={(e) => set('examType', e.target.value)}>
                {EXAM_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
              </select>
            </label>
            <label className="field">Class
              <input className="input" value={form.class} onChange={(e) => set('class', e.target.value)} placeholder="11 / 12 / Dropper" />
            </label>
            <label className="field">Board
              <input className="input" value={form.board} onChange={(e) => set('board', e.target.value)} placeholder="CBSE / ICSE / State" />
            </label>
            <label className="field">Date *
              <input className="input" type="date" value={form.date} onChange={(e) => set('date', e.target.value)} />
            </label>
            <label className="field">Registration deadline
              <input className="input" type="date" value={form.regDeadline} onChange={(e) => set('regDeadline', e.target.value)} />
            </label>
            <label className="field">Start time *
              <input className="input" type="time" value={form.startTime} onChange={(e) => set('startTime', e.target.value)} />
            </label>
            <label className="field">End time *
              <input className="input" type="time" value={form.endTime} onChange={(e) => set('endTime', e.target.value)} />
            </label>
            <label className="field">Duration (minutes) *
              <input className="input" type="number" min="1" value={form.durationMin} onChange={(e) => set('durationMin', e.target.value)} />
            </label>
            <label className="field">Fee (₹)
              <input className="input" type="number" min="0" value={form.fee} onChange={(e) => set('fee', e.target.value)} />
            </label>
            <label className="field">Max attempts
              <input className="input" type="number" min="1" value={form.maxAttempts} onChange={(e) => set('maxAttempts', e.target.value)} />
            </label>
            <label className="field">Status
              <select className="select" value={form.status} onChange={(e) => set('status', e.target.value)}>
                <option value="DRAFT">DRAFT</option>
                <option value="SCHEDULED">SCHEDULED</option>
                <option value="PUBLISHED">PUBLISHED</option>
              </select>
            </label>
          </div>
          <label className="field">Instructions
            <textarea className="textarea" value={form.instructions} onChange={(e) => set('instructions', e.target.value)} placeholder="Rules shown to students before the exam…" />
          </label>
        </section>

        <section className="card form-section">
          <h3>Marking scheme</h3>
          <div className="form-grid">
            <label className="field">Marks per correct answer
              <input className="input" type="number" step="0.5" value={form.marksCorrect} onChange={(e) => set('marksCorrect', e.target.value)} />
            </label>
            <label className="field">Negative marks
              <input className="input" type="number" step="0.25" min="0" value={form.negativeMarks} onChange={(e) => set('negativeMarks', e.target.value)} />
            </label>
            <label className="field">Marks for unanswered
              <input className="input" type="number" step="0.5" value={form.marksUnanswered} onChange={(e) => set('marksUnanswered', e.target.value)} />
            </label>
          </div>
        </section>

        <section className="card form-section">
          <h3>Result</h3>
          <div className="form-grid">
            <label className="field">Result date
              <input className="input" type="date" value={form.resultDate} onChange={(e) => set('resultDate', e.target.value)} />
            </label>
            <label className="check-row">
              <input type="checkbox" checked={form.answerReview} onChange={(e) => set('answerReview', e.target.checked)} />
              Allow answer review after result
            </label>
          </div>
        </section>

        <div className="admin-actions">
          <button className="btn" disabled={isLoading}>{isLoading ? 'Scheduling…' : 'Schedule exam'}</button>
        </div>
      </form>
    </div>
  );
}
