import { useState } from 'react'
import MatchScoreBadge from './MatchScoreBadge'

const SOURCE_COLORS = {
  unstop:           { bg: '#1a1a2e', accent: '#6c63ff', label: 'Unstop' },
  internshala:      { bg: '#1a2a1a', accent: '#22d3a0', label: 'Internshala' },
  hackernews:       { bg: '#2a1a0e', accent: '#fb923c', label: 'HackerNews' },
  reddit:           { bg: '#2a1414', accent: '#ff5722', label: 'Reddit' },
  company_careers:  { bg: '#14202a', accent: '#38bdf8', label: 'Company Careers' },
  rss_blogs:        { bg: '#241a2a', accent: '#c084fc', label: 'Eng Blog' },
  linkedin:         { bg: '#0f1e2e', accent: '#0a66c2', label: 'LinkedIn' },
  naukri:           { bg: '#2a1f0a', accent: '#f6a821', label: 'Naukri' },
  indeed:           { bg: '#0f1a2e', accent: '#2557a7', label: 'Indeed' },
  wellfound:        { bg: '#14241a', accent: '#4ade80', label: 'Wellfound' },
}

function timeAgo(dateStr) {
  if (!dateStr) return ''
  const diff = Date.now() - new Date(dateStr).getTime()
  const h = Math.floor(diff / 3600000)
  const d = Math.floor(h / 24)
  if (d > 30) return `${Math.floor(d/30)}mo ago`
  if (d > 0)  return `${d}d ago`
  if (h > 0)  return `${h}h ago`
  return 'just now'
}

export default function JobCard({ job, onClick, matchScore }) {
  const [hovered, setHovered] = useState(false)
  const src = SOURCE_COLORS[job.sourceName] || { bg: '#1a1a1a', accent: '#6b6b7e', label: job.sourceName }
  const isIntern = job.listingType === 'internship'

  return (
    <div
      onClick={() => onClick(job)}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        background: hovered ? '#1a1a24' : 'var(--card-bg)',
        border: `1px solid ${hovered ? src.accent + '60' : 'var(--border)'}`,
        borderRadius: 'var(--radius)',
        padding: '20px 22px',
        cursor: 'pointer',
        transition: 'all 0.18s ease',
        transform: hovered ? 'translateY(-2px)' : 'none',
        boxShadow: hovered ? `0 8px 32px ${src.accent}20` : 'none',
        display: 'flex',
        flexDirection: 'column',
        gap: '12px',
      }}
    >
      {/* Header row */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 8 }}>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{
            fontFamily: 'var(--font-head)',
            fontWeight: 600,
            fontSize: '1rem',
            color: 'var(--text)',
            lineHeight: 1.35,
            overflow: 'hidden',
            display: '-webkit-box',
            WebkitLineClamp: 2,
            WebkitBoxOrient: 'vertical',
          }}>
            {job.jobTitle}
          </div>
          <div style={{ color: src.accent, fontSize: '0.88rem', marginTop: 3, fontWeight: 500 }}>
            {job.companyName}
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
          {typeof matchScore === 'number' && <MatchScoreBadge score={matchScore} />}
          <span style={{
            background: src.bg,
            color: src.accent,
            border: `1px solid ${src.accent}40`,
            borderRadius: 20,
            padding: '3px 10px',
            fontSize: '0.72rem',
            fontWeight: 600,
            letterSpacing: '0.04em',
            whiteSpace: 'nowrap',
          }}>
            {src.label}
          </span>
        </div>
      </div>

      {/* Meta row */}
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
        {job.location && job.location !== 'Unknown' && (
          <Tag icon="📍">{job.location.length > 30 ? job.location.slice(0,30)+'…' : job.location}</Tag>
        )}
        {job.isRemote && <Tag icon="🌐" color="var(--green)">Remote</Tag>}
        <Tag icon={isIntern ? '🎓' : '💼'} color={isIntern ? 'var(--yellow)' : 'var(--accent)'}>
          {isIntern ? 'Internship' : 'Full-time'}
        </Tag>
        {job.stipend && <Tag icon="💰" color="var(--green)">{job.stipend}</Tag>}
      </div>

      {/* Description preview */}
      {job.description && (
        <p style={{
          color: 'var(--muted)',
          fontSize: '0.83rem',
          overflow: 'hidden',
          display: '-webkit-box',
          WebkitLineClamp: 2,
          WebkitBoxOrient: 'vertical',
          lineHeight: 1.5,
        }}>
          {job.description}
        </p>
      )}

      {/* Footer */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 2 }}>
        <span style={{ color: 'var(--muted)', fontSize: '0.78rem' }}>
          {timeAgo(job.scrapedAt)}
        </span>
        <span style={{
          color: src.accent,
          fontSize: '0.78rem',
          fontWeight: 600,
          opacity: hovered ? 1 : 0,
          transition: 'opacity 0.15s',
        }}>
          View details →
        </span>
      </div>
    </div>
  )
}

function Tag({ icon, children, color = 'var(--muted)' }) {
  return (
    <span style={{
      display: 'inline-flex',
      alignItems: 'center',
      gap: 4,
      background: 'var(--bg3)',
      border: '1px solid var(--border)',
      borderRadius: 20,
      padding: '2px 9px',
      fontSize: '0.75rem',
      color,
      whiteSpace: 'nowrap',
    }}>
      {icon} {children}
    </span>
  )
}
