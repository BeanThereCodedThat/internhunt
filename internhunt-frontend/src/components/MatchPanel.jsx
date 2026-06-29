import { useState, useEffect } from 'react'
import { api } from '../api'
import MatchScoreBadge from './MatchScoreBadge'

const PROFICIENCY = ['', 'Beginner', 'Intermediate', 'Advanced']

/**
 * MatchPanel — shows match score and skill breakdown.
 * No AI, no cover letter, no external calls.
 * Pure: your skills vs job required skills.
 */
export default function MatchPanel({ job, userId }) {
  const [match,        setMatch]        = useState(null)
  const [loading,      setLoading]      = useState(true)

  useEffect(() => {
    if (!userId || !job?.id) { setLoading(false); return }
    api.get(`/match/${userId}/${job.id}`)
      .then(data => { setMatch(data); setLoading(false) })
      .catch(() => setLoading(false))
  }, [userId, job?.id])

  if (!userId) {
    return <EmptyState icon="👤" title="Set up your profile first" desc="Add your skills to your profile to see your match score." />
  }

  if (loading) {
    return (
      <div style={{ padding: '32px 0', textAlign: 'center', color: 'var(--muted)' }}>
        <Spinner />
        <div style={{ marginTop: 12, fontSize: '0.85rem' }}>Calculating match…</div>
      </div>
    )
  }

  if (!match) {
    return <EmptyState icon="⚠️" title="Unavailable" desc="Could not load match data. Try again." />
  }

  const score = match.score ?? -1

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>

      {/* ── Score header ──────────────────────────────────────────────────── */}
      <div style={{
        display: 'flex', alignItems: 'center', gap: 20,
        background: 'var(--bg3)', borderRadius: 12,
        padding: '16px 20px', border: '1px solid var(--border)',
      }}>
        <MatchScoreBadge score={score} large />
        <div style={{ flex: 1 }}>
          <div style={{
            fontFamily: 'var(--font-head)', fontWeight: 700,
            fontSize: '1.05rem', marginBottom: 4,
          }}>
            {scoreLabel(score)}
          </div>
          <div style={{ color: 'var(--muted)', fontSize: '0.85rem', lineHeight: 1.5 }}>
            {match.summary}
          </div>
          {match.meetsRequirements && (
            <div style={{ marginTop: 6, color: 'var(--green)', fontSize: '0.78rem', fontWeight: 600 }}>
              ✓ Meets all mandatory requirements
            </div>
          )}
        </div>
      </div>

      {/* ── Skills breakdown ──────────────────────────────────────────────── */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>

        {match.matched?.length > 0 && (
          <SkillsBlock title="✅ You have" color="var(--green)">
            {match.matched.map(s => (
              <SkillRow
                key={s.skillName}
                name={s.skillName}
                right={PROFICIENCY[s.userProficiency] || ''}
                tag={s.mandatory ? null : 'optional'}
                color="var(--green)"
              />
            ))}
          </SkillsBlock>
        )}

        {match.missing?.length > 0 && (
          <SkillsBlock title="❌ Missing" color="#f87171">
            {match.missing.map(name => (
              <div key={name} style={{
                fontSize: '0.82rem', color: '#f87171',
                padding: '5px 0', borderBottom: '1px solid var(--border)',
              }}>
                {name}
              </div>
            ))}
          </SkillsBlock>
        )}

        {match.bonus?.length > 0 && (
          <SkillsBlock title="⭐ Bonus skills" color="var(--yellow)">
            {match.bonus.map(name => (
              <div key={name} style={{
                fontSize: '0.82rem', color: 'var(--yellow)',
                padding: '5px 0', borderBottom: '1px solid var(--border)',
              }}>
                {name}
              </div>
            ))}
          </SkillsBlock>
        )}
      </div>

      {/* ── No skills extracted yet ───────────────────────────────────────── */}
      {match.matched?.length === 0 && match.missing?.length === 0 && (
        <div style={{
          textAlign: 'center', padding: '24px',
          color: 'var(--muted)', fontSize: '0.85rem',
          background: 'var(--bg3)', borderRadius: 10,
          border: '1px solid var(--border)',
        }}>
          No skill data for this listing yet.
          <br />
          <button
            onClick={() => api.post(`/skills/extract/${job.id}`).then(() => window.location.reload())}
            style={{
              marginTop: 10, background: 'none', color: 'var(--accent)',
              border: '1px solid var(--accent)', borderRadius: 8,
              padding: '5px 14px', fontSize: '0.8rem', cursor: 'pointer',
            }}
          >
            Extract now
          </button>
        </div>
      )}
    </div>
  )
}

// ── Sub-components ─────────────────────────────────────────────────────────────

function SkillsBlock({ title, color, children }) {
  return (
    <div style={{
      background: 'var(--bg3)', borderRadius: 10,
      padding: '12px 14px', border: '1px solid var(--border)',
    }}>
      <div style={{
        color, fontWeight: 700, fontSize: '0.78rem', marginBottom: 10,
        textTransform: 'uppercase', letterSpacing: '0.05em',
      }}>
        {title}
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
        {children}
      </div>
    </div>
  )
}

function SkillRow({ name, right, tag, color }) {
  return (
    <div style={{
      display: 'flex', justifyContent: 'space-between', alignItems: 'center',
      fontSize: '0.82rem', padding: '5px 0', borderBottom: '1px solid var(--border)',
      gap: 6,
    }}>
      <span style={{ color }}>{name}</span>
      <div style={{ display: 'flex', gap: 6, alignItems: 'center', flexShrink: 0 }}>
        {tag && (
          <span style={{
            color: 'var(--muted)', fontSize: '0.68rem',
            border: '1px solid var(--border)', borderRadius: 10,
            padding: '1px 6px',
          }}>{tag}</span>
        )}
        <span style={{ color: 'var(--muted)', fontSize: '0.72rem' }}>{right}</span>
      </div>
    </div>
  )
}

function EmptyState({ icon, title, desc }) {
  return (
    <div style={{ textAlign: 'center', padding: '36px 20px' }}>
      <div style={{ fontSize: '2.5rem', marginBottom: 10 }}>{icon}</div>
      <div style={{ fontFamily: 'var(--font-head)', fontWeight: 600, marginBottom: 6 }}>{title}</div>
      <div style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>{desc}</div>
    </div>
  )
}

function Spinner() {
  return (
    <span style={{
      display: 'inline-block', width: 20, height: 20,
      border: '2px solid var(--border)', borderTopColor: 'var(--accent)',
      borderRadius: '50%', animation: 'spin 0.7s linear infinite',
    }}>
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </span>
  )
}

function scoreLabel(score) {
  if (score < 0)   return 'No match data yet'
  if (score >= 80) return 'Strong match 🎯'
  if (score >= 60) return 'Good match 👍'
  if (score >= 40) return 'Partial match'
  return 'Weak match — skill gaps present'
}
