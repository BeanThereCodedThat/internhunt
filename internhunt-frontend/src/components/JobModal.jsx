import { useEffect, useState } from 'react'
import { api, createApplication } from '../api'

export default function JobModal({ job, onClose, userId }) {
  const [scamWarning, setScamWarning] = useState(null)
  const [tracked,     setTracked]     = useState(false)
  const [tracking,    setTracking]    = useState(false)

  useEffect(() => {
    const handler = (e) => { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [onClose])

  // Check scam reports for this company
  useEffect(() => {
    if (!job?.companyName || job.companyName === 'Unknown') return
    api.get(`/scam-reports/check?company=${encodeURIComponent(job.companyName)}`)
      .then(data => { if (data?.flagged) setScamWarning(data) })
      .catch(() => {})
  }, [job?.companyName])

  // Check if already tracked
  useEffect(() => {
    if (!userId || !job?.id) return
    api.get(`/applications/user/${userId}`)
      .then(apps => {
        const already = apps.some(a => a.job?.id === job.id)
        setTracked(already)
      })
      .catch(() => {})
  }, [userId, job?.id])

  async function trackApplication() {
    if (!userId || tracked || tracking) return
    setTracking(true)
    try {
      await createApplication({ user: { id: userId }, job: { id: job.id }, status: 'pending' })
      setTracked(true)
    } catch (e) {
      console.error(e)
    }
    setTracking(false)
  }

  if (!job) return null

  return (
    <div
      onClick={onClose}
      style={{
        position: 'fixed', inset: 0,
        background: 'rgba(0,0,0,0.7)',
        backdropFilter: 'blur(4px)',
        zIndex: 100,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        padding: '20px',
      }}
    >
      <div
        onClick={e => e.stopPropagation()}
        style={{
          background: 'var(--bg2)',
          border: '1px solid var(--border)',
          borderRadius: 16,
          padding: '32px',
          width: '100%', maxWidth: 640,
          maxHeight: '85vh', overflowY: 'auto',
          display: 'flex', flexDirection: 'column', gap: 18,
        }}
      >
        {/* Scam warning banner */}
        {scamWarning && (
          <div style={{
            background: '#2a0f0f',
            border: '1px solid #f87171',
            borderRadius: 10,
            padding: '12px 16px',
            display: 'flex', gap: 10, alignItems: 'flex-start',
          }}>
            <span style={{ fontSize: '1.2rem' }}>⚠️</span>
            <div>
              <div style={{ color: '#f87171', fontWeight: 700, fontSize: '0.88rem', marginBottom: 4 }}>
                Scam Warning — {scamWarning.severity === 'confirmed_scam' ? 'Confirmed Scam' : 'Warning Flag'}
              </div>
              <div style={{ color: '#fca5a5', fontSize: '0.82rem', lineHeight: 1.5 }}>
                {job.companyName} has been flagged on Reddit. Proceed with caution and verify before applying.
              </div>
              {scamWarning.sourceUrl && (
                <a href={scamWarning.sourceUrl} target="_blank" rel="noreferrer"
                  style={{ color: '#f87171', fontSize: '0.78rem', marginTop: 4, display: 'inline-block' }}>
                  View Reddit report →
                </a>
              )}
            </div>
          </div>
        )}

        {/* Top bar */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <div>
            <h2 style={{ fontFamily: 'var(--font-head)', fontSize: '1.3rem', fontWeight: 700, lineHeight: 1.3 }}>
              {job.jobTitle}
            </h2>
            <div style={{ color: 'var(--accent)', fontWeight: 600, marginTop: 4 }}>
              {job.companyName}
            </div>
          </div>
          <button
            onClick={onClose}
            style={{
              background: 'var(--bg3)', color: 'var(--muted)',
              border: '1px solid var(--border)', borderRadius: 8,
              width: 34, height: 34, fontSize: '1.1rem',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              flexShrink: 0, cursor: 'pointer',
            }}
          >×</button>
        </div>

        {/* Meta */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10 }}>
          <MetaItem label="Source"   value={job.sourceName} />
          <MetaItem label="Type"     value={job.listingType} />
          <MetaItem label="Location" value={job.location} />
          <MetaItem label="Remote"   value={job.isRemote ? 'Yes' : 'No'} />
          {job.stipend && <MetaItem label="Stipend" value={job.stipend} color="var(--green)" />}
          {job.deadline && <MetaItem label="Deadline" value={new Date(job.deadline).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })} color="var(--yellow)" />}
        </div>

        <hr style={{ border: 'none', borderTop: '1px solid var(--border)' }} />

        {/* Description */}
        {job.description && (
          <div style={{ color: 'var(--muted)', fontSize: '0.88rem', lineHeight: 1.75, whiteSpace: 'pre-wrap' }}>
            {job.description}
          </div>
        )}

        {/* CTAs */}
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginTop: 4 }}>
          {job.sourceUrl && (
            <a
              href={job.sourceUrl}
              target="_blank"
              rel="noopener noreferrer"
              style={{
                display: 'inline-block',
                background: 'var(--accent)', color: '#fff',
                fontFamily: 'var(--font-head)', fontWeight: 600,
                padding: '12px 24px', borderRadius: 10,
                textAlign: 'center', transition: 'opacity 0.15s', textDecoration: 'none',
              }}
              onMouseEnter={e => e.currentTarget.style.opacity = '0.85'}
              onMouseLeave={e => e.currentTarget.style.opacity = '1'}
            >
              Apply / View Original →
            </a>
          )}

          {/* Track button */}
          {userId && (
            <button
              onClick={trackApplication}
              disabled={tracked || tracking}
              style={{
                background: tracked ? '#1a2a1a' : 'transparent',
                color: tracked ? 'var(--green)' : 'var(--accent)',
                border: `1px solid ${tracked ? 'var(--green)' : 'var(--accent)'}`,
                borderRadius: 10, padding: '12px 20px',
                fontWeight: 600, fontSize: '0.9rem',
                cursor: tracked || tracking ? 'default' : 'pointer',
                transition: 'all 0.15s',
              }}
            >
              {tracked ? '✓ Tracked' : tracking ? '...' : '+ Track Application'}
            </button>
          )}
        </div>
      </div>
    </div>
  )
}

function MetaItem({ label, value, color = 'var(--text)' }) {
  if (!value || value === 'Unknown') return null
  return (
    <div style={{
      background: 'var(--bg3)', border: '1px solid var(--border)',
      borderRadius: 8, padding: '6px 12px',
      display: 'flex', flexDirection: 'column', gap: 1,
    }}>
      <span style={{ color: 'var(--muted)', fontSize: '0.68rem', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
        {label}
      </span>
      <span style={{ color, fontWeight: 500, fontSize: '0.85rem' }}>
        {String(value).charAt(0).toUpperCase() + String(value).slice(1)}
      </span>
    </div>
  )
}
