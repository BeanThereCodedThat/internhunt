import { useState } from 'react'
import { triggerScraper } from '../api'

const SCRAPERS = [
  { id: 'unstop',      label: 'Unstop',      icon: '⚡', color: '#6c63ff' },
  { id: 'internshala', label: 'Internshala', icon: '🎓', color: '#22d3a0' },
  { id: 'hackernews',  label: 'HackerNews',  icon: '🔶', color: '#fb923c' },
]

export default function ScraperPanel({ onScraped }) {
  const [statuses, setStatuses] = useState({})

  const run = async (id) => {
    setStatuses(s => ({ ...s, [id]: 'running' }))
    try {
      await triggerScraper(id)
      setStatuses(s => ({ ...s, [id]: 'started' }))
      setTimeout(() => {
        setStatuses(s => ({ ...s, [id]: null }))
        if (onScraped) onScraped()
      }, 3000)
    } catch {
      setStatuses(s => ({ ...s, [id]: 'error' }))
      setTimeout(() => setStatuses(s => ({ ...s, [id]: null })), 3000)
    }
  }

  return (
    <div style={{
      background: 'var(--bg2)',
      border: '1px solid var(--border)',
      borderRadius: 'var(--radius)',
      padding: '18px 20px',
    }}>
      <div style={{ fontFamily: 'var(--font-head)', fontWeight: 700, fontSize: '0.9rem', marginBottom: 14, color: 'var(--muted)', letterSpacing: '0.05em', textTransform: 'uppercase' }}>
        Run Scrapers
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {SCRAPERS.map(s => {
          const status = statuses[s.id]
          return (
            <button
              key={s.id}
              onClick={() => run(s.id)}
              disabled={status === 'running'}
              style={{
                background: status === 'started' ? '#16241e' : status === 'error' ? '#2a1616' : 'var(--bg3)',
                border: `1px solid ${status === 'started' ? '#22d3a060' : status === 'error' ? '#ef444460' : 'var(--border)'}`,
                color: status === 'started' ? 'var(--green)' : status === 'error' ? '#ef4444' : 'var(--text)',
                borderRadius: 8,
                padding: '9px 14px',
                display: 'flex',
                alignItems: 'center',
                gap: 10,
                fontSize: '0.85rem',
                fontWeight: 500,
                transition: 'all 0.15s',
                cursor: status === 'running' ? 'not-allowed' : 'pointer',
                opacity: status === 'running' ? 0.6 : 1,
              }}
            >
              <span>{s.icon}</span>
              <span style={{ flex: 1, textAlign: 'left' }}>{s.label}</span>
              <span style={{ fontSize: '0.75rem', color: s.color }}>
                {status === 'running' ? 'Starting…'
                  : status === 'started' ? '✓ Started'
                  : status === 'error'   ? '✗ Error'
                  : 'Run'}
              </span>
            </button>
          )
        })}
      </div>
      <p style={{ color: 'var(--muted)', fontSize: '0.72rem', marginTop: 12, lineHeight: 1.5 }}>
        Scrapers run in the background. Refresh the list in a minute to see new results.
      </p>
    </div>
  )
}
