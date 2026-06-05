import { useState, useEffect } from 'react'
import { api } from '../api'

const STATUS_CONFIG = {
  pending:  { label: 'Pending',  color: 'var(--muted)',   bg: 'var(--bg3)',    icon: '⏳' },
  applied:  { label: 'Applied',  color: '#60a5fa',        bg: '#0d1b2a',       icon: '📤' },
  rejected: { label: 'Rejected', color: '#f87171',        bg: '#2a0d0d',       icon: '❌' },
  selected: { label: 'Selected', color: 'var(--green)',   bg: '#0d2a0d',       icon: '✅' },
}

const STATUS_ORDER = ['pending', 'applied', 'rejected', 'selected']

/**
 * Application Tracker — shows all applications for a user with status pipeline.
 */
export default function ApplicationTracker({ userId, onClose }) {
  const [applications, setApplications] = useState([])
  const [loading, setLoading] = useState(true)
  const [statusFilter, setStatusFilter] = useState('all')
  const [expandedId, setExpandedId] = useState(null)

  useEffect(() => {
    loadApplications()
  }, [userId])

  async function loadApplications() {
    setLoading(true)
    try {
      const data = await api.get(`/applications/user/${userId}`)
      setApplications(data)
    } catch (e) { console.error(e) }
    setLoading(false)
  }

  async function updateStatus(appId, status) {
    try {
      await api.put(`/applications/${appId}`, { status })
      setApplications(as => as.map(a => a.id === appId ? { ...a, status } : a))
    } catch (e) { console.error(e) }
  }

  async function deleteApplication(appId) {
    if (!confirm('Remove this application?')) return
    try {
      await api.delete(`/applications/${appId}`)
      setApplications(as => as.filter(a => a.id !== appId))
    } catch (e) { console.error(e) }
  }

  const displayed = statusFilter === 'all'
    ? applications
    : applications.filter(a => a.status === statusFilter)

  // Stats
  const counts = STATUS_ORDER.reduce((acc, s) => {
    acc[s] = applications.filter(a => a.status === s).length
    return acc
  }, {})

  function timeAgo(dateStr) {
    if (!dateStr) return ''
    const diff = Date.now() - new Date(dateStr).getTime()
    const d = Math.floor(diff / 86400000)
    if (d > 0) return `${d}d ago`
    const h = Math.floor(diff / 3600000)
    if (h > 0) return `${h}h ago`
    return 'just now'
  }

  return (
    <div style={{
      position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      zIndex: 200, padding: 24,
    }} onClick={onClose}>
      <div style={{
        background: 'var(--bg2)', border: '1px solid var(--border)',
        borderRadius: 16, width: '100%', maxWidth: 800,
        maxHeight: '90vh', overflow: 'hidden', display: 'flex', flexDirection: 'column',
      }} onClick={e => e.stopPropagation()}>

        {/* Header */}
        <div style={{
          padding: '20px 28px', borderBottom: '1px solid var(--border)',
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        }}>
          <div style={{ fontFamily: 'var(--font-head)', fontWeight: 700, fontSize: '1.15rem' }}>
            📋 Application Tracker
          </div>
          <button onClick={onClose} style={{ background: 'none', color: 'var(--muted)', border: 'none', fontSize: '1.3rem', cursor: 'pointer' }}>✕</button>
        </div>

        {/* Pipeline stats */}
        <div style={{
          display: 'flex', padding: '16px 28px', gap: 12, borderBottom: '1px solid var(--border)',
          background: 'var(--bg1)',
        }}>
          {STATUS_ORDER.map(s => {
            const cfg = STATUS_CONFIG[s]
            return (
              <div key={s} style={{
                flex: 1, background: cfg.bg, border: `1px solid ${cfg.color}30`,
                borderRadius: 10, padding: '12px 16px', textAlign: 'center',
                cursor: 'pointer',
                outline: statusFilter === s ? `2px solid ${cfg.color}` : 'none',
                transition: 'outline 0.15s',
              }} onClick={() => setStatusFilter(statusFilter === s ? 'all' : s)}>
                <div style={{ fontSize: '1.4rem', marginBottom: 4 }}>{cfg.icon}</div>
                <div style={{ color: cfg.color, fontFamily: 'var(--font-head)', fontWeight: 700, fontSize: '1.3rem' }}>
                  {counts[s]}
                </div>
                <div style={{ color: 'var(--muted)', fontSize: '0.75rem' }}>{cfg.label}</div>
              </div>
            )
          })}
          <div style={{
            flex: 1, background: 'var(--bg3)', border: '1px solid var(--border)',
            borderRadius: 10, padding: '12px 16px', textAlign: 'center',
          }}>
            <div style={{ fontSize: '1.4rem', marginBottom: 4 }}>📊</div>
            <div style={{ color: 'var(--text)', fontFamily: 'var(--font-head)', fontWeight: 700, fontSize: '1.3rem' }}>
              {applications.length}
            </div>
            <div style={{ color: 'var(--muted)', fontSize: '0.75rem' }}>Total</div>
          </div>
        </div>

        {/* Filter row */}
        {statusFilter !== 'all' && (
          <div style={{
            padding: '10px 28px', background: `${STATUS_CONFIG[statusFilter].color}10`,
            borderBottom: '1px solid var(--border)', display: 'flex', alignItems: 'center', gap: 10,
          }}>
            <span style={{ color: STATUS_CONFIG[statusFilter].color, fontSize: '0.85rem', fontWeight: 600 }}>
              Showing: {STATUS_CONFIG[statusFilter].label}
            </span>
            <button onClick={() => setStatusFilter('all')} style={{
              background: 'none', color: 'var(--muted)', border: '1px solid var(--border)',
              borderRadius: 6, padding: '2px 10px', fontSize: '0.75rem', cursor: 'pointer',
            }}>Clear filter</button>
          </div>
        )}

        {/* List */}
        <div style={{ overflowY: 'auto', flex: 1 }}>
          {loading ? (
            <div style={{ padding: 40, textAlign: 'center', color: 'var(--muted)' }}>Loading...</div>
          ) : displayed.length === 0 ? (
            <div style={{ padding: 60, textAlign: 'center' }}>
              <div style={{ fontSize: '3rem', marginBottom: 14 }}>📭</div>
              <div style={{ fontFamily: 'var(--font-head)', fontSize: '1.1rem', marginBottom: 8 }}>
                {statusFilter === 'all' ? 'No applications yet' : `No ${STATUS_CONFIG[statusFilter].label.toLowerCase()} applications`}
              </div>
              <div style={{ color: 'var(--muted)', fontSize: '0.88rem' }}>
                {statusFilter === 'all'
                  ? 'Click "Track Application" on a job listing to start tracking.'
                  : 'Try a different status filter.'}
              </div>
            </div>
          ) : (
            displayed.map(app => (
              <AppRow
                key={app.id}
                app={app}
                expanded={expandedId === app.id}
                onToggle={() => setExpandedId(expandedId === app.id ? null : app.id)}
                onStatusChange={updateStatus}
                onDelete={deleteApplication}
                timeAgo={timeAgo}
              />
            ))
          )}
        </div>
      </div>
    </div>
  )
}

