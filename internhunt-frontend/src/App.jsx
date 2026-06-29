import { useState, useEffect, useCallback, useRef } from 'react'
import { fetchJobs, fetchMatchScores } from './api'
import JobCard from './components/JobCard'
import JobModal from './components/JobModal'
import StatsBar from './components/StatsBar'
import ScraperPanel from './components/ScraperPanel'

const PAGE_SIZE = 18

export default function App() {
  const [jobs,       setJobs]       = useState([])
  const [total,      setTotal]      = useState(0)
  const [page,       setPage]       = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading,    setLoading]    = useState(true)
  const [selected,   setSelected]   = useState(null)
  const [statsKey,   setStatsKey]   = useState(0)
  const [scores,     setScores]     = useState({})

  const [search, setSearch] = useState('')
  const [source, setSource] = useState('')
  const [type,   setType]   = useState('')
  const [remote, setRemote] = useState('')

  const searchTimeout = useRef(null)

  const load = useCallback(async (pg = 0) => {
    setLoading(true)
    try {
      const data = await fetchJobs({
        page:   pg,
        size:   PAGE_SIZE,
        search: search.trim(),
        source,
        type,
        remote: remote === '' ? null : remote === 'true',
      })
      setJobs(data.content || [])
      setTotal(data.totalElements || 0)
      setTotalPages(data.totalPages || 0)
      setPage(pg)

      // Fire-and-forget — badges just show "no data" until this resolves.
      const ids = (data.content || []).map(j => j.id)
      fetchMatchScores(ids).then(setScores)
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }, [search, source, type, remote])

  // Debounce search
  useEffect(() => {
    clearTimeout(searchTimeout.current)
    searchTimeout.current = setTimeout(() => { load(0) }, 300)
    return () => clearTimeout(searchTimeout.current)
  }, [load])

  const handleScraped = () => {
    setStatsKey(k => k + 1)
    setTimeout(() => load(0), 5000)
  }

  const totalInternships = jobs.filter(j => j.listingType === 'internship').length

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>

      {/* ── Header ── */}
      <header style={{
        background: 'var(--bg2)',
        borderBottom: '1px solid var(--border)',
        padding: '0 32px',
        height: 60,
        display: 'flex',
        alignItems: 'center',
        gap: 24,
        position: 'sticky',
        top: 0,
        zIndex: 50,
      }}>
        <div style={{ fontFamily: 'var(--font-head)', fontWeight: 800, fontSize: '1.35rem', letterSpacing: '-0.02em' }}>
          Intern<span style={{ color: 'var(--accent)' }}>Hunt</span>
        </div>
        <div style={{ flex: 1, maxWidth: 520 }}>
          <SearchInput value={search} onChange={setSearch} />
        </div>
        <div style={{ marginLeft: 'auto', color: 'var(--muted)', fontSize: '0.8rem' }}>
          {total.toLocaleString()} listings
        </div>
      </header>

      {/* ── Stats Bar ── */}
      <StatsBar key={statsKey} />

      {/* ── Body ── */}
      <div style={{ display: 'flex', flex: 1, maxWidth: 1400, margin: '0 auto', width: '100%', padding: '24px 24px' }}>

        {/* ── Sidebar ── */}
        <aside style={{ width: 240, flexShrink: 0, marginRight: 28, display: 'flex', flexDirection: 'column', gap: 16 }}>

          {/* Filters */}
          <div style={{
            background: 'var(--bg2)',
            border: '1px solid var(--border)',
            borderRadius: 'var(--radius)',
            padding: '18px 20px',
          }}>
            <div style={{ fontFamily: 'var(--font-head)', fontWeight: 700, fontSize: '0.9rem', marginBottom: 16, color: 'var(--muted)', letterSpacing: '0.05em', textTransform: 'uppercase' }}>
              Filters
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              <FilterGroup label="Source">
                {['', 'unstop', 'internshala', 'hackernews'].map(v => (
                  <FilterChip key={v} active={source === v} onClick={() => setSource(v)}>
                    {v || 'All'}
                  </FilterChip>
                ))}
              </FilterGroup>

              <FilterGroup label="Type">
                {[['', 'All'], ['internship', 'Internship'], ['full_time', 'Full-time'], ['contract', 'Contract']].map(([v,l]) => (
                  <FilterChip key={v} active={type === v} onClick={() => setType(v)}>
                    {l}
                  </FilterChip>
                ))}
              </FilterGroup>

              <FilterGroup label="Location">
                {[['', 'All'], ['true', 'Remote only'], ['false', 'Onsite only']].map(([v, l]) => (
                  <FilterChip key={v} active={remote === v} onClick={() => setRemote(v)}>
                    {l}
                  </FilterChip>
                ))}
              </FilterGroup>
            </div>

            {(source || type || remote) && (
              <button
                onClick={() => { setSource(''); setType(''); setRemote('') }}
                style={{
                  marginTop: 14,
                  background: 'none',
                  color: 'var(--accent)',
                  fontSize: '0.78rem',
                  fontWeight: 600,
                  padding: 0,
                  border: 'none',
                }}
              >
                Clear all filters
              </button>
            )}
          </div>

          {/* Scraper panel */}
          <ScraperPanel onScraped={handleScraped} />
        </aside>

        {/* ── Job Grid ── */}
        <main style={{ flex: 1, minWidth: 0 }}>

          {/* Active filter tags */}
          {(source || type || remote || search) && (
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 16 }}>
              {search && <ActiveFilter label={`"${search}"`} onRemove={() => setSearch('')} />}
              {source && <ActiveFilter label={source} onRemove={() => setSource('')} />}
              {type   && <ActiveFilter label={type}   onRemove={() => setType('')} />}
              {remote && <ActiveFilter label={remote === 'true' ? 'Remote' : 'Onsite'} onRemove={() => setRemote('')} />}
              <span style={{ color: 'var(--muted)', fontSize: '0.8rem', alignSelf: 'center' }}>
                {total.toLocaleString()} result{total !== 1 ? 's' : ''}
              </span>
            </div>
          )}

          {loading ? (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: 14 }}>
              {Array.from({ length: 12 }).map((_, i) => <SkeletonCard key={i} />)}
            </div>
          ) : jobs.length === 0 ? (
            <Empty />
          ) : (
            <>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: 14 }}>
                {jobs.map(job => (
                  <JobCard key={job.id} job={job} onClick={setSelected} matchScore={scores[job.id]} />
                ))}
              </div>

              {/* Pagination */}
              {totalPages > 1 && (
                <div style={{ display: 'flex', justifyContent: 'center', gap: 8, marginTop: 32, flexWrap: 'wrap' }}>
                  <PageBtn disabled={page === 0} onClick={() => load(page - 1)}>← Prev</PageBtn>
                  {Array.from({ length: Math.min(totalPages, 7) }).map((_, i) => {
                    const p = totalPages <= 7 ? i
                      : page < 4 ? i
                      : page > totalPages - 5 ? totalPages - 7 + i
                      : page - 3 + i
                    return (
                      <PageBtn key={p} active={p === page} onClick={() => load(p)}>
                        {p + 1}
                      </PageBtn>
                    )
                  })}
                  <PageBtn disabled={page >= totalPages - 1} onClick={() => load(page + 1)}>Next →</PageBtn>
                </div>
              )}
            </>
          )}
        </main>
      </div>

      {/* ── Modal ── */}
      {selected && <JobModal job={selected} onClose={() => setSelected(null)} />}
    </div>
  )
}

