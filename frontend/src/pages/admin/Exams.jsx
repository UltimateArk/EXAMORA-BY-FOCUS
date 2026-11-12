import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useListExamsQuery, usePublishExamMutation } from '../../services/scheduleApi.js';

function asList(data) {
  if (Array.isArray(data)) return data;
  return data?.items || data?.exams || data?.content || [];
}

export default function Exams() {
  const { data, isLoading, isError, refetch } = useListExamsQuery();
  const [publishExam] = usePublishExamMutation();
  const [overrides, setOverrides] = useState({});
  const [banner, setBanner] = useState(null);
  const navigate = useNavigate();
  const exams = asList(data);

  const statusOf = (ex) => overrides[ex.id ?? ex._id] || String(ex.status || 'DRAFT').toUpperCase();

  const togglePublish = async (ex) => {
    const id = ex.id ?? ex._id;
    if (statusOf(ex) === 'PUBLISHED') {
      // Backend has no unpublish — keep local only
      setOverrides((o) => ({ ...o, [id]: 'DRAFT' }));
      setBanner({ type: 'info', msg: 'Unpublished on this device. The server still shows it as published.' });
      return;
    }
    try {
      await publishExam(id).unwrap();
      setBanner({ type: 'success', msg: 'Exam published.' });
    } catch {
      setOverrides((o) => ({ ...o, [id]: 'PUBLISHED' }));
      setBanner({ type: 'info', msg: 'Could not reach the server — change kept on this device for now.' });
    }
  };

  const viewExam = (ex) => {
    const paperId = ex.paperId || ex.papers?.[0]?.id || ex.papers?.[0]?.paperId;
    if (paperId) navigate(`/admin/review?paperId=${paperId}`);
    else navigate(`/admin/upload?examId=${ex.id ?? ex._id}`);
  };

  return (
    <div className="admin-page">
      <h2 className="admin-title">Exams</h2>
      <p className="admin-sub">All scheduled exams, their status and quick actions.</p>
      {banner && <div className={`banner ${banner.type}`}>{banner.msg}</div>}

      {isLoading && (
        <div className="skeleton-list">
          {[0, 1, 2].map((i) => <div key={i} className="skeleton" />)}
        </div>
      )}
      {isError && (
        <div className="banner error">
          Could not load exams. <button className="btn btn-ghost" onClick={() => refetch()}>Retry</button>
        </div>
      )}
      {!isLoading && !isError && exams.length === 0 && (
        <div className="card empty">
          <h3>No exams yet</h3>
          <p>Schedule your first exam to see it here.</p>
          <button className="btn" onClick={() => navigate('/admin')}>Schedule exam</button>
        </div>
      )}

      {exams.length > 0 && (
        <>
          <div className="exams-table-wrap card">
            <table className="exams-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Type</th>
                  <th>Date</th>
                  <th>Status</th>
                  <th>Registrations</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {exams.map((ex) => {
                  const id = ex.id ?? ex._id;
                  return (
                    <tr key={id}>
                      <td><b>{ex.name || `Exam ${id}`}</b></td>
                      <td>{ex.examType || '—'}</td>
                      <td>{ex.date || '—'}</td>
                      <td><span className={`status-pill ${statusOf(ex).toLowerCase()}`}>{statusOf(ex)}</span></td>
                      <td>{ex.registrations ?? ex._count?.registrations ?? '—'}</td>
                      <td>
                        <div className="admin-actions inline">
                          <button className="btn btn-ghost" onClick={() => viewExam(ex)}>View</button>
                          <button className="btn" onClick={() => togglePublish(ex)}>
                            {statusOf(ex) === 'PUBLISHED' ? 'Unpublish' : 'Publish'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          <div className="exams-cards">
            {exams.map((ex) => {
              const id = ex.id ?? ex._id;
              return (
                <article key={id} className="card exam-card">
                  <div className="exam-card-head">
                    <b>{ex.name || `Exam ${id}`}</b>
                    <span className={`status-pill ${statusOf(ex).toLowerCase()}`}>{statusOf(ex)}</span>
                  </div>
                  <p className="muted">{ex.examType || '—'} · {ex.date || 'No date'} · Reg: {ex.registrations ?? '—'}</p>
                  <div className="admin-actions">
                    <button className="btn btn-ghost" onClick={() => viewExam(ex)}>View</button>
                    <button className="btn" onClick={() => togglePublish(ex)}>
                      {statusOf(ex) === 'PUBLISHED' ? 'Unpublish' : 'Publish'}
                    </button>
                  </div>
                </article>
              );
            })}
          </div>
        </>
      )}
    </div>
  );
}
