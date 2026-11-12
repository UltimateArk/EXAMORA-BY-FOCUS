import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import {
  useUploadPdfMutation,
  useProcessPaperMutation,
  useAttachPaperMutation,
  useListExamsQuery,
  useSubjectsQuery
} from '../../services/examApi.js';

const STEPS = ['Uploading', 'Reading pages', 'Finding questions', 'Saving'];

function asList(data) {
  if (Array.isArray(data)) return data;
  return data?.items || data?.exams || data?.content || [];
}

export default function UploadPaper() {
  const [searchParams] = useSearchParams();
  const queryExamId = searchParams.get('examId') || '';
  const navigate = useNavigate();

  const [file, setFile] = useState(null);
  const [paperId, setPaperId] = useState('');
  const fileRef = useRef(null);
  const [meta, setMeta] = useState({ exam: 'JEE', subject: 'physics', topic: '', difficulty: 'medium' });
  const [banner, setBanner] = useState(null);
  const [savedCount, setSavedCount] = useState(null);
  const [processing, setProcessing] = useState(false);
  const [stepIdx, setStepIdx] = useState(0);
  const [selectedExamId, setSelectedExamId] = useState(queryExamId);

  const [uploadPdf, { isLoading: uploading }] = useUploadPdfMutation();
  const [processPaper] = useProcessPaperMutation();
  const [attachPaper, { isLoading: attaching }] = useAttachPaperMutation();
  const { data: examsData } = useListExamsQuery();
  const { data: subjectsData } = useSubjectsQuery();

  const exams = asList(examsData);

  useEffect(() => {
    if (queryExamId) setSelectedExamId(queryExamId);
  }, [queryExamId]);

  const ok = (msg) => setBanner({ type: 'success', msg });
  const err = (msg) => setBanner({ type: 'error', msg });

  const resolveSubjectId = () => {
    const list = Array.isArray(subjectsData) ? subjectsData : (subjectsData?.items || []);
    return list.find((s) => String(s.name || '').toLowerCase() === String(meta.subject).toLowerCase())?.id;
  };

  const runPipeline = async (pid) => {
    setProcessing(true);
    setStepIdx(0);
    const tick = setInterval(() => setStepIdx((i) => Math.min(i + 1, STEPS.length - 1)), 1200);
    try {
      const topicNum = meta.topic.trim() !== '' && !Number.isNaN(Number(meta.topic)) ? Number(meta.topic) : undefined;
      const res = await processPaper({
        paperId: pid,
        examType: meta.exam,
        subjectId: resolveSubjectId(),
        topicId: topicNum,
        difficulty: String(meta.difficulty).toUpperCase()
      }).unwrap();
      clearInterval(tick);
      setStepIdx(STEPS.length - 1);
      setSavedCount(res.savedCount ?? 0);
      if (selectedExamId) {
        try {
          await attachPaper({ examId: selectedExamId, paperId: pid }).unwrap();
          ok(`${res.savedCount ?? 0} questions ready and linked to the selected test.`);
        } catch {
          ok(`${res.savedCount ?? 0} questions ready (reference ${pid}). Could not link to the test — retry from Exams.`);
        }
      } else {
        ok(`${res.savedCount ?? 0} questions ready.`);
      }
    } catch {
      clearInterval(tick);
      err('Preparing the paper failed. Check the file and try again.');
    } finally {
      setProcessing(false);
    }
  };

  const doUpload = async (f) => {
    if (!f || uploading || processing) return;
    setBanner(null);
    setSavedCount(null);
    setFile(f);
    try {
      const res = await uploadPdf(f).unwrap();
      setPaperId(res.paperId);
      await runPipeline(res.paperId);
    } catch {
      err('Upload failed. Please try again.');
    }
  };

  const onPickFile = (e) => {
    const f = e.target.files?.[0] || null;
    // allow re-picking the same file twice
    e.target.value = '';
    if (!f) return;
    doUpload(f);
  };

  const onUpload = () => fileRef.current?.click();
  const busy = uploading || processing || attaching;

  return (
    <div className="admin-page">
      <h2 className="admin-title">Upload question paper</h2>
      {banner && <div className={`banner ${banner.type}`}>{banner.msg}</div>}
      {savedCount != null && paperId && !processing && (
        <div className="admin-actions">
          <span className="muted">{savedCount} questions ready.</span>
          <button className="btn btn-green" onClick={() => navigate(`/admin/review?paperId=${paperId}`)}>
            Review questions
          </button>
        </div>
      )}

      <div className="admin-form">
        <section className="card form-section">
          <span className="pill">1. Upload</span>
          {exams.length === 0 ? (
            <p className="muted">
              No tests yet. <Link to="/admin" style={{ fontWeight: 800, textDecoration: 'underline' }}>Schedule a test first</Link> or continue without one.
            </p>
          ) : (
            <div className="exam-pick-list">
              <button
                type="button"
                className={`exam-pick${selectedExamId === '' ? ' selected' : ''}`}
                onClick={() => setSelectedExamId('')}
              >
                <span className="exam-pick-name">No test (standalone)</span>
              </button>
              {exams.map((ex) => {
                const id = String(ex.id ?? ex._id ?? '');
                const active = selectedExamId !== '' && String(selectedExamId) === id;
                return (
                  <button
                    key={id}
                    type="button"
                    className={`exam-pick${active ? ' selected' : ''}`}
                    onClick={() => setSelectedExamId(id)}
                  >
                    <span className="exam-pick-name">{ex.name || id}</span>
                    {ex.examDate || ex.date ? <span className="muted">{ex.examDate || ex.date}</span> : null}
                    {ex.status ? <span className="status-pill">{ex.status}</span> : null}
                  </button>
                );
              })}
            </div>
          )}
        </section>

        <section className="card form-section">
          <span className="pill">2. Details</span>
          <div className="form-grid">
            <label className="field">Exam<input className="input" value={meta.exam} onChange={(e) => setMeta({ ...meta, exam: e.target.value })} /></label>
            <label className="field">Subject
              <select className="select" value={meta.subject} onChange={(e) => setMeta({ ...meta, subject: e.target.value })}>
                <option value="physics">Physics</option>
                <option value="chemistry">Chemistry</option>
                <option value="maths">Maths</option>
              </select>
            </label>
            <label className="field">Topic<input className="input" value={meta.topic} onChange={(e) => setMeta({ ...meta, topic: e.target.value })} /></label>
            <label className="field">Difficulty
              <select className="select" value={meta.difficulty} onChange={(e) => setMeta({ ...meta, difficulty: e.target.value })}>
                <option value="easy">Easy</option>
                <option value="medium">Medium</option>
                <option value="hard">Hard</option>
              </select>
            </label>
          </div>
        </section>

        <section className="card form-section">
          <span className="pill">3. Prepare</span>
          <input
            ref={fileRef}
            type="file"
            accept="application/pdf,.pdf"
            onChange={onPickFile}
            hidden
          />
          <div className="admin-actions">
            <button className="btn" onClick={onUpload} disabled={busy}>
              {uploading ? 'Uploading…' : processing ? 'Preparing…' : 'Upload PDF'}
            </button>
            {file && !busy && (
              <button
                className="btn btn-ghost"
                onClick={() => { setFile(null); setPaperId(''); setSavedCount(null); }}
              >
                Remove
              </button>
            )}
          </div>
          {file && (
            <p className="muted">
              Selected: <b>{file.name}</b> ({(file.size / 1024).toFixed(0)} KB)
              {paperId ? ' — uploaded.' : ' — uploading…'}
            </p>
          )}
          {paperId && <p className="muted">Paper reference: {paperId}</p>}
          {(uploading || processing) && (
            <div className="prepare-progress" aria-live="polite">
              <span className="spinner" aria-hidden="true" />
              <ol className="prepare-steps">
                {STEPS.map((s, i) => (
                  <li key={s} className={i < stepIdx ? 'done' : i === stepIdx ? 'current' : ''}>
                    {s}{i === stepIdx ? '…' : ''}
                  </li>
                ))}
              </ol>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