// ─── Sub-components ────────────────────────────────────────────────────────────

function SearchInput({ value, onChange }) {
  return (
    <div style={{ position: 'relative' }}>
      <span style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', color: 'var(--muted)', fontSize: '1rem', pointerEvents: 'none' }}>🔍</span>
      <input
        value={value}
        onChange={e => onChange(e.target.value)}
        placeholder="Search jobs, companies, skills…"
        style={{
          width: '100%',
          background: 'var(--bg3)',
          border: '1px solid var(--border)',
          borderRadius: 9,
          padding: '8px 14px 8px 38px',
          color: 'var(--text)',
          fontSize: '0.9rem',
          outline: 'none',
          transition: 'border-color 0.15s',
        }}
        onFocus={e => e.target.style.borderColor = 'var(--accent)'}
        onBlur={e => e.target.style.borderColor = 'var(--border)'}
      />
    </div>
  )
}

function FilterGroup({ label, children }) {
  return (
    <div>
      <div style={{ color: 'var(--muted)', fontSize: '0.72rem', textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: 7 }}>{label}</div>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 5 }}>{children}</div>
    </div>
  )
}

function FilterChip({ active, onClick, children }) {
  return (
    <button
      onClick={onClick}
      style={{
        background: active ? 'var(--accent)' : 'var(--bg3)',
        color: active ? '#fff' : 'var(--muted)',
        border: `1px solid ${active ? 'var(--accent)' : 'var(--border)'}`,
        borderRadius: 20,
        padding: '3px 10px',
        fontSize: '0.75rem',
        fontWeight: active ? 600 : 400,
        transition: 'all 0.15s',
      }}
    >
      {children}
    </button>
  )
}

