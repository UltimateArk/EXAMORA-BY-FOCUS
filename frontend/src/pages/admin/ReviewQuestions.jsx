import { useEffect, useMemo, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import ExamQuestionView, { normalizeQuestion } from '../../components/ExamQuestionView.jsx';
import {
  useSavedQuestionsQuery,
  useVerifyQuestionMutation,
  useUpdateQuestionMutation,
  useUploadQuestionImageMutation,
  useListExamsQuery
} from '../../services/examApi.js';

function asList(data) {
  if (Array.isArray(data)) return data;
  return data?.items || data?.questions || data?.content || [];
}

const statusOf = (q) => String(q?.status ?? q?.verificationStatus ?? 'PENDING').toUpperCase();
const qid = (q, i) => String(q?.id ?? q?._id ?? `idx-${i}`);

export default function ReviewQuestions() {
  const [searchParams, setSearchParams] = useSearchParams();
  const paperId = searchParams.get('paperId') || '';
  const [paperInput, setPaperInput] = useState(paperId);

  const { data, isLoading, isFetching, isError, refetch } = useSavedQuestionsQuery(
    paperId ? { paperId } : null,
    { skip: !paperId }
  );
  const { data: examsData } = useListExamsQuery();
  const [verify, { isLoading: verifying }] = useVerifyQuestionMutation();
  const [updateQuestion, { isLoading: savingEdit }] = useUpdateQuestionMutation();
  const [uploadImage, { isLoading: uploadingImg }] = useUploadQuestionImageMutation();

  const list = useMemo(() => asList(data), [data]);
  const [idx, setIdx] = useState(0);
  const [seen, setSeen] = useState([]);
  const [flagged, setFlagged] = useState([]);
  const [banner, setBanner] = useState(null);
  const [editMode, setEditMode] = useState(false);
  const [draft, setDraft] = useState(null);
  const [imgFile, setImgFile] = useState(null);
  const cardRef = useRef(null);

  const safeIdx = list.length ? Math.min(idx, list.length - 1) : 0;
  const current = list[safeIdx];
  const currentId = current ? qid(current, safeIdx) : null;
  const norm = useMemo(() => (current ? normalizeQuestion(current) : null), [current]);

  useEffect(() => { setIdx(0); setSeen([]); setEditMode(false); setDraft(null); }, [paperId]);

  useEffect(() => {
    if (currentId && !seen.includes(currentId)) setSeen((s) => [...s, currentId]);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentId]);

  useEffect(() => {
    cardRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }, [safeIdx]);

  useEffect(() => {
    const onKey = (e) => {
      const tag = String(e.target?.tagName || '').toUpperCase();
      if (['INPUT', 'TEXTAREA', 'SELECT'].includes(tag)) return;
      if (e.key === 'ArrowRight') go(1);
      if (e.key === 'ArrowLeft') go(-1);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [list.length, safeIdx]);

  const go = (d) => {
    setEditMode(false);
    setIdx((i) => Math.min(Math.max(i + d, 0), Math.max(list.length - 1, 0)));
  };
  const jump = (i) => { setEditMode(false); setIdx(i); };

  const loadPaper = () => {
    if (!paperInput.trim()) return;
    setSearchParams({ paperId: paperInput.trim() });
  };

  const doVerify = async (status) => {
    if (!current) return;
    try {
      await verify({ id: current.id ?? current._id, status }).unwrap();
      setBanner({ type: 'success', msg: `Question ${status.toLowerCase()}.` });
      refetch();
    } catch {
      setBanner({ type: 'error', msg: 'Verify call failed.' });
    }
  };

  const doFlag = () => {
    if (!currentId) return;
    setFlagged((f) => (f.includes(currentId) ? f.filter((x) => x !== currentId) : [...f, currentId]));
    setBanner({ type: 'success', msg: 'Flagged locally for this session.' });
  };

  const startEdit = () => {
    if (!norm) return;
    const isMulti = norm.kind === 'MULTI' || norm.correctIndices.length > 1;
    setDraft({
      questionText: norm.text,
      explanation: norm.explanation,
      difficulty: norm.difficulty || 'medium',
      topic: norm.topic || '',
      options: norm.options.length ? norm.options.map((o) => ({ ...o })) : [{ text: '', correct: false }],
      isMulti,
      correctAnswer: norm.correctAnswer ?? '',
      tolerance: current.tolerance ?? ''
    });
    setImgFile(null);
    setEditMode(true);
  };

  const toggleCorrect = (i) => {
    setDraft((d) => {
      if (!d) return d;
      const options = d.options.map((o, j) => ({
        ...o,
        correct: d.isMulti ? (j === i ? !o.correct : o.correct) : j === i
      }));
      return { ...d, options };
    });
  };

  const saveEdit = async () => {
    if (!current || !draft) return;
    if (!draft.questionText.trim()) {
      setBanner({ type: 'error', msg: 'Question text cannot be empty.' });
      return;
    }
    try {
      const id = current.id ?? current._id;
      if (imgFile) {
        try {
          await uploadImage({ id, file: imgFile }).unwrap();
        } catch {
          setBanner({ type: 'error', msg: 'Image upload failed — text changes not saved.' });
          return;
        }
      }
      const body = {
        questionText: draft.questionText,
        text: draft.questionText,
        latex: draft.questionText,
        explanation: draft.explanation,
        difficulty: draft.difficulty,
        topic: draft.topic,
        options: draft.options.map((o, i) => ({
          label: o.label || 'ABCD'[i] || `OPT${i + 1}`,
          optionText: o.text,
          optionLatex: o.latex || o.text,
          correct: o.correct
        })),
        correctAnswer: draft.correctAnswer,
        tolerance: draft.tolerance === '' ? undefined : Number(draft.tolerance)
      };
      await updateQuestion({ id, ...body }).unwrap();
      setEditMode(false);
      setImgFile(null);
      setBanner({ type: 'success', msg: 'Question updated.' });
      refetch();
    } catch {
      setBanner({ type: 'error', msg: 'Update failed.' });
    }
  };

  const removeImage = async () => {
    if (!current) return;
    try {
      await updateQuestion({ id: current.id ?? current._id, imageUrl: null, removeImage: true }).unwrap();
      setBanner({ type: 'success', msg: 'Image removed.' });
      refetch();
    } catch {
      setBanner({ type: 'error', msg: 'Could not remove image.' });
    }
  };

  const exams = Array.isArray(examsData) ? examsData : (examsData?.items || examsData?.exams || []);
  const progress = list.length ? Math.round(((safeIdx + 1) / list.length) * 100) : 0;

  const dotClass = (q, i) => {
    const id = qid(q, i);
    const st = statusOf(q);
    let c = 'dot unvisited';
    if (flagged.includes(id)) c = 'dot flagged';
    else if (st === 'APPROVED') c = 'dot approved';
    else if (seen.includes(id)) c = 'dot seen';
    if (i === safeIdx) c += ' current';
    return c;
  };

  return (
    <div className="admin-page">
      <h2 className="admin-title">Review questions</h2>
      <p className="admin-sub">One-by-one student-view preview. Use ← → keys to move.</p>

      <div className="card review-toolbar">
        <label className="field review-paper-field">
          Paper ID
          <input
            className="input"
            value={paperInput}
            onChange={(e) => setPaperInput(e.target.value)}
            placeholder="paper_123"
          />
        </label>
        <button className="btn" onClick={loadPaper}>Load paper</button>
        {exams.length > 0 && (
          <select
            className="select"
            value={paperId}
            onChange={(e) => e.target.value && setSearchParams({ paperId: e.target.value })}
            aria-label="Pick paper from exams"
          >
            <option value="">…or pick from exams</option>
            {exams.map((ex) => {
              const pid = ex.paperId || ex.papers?.[0]?.id || ex.papers?.[0]?.paperId;
              if (!pid) return null;
              return <option key={ex.id ?? ex._id} value={pid}>{ex.name || ex.id} — {pid}</option>;
            })}
          </select>
        )}
      </div>

      {banner && <div className={`banner ${banner.type}`}>{banner.msg}</div>}

      {!paperId && (
        <div className="card empty">
          <h3>No paper loaded</h3>
          <p>Enter a paper ID above (or pick one from the exam dropdown) to start reviewing.</p>
        </div>
      )}

      {(isLoading || isFetching) && paperId && (
        <div className="skeleton-list">
          <div className="skeleton tall" />
          <div className="skeleton" />
        </div>
      )}

      {isError && paperId && (
        <div className="banner error">
          Could not load questions for this paper.{' '}
          <button className="btn btn-ghost" onClick={() => refetch()}>Retry</button>
        </div>
      )}

      {!isLoading && !isError && paperId && list.length === 0 && (
        <div className="card empty">
          <h3>No questions found</h3>
          <p>This paper has no saved questions yet. Upload a paper first.</p>
        </div>
      )}

      {current && norm && (
        <div className="review-wrap">
          <div className="review-meta">
            <span className={`status-pill ${statusOf(current).toLowerCase()}`}>{statusOf(current)}</span>
            <span className="muted">Question {safeIdx + 1} of {list.length}</span>
          </div>

          <div className="progress" aria-label="Review progress">
            <div className="progress-bar" style={{ width: `${progress}%` }} />
          </div>

          <div className="palette" role="listbox" aria-label="Question palette">
            {list.map((q, i) => (
              <button
                key={qid(q, i)}
                role="option"
                aria-selected={i === safeIdx}
                title={`Q${i + 1}: ${statusOf(q)}`}
                className={dotClass(q, i)}
                onClick={() => jump(i)}
              >
                {i + 1}
              </button>
            ))}
          </div>

          <div ref={cardRef}>
            <ExamQuestionView question={current} index={safeIdx} mode="preview" />
          </div>

          <div className="review-nav">
            <button className="btn btn-ghost" onClick={() => go(-1)} disabled={safeIdx === 0}>← Prev</button>
            <button className="btn btn-ghost" onClick={() => go(1)} disabled={safeIdx >= list.length - 1}>Next →</button>
          </div>

          {!editMode ? (
            <div className="card verify-row">
              <div className="admin-actions">
                <button className="btn btn-green" onClick={() => doVerify('APPROVED')} disabled={verifying}>Approve</button>
                <button className="btn btn-danger-ghost" onClick={() => doVerify('REJECTED')} disabled={verifying}>Reject</button>
                <button className="btn btn-ghost" onClick={doFlag}>Flag for review</button>
                <button className="btn btn-ghost" onClick={startEdit}>Edit</button>
              </div>
            </div>
          ) : (
            draft && (
              <div className="card edit-grid">
                <h3>Edit question {safeIdx + 1}</h3>
                <label className="field">Question text
                  <textarea className="textarea" value={draft.questionText} onChange={(e) => setDraft({ ...draft, questionText: e.target.value })} />
                </label>
                {norm.kind !== 'INTEGER' ? (
                  <>
                    <label className="check-row">
                      <input type="checkbox" checked={draft.isMulti} onChange={(e) => setDraft({ ...draft, isMulti: e.target.checked })} />
                      Multiple correct answers
                    </label>
                    {draft.options.map((o, i) => (
                      <div key={i} className="opt-edit">
                        <input
                          type={draft.isMulti ? 'checkbox' : 'radio'}
                          name="correct-opt"
                          checked={!!o.correct}
                          onChange={() => toggleCorrect(i)}
                          aria-label={`Mark option ${i + 1} correct`}
                        />
                        <input
                          className="input"
                          value={o.text}
                          onChange={(e) => setDraft({
                            ...draft,
                            options: draft.options.map((x, j) => (j === i ? { ...x, text: e.target.value } : x))
                          })}
                          placeholder={`Option ${'ABCD'[i] || i + 1}`}
                        />
                      </div>
                    ))}
                  </>
                ) : (
                  <div className="form-grid">
                    <label className="field">Correct answer
                      <input className="input" value={draft.correctAnswer} onChange={(e) => setDraft({ ...draft, correctAnswer: e.target.value })} />
                    </label>
                    <label className="field">Tolerance
                      <input className="input" type="number" step="any" value={draft.tolerance} onChange={(e) => setDraft({ ...draft, tolerance: e.target.value })} />
                    </label>
                  </div>
                )}
                <label className="field">Explanation
                  <textarea className="textarea" value={draft.explanation} onChange={(e) => setDraft({ ...draft, explanation: e.target.value })} />
                </label>
                <div className="form-grid">
                  <label className="field">Difficulty
                    <select className="select" value={draft.difficulty} onChange={(e) => setDraft({ ...draft, difficulty: e.target.value })}>
                      <option value="easy">Easy</option>
                      <option value="medium">Medium</option>
                      <option value="hard">Hard</option>
                    </select>
                  </label>
                  <label className="field">Topic
                    <input className="input" value={draft.topic} onChange={(e) => setDraft({ ...draft, topic: e.target.value })} />
                  </label>
                </div>
                <div className="form-grid">
                  <label className="field">Replace image
                    <input type="file" accept="image/*" onChange={(e) => setImgFile(e.target.files?.[0] || null)} />
                  </label>
                  <div className="admin-actions inline">
                    <button className="btn btn-danger-ghost" onClick={removeImage}>Remove image</button>
                  </div>
                </div>
                <div className="admin-actions">
                  <button className="btn btn-green" onClick={saveEdit} disabled={savingEdit || uploadingImg}>
                    {savingEdit || uploadingImg ? 'Saving…' : 'Save changes'}
                  </button>
                  <button className="btn btn-ghost" onClick={() => { setEditMode(false); setDraft(null); setImgFile(null); }}>
                    Cancel
                  </button>
                </div>
              </div>
            )
          )}
        </div>
      )}
    </div>
  );
}
