import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import {
  useUploadPdfMutation,
  useExtractTextMutation,
  useExtractQuestionImagesMutation,
  useSaveExtractedMutation,
  useAttachPaperMutation,
  useGetExamQuery,
  useSubjectsQuery,
  questionImageUrl
} from '../../services/examApi.js';

export default function UploadPaper() {
  const [searchParams] = useSearchParams();
  const examId = searchParams.get('examId') || '';
  const navigate = useNavigate();

  const [file, setFile] = useState(null);
  const [paperId, setPaperId] = useState('');
  const [meta, setMeta] = useState({ exam: 'JEE', subject: 'physics', topic: '', difficulty: 'medium' });
  const [text, setText] = useState('');
  const [pageCount, setPageCount] = useState(0);
  const [images, setImages] = useState([]);
  const [banner, setBanner] = useState(null);
  const [savedCount, setSavedCount] = useState(null);

  const [uploadPdf, { isLoading: uploading }] = useUploadPdfMutation();
  const [extractText, { isLoading: extracting }] = useExtractTextMutation();
  const [extractImages, { isLoading: extractingImg }] = useExtractQuestionImagesMutation();
  const [saveExtracted, { isLoading: saving }] = useSaveExtractedMutation();
  const [attachPaper, { isLoading: attaching }] = useAttachPaperMutation();
  const { data: exam } = useGetExamQuery(examId, { skip: !examId });
  const { data: subjectsData } = useSubjectsQuery();

  const ok = (msg) => setBanner({ type: 'success', msg });
  const err = (msg) => setBanner({ type: 'error', msg });

  const resolveSubjectId = () => {
    const list = Array.isArray(subjectsData) ? subjectsData : (subjectsData?.items || []);
    return list.find((s) => String(s.name || '').toLowerCase() === String(meta.subject).toLowerCase())?.id;
  };

  const onUpload = async () => {
    if (!file) return err('Choose a PDF first.');
    try {
      const res = await uploadPdf(file).unwrap();
      setPaperId(res.paperId);
      setSavedCount(null);
      ok(`PDF uploaded (paper ${res.paperId}).`);
    } catch { err('Upload failed. Is exam API running on :8080?'); }
  };

  const onExtractText = async () => {
    try {
      const res = await extractText(paperId).unwrap();
      const pages = res.pages || [];
      setPageCount(pages.length);
      setText(pages.map((p) => `--- Page ${p.pageNumber} ---\n${p.extractedText || ''}`).join('\n\n'));
      ok(`Text extracted via Nougat (${pages.length} pages).`);
    } catch { err('Text extraction failed.'); }
  };

  const onExtractImages = async () => {
    try {
      const res = await extractImages(paperId).unwrap();
      const imgs = (res.images || []).map((im) => ({
        ...im,
        url: questionImageUrl(paperId, im.imagePath)
      }));
      setImages(imgs);
      ok(`${imgs.length} page images, ${(res.questions || []).length} question crops via PDFBox.`);
    } catch { err('Image extraction failed.'); }
  };

  const onSave = async () => {
    try {
      const topicNum = meta.topic.trim() !== '' && !Number.isNaN(Number(meta.topic)) ? Number(meta.topic) : undefined;
      const res = await saveExtracted({
        paperId,
        examType: meta.exam,
        subjectId: resolveSubjectId(),
        topicId: topicNum,
        difficulty: String(meta.difficulty).toUpperCase()
      }).unwrap();
      setSavedCount(res.savedCount ?? 0);
      if (examId && paperId) {
        try {
          await attachPaper({ examId, paperId }).unwrap();
          ok(`Saved ${res.savedCount ?? 0} questions and attached to exam ${examId}.`);
        } catch {
          ok(`Saved ${res.savedCount ?? 0} (paper ${paperId}). Attach to exam failed — retry from Exams.`);
        }
      } else {
        ok(`Saved ${res.savedCount ?? 0} questions. Awaiting verification.`);
      }
    } catch { err('Save failed.'); }
  };

  return (
    <div className="admin-page">
      <h2 className="admin-title">Upload question paper</h2>
      {examId ? (
        <div className="banner success">
          Linked exam: {exam?.name || examId}{exam?.date ? ` · ${exam.date}` : ''}
        </div>
      ) : (
        <div className="banner info">
          No exam linked. <Link to="/admin" style={{ fontWeight: 800, textDecoration: 'underline' }}>Schedule an exam first</Link> or continue standalone.
        </div>
      )}
      {banner && <div className={`banner ${banner.type}`}>{banner.msg}</div>}
      {savedCount != null && paperId && (
        <div className="admin-actions">
          <button className="btn" onClick={() => navigate(`/admin/review?paperId=${paperId}`)}>
            Continue to review questions
          </button>
        </div>
      )}

      <div className="admin-form">
        <section className="card form-section">
          <span className="pill">Step 1 — Upload PDF</span>
          <input type="file" accept="application/pdf" onChange={(e) => setFile(e.target.files?.[0] || null)} />
          <div className="admin-actions">
            <button className="btn" onClick={onUpload} disabled={uploading}>{uploading ? 'Uploading…' : 'Upload'}</button>
          </div>
          {paperId && <p className="muted">paperId: {paperId}</p>}
        </section>

        <section className="card form-section">
          <span className="pill">Step 2 — Details</span>
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
          <span className="pill">Step 3 — Extract &amp; preview</span>
          <div className="admin-actions">
            <button className="btn btn-ghost" onClick={onExtractText} disabled={!paperId || extracting}>{extracting ? 'Extracting…' : 'Extract text (Nougat)'}</button>
            <button className="btn btn-ghost" onClick={onExtractImages} disabled={!paperId || extractingImg}>{extractingImg ? 'Extracting…' : 'Preview images (PDFBox)'}</button>
          </div>
          <textarea className="textarea" value={text} onChange={(e) => setText(e.target.value)} placeholder="Extracted text appears here…" />
          {pageCount > 0 && <p className="muted">{pageCount} pages extracted.</p>}
          <div className="img-row">
            {images.map((im, i) => <img key={im.imagePath || i} src={im.url} alt={`page ${im.pageNumber ?? i}`} />)}
          </div>
        </section>

        <section className="card form-section">
          <span className="pill">Step 4 — Save{examId ? ' & attach' : ''}</span>
          <div className="admin-actions">
            <button className="btn btn-green" onClick={onSave} disabled={saving || attaching}>
              {saving || attaching ? 'Saving…' : examId ? 'Save & attach to exam' : 'Save extracted'}
            </button>
          </div>
        </section>
      </div>
    </div>
  );
}