function ActiveFilter({ label, onRemove }) {
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: 6,
      background: '#1a1a2e', border: '1px solid var(--accent)',
      color: 'var(--accent)', borderRadius: 20, padding: '3px 10px',
      fontSize: '0.78rem', fontWeight: 600,
    }}>
      {label}
      <button onClick={onRemove} style={{ background: 'none', color: 'var(--accent)', fontSize: '1rem', lineHeight: 1, paddingBottom: 1 }}>×</button>
    </span>
  )
}

function PageBtn({ active, disabled, onClick, children }) {
  return (
    <button
      disabled={disabled}
      onClick={onClick}
      style={{
        background: active ? 'var(--accent)' : 'var(--bg2)',
        color: active ? '#fff' : disabled ? 'var(--muted)' : 'var(--text)',
        border: `1px solid ${active ? 'var(--accent)' : 'var(--border)'}`,
        borderRadius: 8,
        padding: '7px 14px',
        fontSize: '0.85rem',
        fontWeight: active ? 600 : 400,
        cursor: disabled ? 'not-allowed' : 'pointer',
        opacity: disabled ? 0.4 : 1,
        transition: 'all 0.15s',
      }}
    >
      {children}
    </button>
  )
}

function SkeletonCard() {
  return (
    <div style={{
      background: 'var(--card-bg)',
      border: '1px solid var(--border)',
      borderRadius: 'var(--radius)',
      padding: '20px 22px',
      display: 'flex', flexDirection: 'column', gap: 12,
    }}>
      {[['70%', 18], ['40%', 13], ['90%', 11], ['60%', 11]].map(([w, h], i) => (
        <div key={i} style={{
          width: w, height: h,
          background: 'linear-gradient(90deg, var(--bg3) 25%, var(--border) 50%, var(--bg3) 75%)',
          backgroundSize: '200% 100%',
          borderRadius: 4,
          animation: `shimmer 1.5s infinite ${i * 0.1}s`,
        }} />
      ))}
      <style>{`@keyframes shimmer { 0%{background-position:200% 0} 100%{background-position:-200% 0} }`}</style>
    </div>
  )
}

function Empty() {
  return (
    <div style={{ textAlign: 'center', padding: '80px 20px', color: 'var(--muted)' }}>
      <div style={{ fontSize: '3rem', marginBottom: 16 }}>🔍</div>
      <div style={{ fontFamily: 'var(--font-head)', fontSize: '1.2rem', color: 'var(--text)', marginBottom: 8 }}>No listings found</div>
      <div style={{ fontSize: '0.9rem' }}>Try different filters or run a scraper to fetch new listings.</div>
    </div>
  )
}
