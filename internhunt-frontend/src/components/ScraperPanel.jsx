import { useState } from 'react'
import { triggerScraper, triggerAllScrapers } from '../api'

const SCRAPERS = [
  { id: 'company_careers', label: '🏢 Company Careers', desc: '20 Indian tech companies', color: '#a78bfa' },
  { id: 'unstop',          label: '🎯 Unstop',          desc: 'Competitions & internships', color: '#6c63ff' },
  { id: 'internshala',     label: '📚 Internshala',     desc: 'India internship listings', color: '#22d3a0' },
  { id: 'reddit',          label: '🔴 Reddit',          desc: 'r/IndiaCSCareerQuestions', color: '#ff6314' },
  { id: 'hackernews',      label: '🔶 HackerNews',      desc: 'Who\'s Hiring thread', color: '#fb923c' },
]

export default function ScraperPanel({ onScraped }) {
  const [running, setRunning] = useState({})
  const [runningAll, setRunningAll] = useState(false)

  async function runOne(id) {
    setRunning(r => ({ ...r, [id]: true }))
    try {
      await triggerScraper(id)
      onScraped?.()
    } catch (e) {
      console.error(e)
    } finally {
      // Keep showing "running" for 3s (it's async in the backend)
      setTimeout(() => {
        setRunning(r => ({ ...r, [id]: false }))
      }, 3000)
    }
  }

  async function runAll() {
    setRunningAll(true)
    try {
      await triggerAllScrapers()
      onScraped?.()
    } catch (e) {
      console.error(e)
    } finally {
      setTimeout(() => setRunningAll(false), 5000)
    }
  }

  return (
    <div style={{
      background: 'var(--bg2)',
      border: '1px solid var(--border)',
      borderRadius: 'var(--radius)',
      padding: '18px 20px',
    }}>
      <div style={{
        fontFamily: 'var(--font-head)', fontWeight: 700, fontSize: '0.9rem',
        marginBottom: 14, color: 'var(--muted)',
        letterSpacing: '0.05em', textTransform: 'uppercase',
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      }}>
        <span>Scrapers</span>
        <span style={{ fontSize: '0.65rem', color: 'var(--border)' }}>auto every 12h</span>
      </div>

      {/* Run All button */}
      <button
        onClick={runAll}
        disabled={runningAll}
        style={{
          width: '100%',
          background: runningAll ? 'var(--bg3)' : 'var(--accent)',
          color: runningAll ? 'var(--muted)' : '#fff',
          border: `1px solid ${runningAll ? 'var(--border)' : 'var(--accent)'}`,
          borderRadius: 8,
          padding: '8px 0',
          fontSize: '0.82rem',
          fontWeight: 700,
          cursor: runningAll ? 'not-allowed' : 'pointer',
          marginBottom: 12,
          transition: 'all 0.15s',
          letterSpacing: '0.03em',
        }}
      >
        {runningAll ? '⏳ Running All...' : '▶ Run All Scrapers'}
      </button>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {SCRAPERS.map(s => (
          <ScraperButton
            key={s.id}
            scraper={s}
            running={!!running[s.id]}
            onClick={() => runOne(s.id)}
          />
        ))}
      </div>

      <div style={{
        marginTop: 12, color: 'var(--muted)', fontSize: '0.72rem', lineHeight: 1.5,
        borderTop: '1px solid var(--border)', paddingTop: 10,
      }}>
        Scrapers run in the background. Stats update after ~30s.
      </div>
    </div>
  )
}

function ScraperButton({ scraper, running, onClick }) {
  const [hovered, setHovered] = useState(false)

  return (
    <button
      onClick={onClick}
      disabled={running}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        background: running ? 'var(--bg3)' : hovered ? `${scraper.color}18` : 'transparent',
        border: `1px solid ${running || hovered ? scraper.color + '60' : 'var(--border)'}`,
        borderRadius: 8,
        padding: '8px 12px',
        cursor: running ? 'not-allowed' : 'pointer',
        transition: 'all 0.15s',
        display: 'flex',
        alignItems: 'center',
        gap: 10,
        textAlign: 'left',
      }}
    >
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          color: running ? 'var(--muted)' : scraper.color,
          fontWeight: 600,
          fontSize: '0.78rem',
        }}>
          {running ? '⏳ Running...' : scraper.label}
        </div>
        <div style={{ color: 'var(--muted)', fontSize: '0.68rem', marginTop: 1 }}>
          {scraper.desc}
        </div>
      </div>
      {!running && (
        <span style={{ color: scraper.color, fontSize: '0.75rem', opacity: hovered ? 1 : 0.4, transition: 'opacity 0.15s' }}>▶</span>
      )}
    </button>
  )
}
