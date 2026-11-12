import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import ExamQuestionView, { normalizeQuestion } from '../../components/ExamQuestionView.jsx';
import {
  useSavedQuestionsQuery,
  useVerifyQuestionMutation,
  useUpdateQuestionMutation,
  useUploadQuestionImageMutation,
  useUploadOptionImageMutation,
  useSubmitPaperMutation,
  useListExamsQuery,
  optionImageSrc
} from '../../services/examApi.js';

function asList(data) {
  if (Array.isArray(data)) return data;
  return data?.items || data?.questions || data?.content || [];
}

const statusOf = (q) => String(q?.status ?? q?.verificationStatus ?? 'PENDING').toUpperCase();
const qid = (q, i) => String(q?.id ?? q?._id ?? `idx-${i}`);

export default function ReviewQuestions() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
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
  const [uploadOptionImage, { isLoading: uploadingOptImg }] = useUploadOptionImageMutation();
  const [submitPaper, { isLoading: submitting }] = useSubmitPaperMutation();

  const list = useMemo(() => asList(data), [data]);
  const [idx, setIdx] = useState(0);
  const [seen, setSeen] = useState([]);
  const [flagged, setFlagged] = useState([]);
  const [banner, setBanner] = useState(null);
  const [editMode, setEditMode] = useState(false);
  const [draft, setDraft] = useState(null);
  const [imgFile, setImgFile] = useState(null);
  const [optImgFiles, setOptImgFiles] = useState({});
  const [done, setDone] = useState(false);
  const cardRef = useRef(null);

  const safeIdx = list.length ? Math.min(idx, list.length - 1) : 0;
  const current = list[safeIdx];
  const currentId = current ? qid(current, safeIdx) : null;
  const norm = useMemo(() => (current ? normalizeQuestion(current) : null), [current]);

  useEffect(() => { setIdx(0); setSeen([]); setEditMode(false); setDraft(null); setDone(false); setOptImgFiles({}); }, [paperId]);

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
    setDone(false);
    setEditMode(false);
    setIdx((i) => Math.min(Math.max(i + d, 0), Math.max(list.length - 1, 0)));
  };
  const jump = (i) => { setDone(false); setEditMode(false); setIdx(i); };

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
      setBanner({ type: 'error', msg: 'Could not save. Please try again.' });
    }
  };

  const approveAndNext = async () => {
    if (!current) return;
    try {
      await verify({ id: current.id ?? current._id, status: 'APPROVED' }).unwrap();
      const res = await refetch();
      const fresh = asList(res?.data ?? data);
      const len = fresh.length || list.length;
      if (safeIdx >= len - 1) {
        setDone(true);
        setBanner({ type: 'success', msg: 'All questions reviewed. Submit the paper below to finish.' });
      } else {
        setEditMode(false);
        setIdx((i) => Math.min(i + 1, Math.max(len - 1, 0)));
        setBanner({ type: 'success', msg: 'Question approved.' });
      }
    } catch {
      setBanner({ type: 'error', msg: 'Could not approve. Please try again.' });
    }
  };

  const doSubmit = async () => {
    if (!paperId || pendingCount > 0) return;
    try {
      const res = await submitPaper(paperId).unwrap();
      setBanner({ type: 'success', msg: `Paper submitted with ${res.submitted ?? list.length} questions.` });
      setTimeout(() => navigate('/admin/exams'), 1200);
    } catch (e) {
      if (e?.status === 409) {
        setBanner({ type: 'error', msg: `${e?.data?.pending ?? 'Some'} questions still need review.` });
        refetch();
      } else {
        setBanner({ type: 'error', msg: 'Could not submit. Please try again.' });
      }
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
    const rawOpts = Array.isArray(current.options) ? current.options : [];
    setDraft({
      questionText: norm.text,
      explanation: norm.explanation,
      difficulty: norm.difficulty || 'medium',
      topic: norm.topic || '',
      options: norm.options.length
        ? norm.options.map((o, i) => ({
            ...o,
            image: optionImageSrc(rawOpts[i]),
            optionImageBase64: rawOpts[i]?.optionImageBase64 ?? rawOpts[i]?.option_image_base64 ?? null
          }))
        : [{ text: '', correct: false, image: null, optionImageBase64: null }],
      isMulti,
      correctAnswer: norm.correctAnswer ?? '',
      tolerance: current.tolerance ?? ''
    });
    setImgFile(null);
    setOptImgFiles({});
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
        options: draft.options.map((o, i) => {
          const opt = {
            label: o.label || 'ABCD'[i] || `OPT${i + 1}`,
            optionText: o.text,
            optionLatex: o.latex || o.text,
            correct: o.correct
          };
          // Keep the stored figure when only text changes (backend only
          // overwrites the figure when this key is present).
          if (o.optionImageBase64) opt.optionImageBase64 = o.optionImageBase64;
          return opt;
        }),
        correctAnswer: draft.correctAnswer,
        tolerance: draft.tolerance === '' ? undefined : Number(draft.tolerance)
      };
      await updateQuestion({ id, ...body }).unwrap();
      // Option figure replacements go through their own endpoint AFTER the
      // text save, so the save above cannot wipe them.
      let optImgFailed = false;
      for (const [i, f] of Object.entries(optImgFiles)) {
        if (!f) continue;
        const o = draft.options[Number(i)];
        const label = o?.label || 'ABCD'[Number(i)] || `OPT${Number(i) + 1}`;
        try {
          await uploadOptionImage({ id, label, file: f }).unwrap();
        } catch {
          optImgFailed = true;
        }
      }
      setEditMode(false);
      setImgFile(null);
      setOptImgFiles({});
      setBanner(optImgFailed
        ? { type: 'error', msg: 'Question updated, but an option figure failed to upload.' }
        : { type: 'success', msg: 'Question updated.' });
      refetch();
    } catch {
      setBanner({ type: 'error', msg: 'Could not save changes. Please try again.' });
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
  const reviewedCount = list.filter((q) => statusOf(q) !== 'PENDING').length;
  const pendingCount = list.length - reviewedCount;

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
          Paper reference
          <input
            className="input"
            value={paperInput}
            onChange={(e) => setPaperInput(e.target.value)}
            placeholder="Enter paper reference"
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
          <p>Enter a paper reference above (or pick one from the list) to start reviewing.</p>
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
            <span className="muted">Reviewed {reviewedCount} of {list.length}</span>
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

          {done ? (
            <div className="card empty">
              <h3>All questions reviewed</h3>
              <p>Reviewed {reviewedCount} of {list.length}. Submit the paper below to finish.</p>
              <button className="btn btn-ghost" onClick={() => setDone(false)}>Back to questions</button>
            </div>
          ) : (
            <div ref={cardRef}>
              <ExamQuestionView question={current} index={safeIdx} mode="preview" />
            </div>
          )}

          <div className="review-nav">
            <button className="btn btn-ghost" onClick={() => go(-1)} disabled={safeIdx === 0}>← Prev</button>
            <button className="btn btn-ghost" onClick={() => go(1)} disabled={safeIdx >= list.length - 1}>Next →</button>
          </div>

          <div className="card submit-bar">
            <span className="muted">
              Reviewed {reviewedCount} of {list.length}
              {pendingCount > 0 ? ` · ${pendingCount} still waiting` : ' · ready to submit'}
            </span>
            <button
              className="btn btn-green"
              onClick={doSubmit}
              disabled={pendingCount > 0 || submitting || list.length === 0}
              title={pendingCount > 0 ? 'Review every question first' : 'Submit paper'}
            >
              {submitting ? 'Submitting…' : 'Submit paper'}
            </button>
          </div>

          {!editMode ? (
            <div className="card verify-row">
              <div className="admin-actions">
                <button className="btn btn-green" onClick={approveAndNext} disabled={verifying}>Approve &amp; Next</button>
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
                      <div key={i} className="opt-fig">
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
                        <div>
                          {o.image
                            ? <img src={o.image} alt={`Option ${'ABCD'[i] || i + 1} figure`} />
                            : <span className="muted">No figure</span>}
                          <input
                            type="file"
                            accept="image/*"
                            onChange={(e) => setOptImgFiles((m) => ({ ...m, [i]: e.target.files?.[0] || null }))}
                            aria-label={`Replace option ${'ABCD'[i] || i + 1} figure`}
                          />
                        </div>
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
                  <button className="btn btn-green" onClick={saveEdit} disabled={savingEdit || uploadingImg || uploadingOptImg}>
                    {savingEdit || uploadingImg || uploadingOptImg ? 'Saving…' : 'Save changes'}
                  </button>
                  <button className="btn btn-ghost" onClick={() => { setEditMode(false); setDraft(null); setImgFile(null); setOptImgFiles({}); }}>
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
