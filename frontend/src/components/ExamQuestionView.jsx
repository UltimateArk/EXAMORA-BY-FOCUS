import { useState } from 'react';
import './ExamQuestionView.css';
import { questionImageSrc } from '../services/examApi.js';

export function normalizeQuestion(q = {}) {
  const text = q.questionText ?? q.text ?? q.stem ?? q.question ?? '';
  const latex = q.latex ?? q.statement_latex ?? '';
  const explanation = q.explanation ?? q.solution ?? q.answerExplanation ?? '';
  let options = [];
  if (Array.isArray(q.options)) {
    options = q.options.map((o, i) => {
      if (typeof o === 'string') return { label: LETTERS[i] || String(i + 1), text: o, latex: '', correct: false };
      return {
        label: o.label ?? LETTERS[i] ?? String(i + 1),
        text: o.optionText ?? o.text ?? o.value ?? '',
        latex: o.optionLatex ?? o.latex ?? '',
        correct: Boolean(o.correct ?? o.isCorrect ?? o.is_correct ?? false)
      };
    });
  }
  const upper = String(q.type ?? q.questionType ?? q.kind ?? '').toUpperCase();
  let kind = 'MCQ';
  if (upper.includes('INT') || upper.includes('NUMERIC') || upper.includes('NUM')) kind = 'INTEGER';
  else if (upper.includes('MULTI') || upper.includes('MSQ') || upper.includes('MULTIPLE')) kind = 'MULTI';
  else if (options.length === 0) {
    kind = (q.integerAnswer != null || q.correctAnswer != null || q.answer != null) ? 'INTEGER' : 'MCQ';
  }
  let correctIndex = -1;
  let correctIndices = [];
  if (Array.isArray(q.correctIndices)) {
    correctIndices = q.correctIndices;
    correctIndex = correctIndices[0] ?? -1;
  } else if (q.correctIndex != null && q.correctIndex !== -1) {
    correctIndex = Number(q.correctIndex);
    correctIndices = [correctIndex];
  } else if (q.correctOption != null) {
    correctIndex = Number(q.correctOption);
    correctIndices = [correctIndex];
  } else {
    options.forEach((o, i) => { if (o.correct) correctIndices.push(i); });
    correctIndex = correctIndices[0] ?? -1;
  }
  const correctAnswer = q.integerAnswer ?? q.correctAnswer ?? q.answer ?? q.numericAnswer ?? '';
  // Prefer embedded base64 (DB) over served file URL; both handled by helper.
  const image = questionImageSrc(q)
    ?? q.imageUrl ?? q.image ?? q.image_url ?? q.figureUrl ?? null;
  return {
    id: q.id ?? q._id,
    paperId: q.paperId ?? q.paper_id,
    text, latex, explanation, options, kind, correctIndex, correctIndices, correctAnswer, image,
    marks: q.marks ?? q.marksCorrect ?? q.mark ?? 4,
    negativeMarks: q.negativeMarks ?? q.negative ?? 0,
    difficulty: q.difficulty ?? '',
    topic: q.topic ?? '',
    status: q.status ?? q.verificationStatus ?? 'PENDING'
  };
}

function renderRichText(text) {
  // Display blocks first: $$...$$ and \[...\] -> block; \(...\) and $...$ -> inline code.
  const parts = String(text ?? '').split(/(\$\$[\s\S]+?\$\$|\\\[[\s\S]+?\\\])/g);
  return parts.map((p, i) => {
    const isBlock = (p.startsWith('$$') && p.endsWith('$$') && p.length >= 4)
      || (p.startsWith('\\[') && p.endsWith('\\]'));
    if (isBlock) {
      const inner = p.startsWith('$$') ? p.slice(2, -2) : p.slice(2, -2);
      return <pre key={i} className="eqv-latex-block">{inner}</pre>;
    }
    const inline = p.split(/(\\\([\s\S]+?\\\)|\$[^$\n]+?\$)/g);
    return (
      <span key={i}>
        {inline.map((s, j) => {
          if (s.startsWith('\\(') && s.endsWith('\\)')) {
            return <code key={j} className="eqv-latex-inline">{s.slice(2, -2)}</code>;
          }
          if (s.startsWith('$') && s.endsWith('$') && s.length >= 3) {
            return <code key={j} className="eqv-latex-inline">{s.slice(1, -1)}</code>;
          }
          return <span key={j}>{s}</span>;
        })}
      </span>
    );
  });
}

const LETTERS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';

export default function ExamQuestionView({
  question,
  index = 0,
  mode = 'preview',
  selected = null,
  onSelect = null,
  inputValue = '',
  onInputChange = null
}) {
  const n = normalizeQuestion(question);
  const [lightbox, setLightbox] = useState(false);
  const isInteger = n.kind === 'INTEGER';
  const typeBadge = n.kind === 'MULTI' ? 'MULTIPLE CHOICE' : n.kind === 'INTEGER' ? 'INTEGER / NUMERIC' : 'SINGLE CHOICE';

  return (
    <div className="eqv-card">
      <div className="eqv-top">
        <span className="eqv-qnum">Q{index + 1}</span>
        <span className="eqv-marks">+{n.marks}{n.negativeMarks ? ` / -${n.negativeMarks}` : ''}</span>
        <span className="eqv-type">{typeBadge}</span>
      </div>

      <div className="eqv-text">{renderRichText(n.text || '(no question text)')}</div>

      {mode === 'preview' && n.latex && (
        <details className="eqv-latex-src">
          <summary>Math source</summary>
          <pre className="eqv-latex-block">{n.latex}</pre>
        </details>
      )}

      {n.image && (
        <div className="eqv-img-wrap">
          <img
            src={n.image}
            alt={`Question ${index + 1} figure`}
            className="eqv-img"
            onClick={() => setLightbox(true)}
            loading="lazy"
          />
          <span className="eqv-zoom-hint">Click to zoom</span>
        </div>
      )}

      {!isInteger ? (
        <div className="eqv-options">
          {n.options.map((o, i) => {
            const isCorrect = mode === 'preview' && (n.correctIndices.includes(i) || n.correctIndex === i);
            const isPicked = mode === 'student' && selected === i;
            return (
              <button
                key={i}
                type="button"
                className={`eqv-option${isCorrect ? ' is-correct' : ''}${isPicked ? ' is-picked' : ''}`}
                onClick={mode === 'student' && onSelect ? () => onSelect(i) : undefined}
                disabled={mode !== 'student'}
              >
                <span className="eqv-letter">{o.label || LETTERS[i] || i + 1}</span>
                <span className="eqv-opt-text">{renderRichText(o.latex || o.text)}</span>
                {isCorrect && <span className="eqv-correct-badge">Correct</span>}
              </button>
            );
          })}
        </div>
      ) : (
        <div className="eqv-integer">
          {mode === 'preview' ? (
            <div className="eqv-answer-box">
              <span className="eqv-answer-label">Correct answer:</span>
              <span className="eqv-answer-value">{String(n.correctAnswer ?? '')}</span>
            </div>
          ) : (
            <label className="eqv-field">
              Your answer
              <input
                className="input"
                inputMode="decimal"
                value={inputValue}
                onChange={(e) => onInputChange && onInputChange(e.target.value)}
                placeholder="Enter numeric answer"
              />
            </label>
          )}
        </div>
      )}

      {lightbox && n.image && (
        <div className="eqv-lightbox" onClick={() => setLightbox(false)} role="dialog" aria-label="Image zoom">
          <img src={n.image} alt={`Question ${index + 1} enlarged`} />
          <span className="eqv-lightbox-close">Click anywhere to close</span>
        </div>
      )}
    </div>
  );
}
