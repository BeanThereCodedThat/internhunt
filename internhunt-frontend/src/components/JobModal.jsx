import { useState, useEffect } from 'react'
import { ACTIVE_USER_ID } from '../api'
import MatchPanel from './MatchPanel'

export default function JobModal({ job, onClose }) {
  const [tab, setTab] = useState('details')

  useEffect(() => {
    const handler = (e) => { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [onClose])

  // Reset to Details whenever a different job is opened.
  useEffect(() => { setTab('details') }, [job?.id])

  if (!job) return null

  return (
    <div
      onClick={onClose}
      style={{
        position: 'fixed', inset: 0,
        background: 'rgba(0,0,0,0.7)',
        backdropFilter: 'blur(4px)',
        zIndex: 100,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
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
          width: '100%',
          maxWidth: 640,
          maxHeight: '85vh',
          overflowY: 'auto',
          display: 'flex',
          flexDirection: 'column',
          gap: 18,
        }}
      >
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
              flexShrink: 0,
            }}
          >×</button>
        </div>

        {/* Tabs */}
        <div style={{ display: 'flex', gap: 18, borderBottom: '1px solid var(--border)' }}>
          <TabButton active={tab === 'details'} onClick={() => setTab('details')}>Details</TabButton>
          <TabButton active={tab === 'match'}   onClick={() => setTab('match')}>AI Match</TabButton>
        </div>

        {tab === 'details' ? (
          <>
            {/* Meta */}
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10 }}>
              <MetaItem label="Source"   value={job.sourceName} />
              <MetaItem label="Type"     value={job.listingType} />
              <MetaItem label="Location" value={job.location} />
              <MetaItem label="Remote"   value={job.isRemote ? 'Yes' : 'No'} />
              {job.stipend && <MetaItem label="Stipend" value={job.stipend} color="var(--green)" />}
              {job.deadline && <MetaItem label="Deadline" value={new Date(job.deadline).toLocaleDateString()} />}
            </div>

            <hr style={{ border: 'none', borderTop: '1px solid var(--border)' }} />

            {/* Description */}
            {job.description && (
              <div style={{ color: 'var(--muted)', fontSize: '0.88rem', lineHeight: 1.75, whiteSpace: 'pre-wrap' }}>
                {job.description}
              </div>
            )}

            {/* CTA */}
            {job.sourceUrl && (
              <a
                href={job.sourceUrl}
                target="_blank"
                rel="noopener noreferrer"
                style={{
                  display: 'inline-block',
                  background: 'var(--accent)',
                  color: '#fff',
                  fontFamily: 'var(--font-head)',
                  fontWeight: 600,
                  padding: '12px 24px',
                  borderRadius: 10,
                  textAlign: 'center',
                  marginTop: 4,
                  transition: 'opacity 0.15s',
                }}
                onMouseEnter={e => e.target.style.opacity = 0.85}
                onMouseLeave={e => e.target.style.opacity = 1}
              >
                Apply / View Original →
              </a>
            )}
          </>
        ) : (
          <MatchPanel job={job} userId={ACTIVE_USER_ID} />
        )}
      </div>
    </div>
  )
}

function TabButton({ active, onClick, children }) {
  return (
    <button
      onClick={onClick}
      style={{
        background: 'none',
        color: active ? 'var(--accent)' : 'var(--muted)',
        borderBottom: active ? '2px solid var(--accent)' : '2px solid transparent',
        padding: '8px 2px',
        marginBottom: -1,
        fontSize: '0.88rem',
        fontWeight: active ? 700 : 500,
        fontFamily: 'var(--font-head)',
      }}
    >
      {children}
    </button>
  )
}

function MetaItem({ label, value, color = 'var(--text)' }) {
  if (!value || value === 'Unknown') return null
  return (
    <div style={{
      background: 'var(--bg3)',
      border: '1px solid var(--border)',
      borderRadius: 8,
      padding: '6px 12px',
      display: 'flex',
      flexDirection: 'column',
      gap: 1,
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
