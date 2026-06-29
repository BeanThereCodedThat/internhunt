// MatchScoreBadge — color-coded SVG ring showing a 0–100 match score.
// score < 0 means "no data yet" (shown as a dash, muted color).
// Tiers: green >=80, yellow >=60, orange >=40, red below.

function tierColor(score) {
  if (score < 0)   return 'var(--muted)'
  if (score >= 80) return 'var(--green)'
  if (score >= 60) return 'var(--yellow)'
  if (score >= 40) return 'var(--orange)'
  return 'var(--red)'
}

export default function MatchScoreBadge({ score, large = false }) {
  const size   = large ? 64 : 36
  const stroke = large ? 6  : 4
  const radius = (size - stroke) / 2
  const circumference = 2 * Math.PI * radius
  const pct    = score < 0 ? 0 : Math.min(100, Math.max(0, score))
  const offset = circumference - (pct / 100) * circumference
  const color  = tierColor(score)

  return (
    <div
      title={score < 0 ? 'No match data yet' : `${score}% match`}
      style={{ position: 'relative', width: size, height: size, flexShrink: 0 }}
    >
      <svg width={size} height={size} style={{ transform: 'rotate(-90deg)' }}>
        <circle
          cx={size / 2} cy={size / 2} r={radius}
          stroke="var(--border)" strokeWidth={stroke} fill="none"
        />
        <circle
          cx={size / 2} cy={size / 2} r={radius}
          stroke={color} strokeWidth={stroke} fill="none"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          strokeLinecap="round"
          style={{ transition: 'stroke-dashoffset 0.4s ease' }}
        />
      </svg>
      <div style={{
        position: 'absolute', inset: 0,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontFamily: 'var(--font-head)', fontWeight: 700,
        fontSize: large ? '1rem' : '0.68rem',
        color,
      }}>
        {score < 0 ? '–' : score}
      </div>
    </div>
  )
}
