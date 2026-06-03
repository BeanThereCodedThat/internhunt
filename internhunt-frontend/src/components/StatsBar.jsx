import { useEffect, useState } from 'react'
import { fetchStats } from '../api'

export default function StatsBar() {
  const [stats, setStats] = useState(null)

  useEffect(() => {
    fetchStats().then(setStats).catch(() => {})
  }, [])

  if (!stats) return null

  const total = (stats.totalJobs || 0) + (stats.totalInternships || 0)

  return (
    <div style={{
      display: 'flex',
      gap: 12,
      flexWrap: 'wrap',
      padding: '14px 20px',
      background: 'var(--bg2)',
      borderBottom: '1px solid var(--border)',
    }}>
      <Stat label="Total listings" value={total.toLocaleString()} color="var(--accent)" />
      <Stat label="Internships"    value={stats.totalInternships?.toLocaleString()} color="var(--yellow)" />
      <Stat label="Jobs"           value={stats.totalJobs?.toLocaleString()} color="var(--green)" />
      <Stat label="Remote"         value={stats.totalRemote?.toLocaleString()} color="#60a5fa" />
      <Stat label="Added today"    value={stats.savedToday?.toLocaleString()} color="var(--accent2)" />

      <div style={{ flex: 1 }} />

      {stats.bySource?.map(s => (
        <Stat key={s.name} label={cap(s.name)} value={s.count?.toLocaleString()} color="var(--muted)" />
      ))}
    </div>
  )
}

function Stat({ label, value, color }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
      <span style={{ color, fontFamily: 'var(--font-head)', fontWeight: 700, fontSize: '1.05rem' }}>
        {value ?? '—'}
      </span>
      <span style={{ color: 'var(--muted)', fontSize: '0.78rem' }}>{label}</span>
      <span style={{ color: 'var(--border)', marginLeft: 4 }}>·</span>
    </div>
  )
}

function cap(s) { return s ? s.charAt(0).toUpperCase() + s.slice(1) : '' }