function AppRow({ app, expanded, onToggle, onStatusChange, onDelete, timeAgo }) {
  const cfg = STATUS_CONFIG[app.status] || STATUS_CONFIG.pending
  const job = app.job || {}

  return (
    <div style={{ borderBottom: '1px solid var(--border)' }}>
      {/* Main row */}
      <div style={{
        padding: '14px 28px', display: 'flex', alignItems: 'center', gap: 16,
        cursor: 'pointer', transition: 'background 0.15s',
      }}
        onMouseEnter={e => e.currentTarget.style.background = 'var(--bg3)'}
        onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
        onClick={onToggle}>

        {/* Status indicator */}
        <div style={{
          width: 10, height: 10, borderRadius: '50%', background: cfg.color, flexShrink: 0,
        }} />

        {/* Job info */}
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontWeight: 600, fontSize: '0.9rem', overflow: 'hidden', whiteSpace: 'nowrap', textOverflow: 'ellipsis' }}>
            {job.jobTitle || 'Unknown Position'}
          </div>
          <div style={{ color: 'var(--muted)', fontSize: '0.8rem' }}>
            {job.companyName || 'Unknown Company'} · {job.location || 'Unknown'}
          </div>
        </div>

        {/* Status badge */}
        <span style={{
          background: cfg.bg, color: cfg.color, border: `1px solid ${cfg.color}30`,
          borderRadius: 20, padding: '3px 12px', fontSize: '0.78rem', fontWeight: 600,
          whiteSpace: 'nowrap',
        }}>
          {cfg.icon} {cfg.label}
        </span>

        <span style={{ color: 'var(--muted)', fontSize: '0.78rem', whiteSpace: 'nowrap' }}>
          {timeAgo(app.appliedAt)}
        </span>

        <span style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>
          {expanded ? '▲' : '▼'}
        </span>
      </div>

      {/* Expanded row */}
      {expanded && (
        <div style={{
          padding: '0 28px 20px 54px', background: 'var(--bg1)',
          borderTop: '1px solid var(--border)',
        }}>
          {/* Status pipeline */}
          <div style={{ marginBottom: 16, paddingTop: 16 }}>
            <div style={{ color: 'var(--muted)', fontSize: '0.75rem', marginBottom: 10, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Update Status
            </div>
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              {STATUS_ORDER.map(s => {
                const c = STATUS_CONFIG[s]
                const active = app.status === s
                return (
                  <button key={s} onClick={() => !active && onStatusChange(app.id, s)}
                    style={{
                      background: active ? c.bg : 'transparent',
                      color: active ? c.color : 'var(--muted)',
                      border: `1px solid ${active ? c.color : 'var(--border)'}`,
                      borderRadius: 8, padding: '6px 14px', fontSize: '0.82rem',
                      fontWeight: active ? 600 : 400, cursor: active ? 'default' : 'pointer',
                      transition: 'all 0.15s',
                    }}>
                    {c.icon} {c.label}
                  </button>
                )
              })}
            </div>
          </div>

          {/* Cover letter preview */}
          {app.coverLetter && (
            <div style={{ marginBottom: 16 }}>
              <div style={{ color: 'var(--muted)', fontSize: '0.75rem', marginBottom: 8, textTransform: 'uppercase', letterSpacing: '0.05em' }}>Cover Letter</div>
              <div style={{
                background: 'var(--bg2)', border: '1px solid var(--border)',
                borderRadius: 8, padding: '12px 14px', fontSize: '0.82rem',
                color: 'var(--muted)', lineHeight: 1.55,
                maxHeight: 150, overflowY: 'auto',
              }}>
                {app.coverLetter}
              </div>
            </div>
          )}

          {/* Actions */}
          <div style={{ display: 'flex', gap: 10 }}>
            {job.sourceUrl && (
              <a href={job.sourceUrl} target="_blank" rel="noreferrer" style={{
                background: 'var(--accent)', color: '#fff', border: 'none',
                borderRadius: 8, padding: '6px 16px', fontSize: '0.82rem',
                fontWeight: 600, textDecoration: 'none', display: 'inline-block',
              }}>Open Listing ↗</a>
            )}
            <button onClick={() => onDelete(app.id)} style={{
              background: 'none', color: '#f87171', border: '1px solid #f8717140',
              borderRadius: 8, padding: '6px 14px', fontSize: '0.82rem', cursor: 'pointer',
            }}>Remove</button>
          </div>
        </div>
      )}
    </div>
  )
}
