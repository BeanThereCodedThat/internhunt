const BASE = '/api'

// Single-user mode for now — matches the hardcoded user id on the backend.
export const ACTIVE_USER_ID = 1

export async function fetchJobs({ page = 0, size = 20, search = '', source = '', type = '', remote = null } = {}) {
  const params = new URLSearchParams({ page, size })
  if (search)        params.set('search', search)
  if (source)        params.set('source', source)
  if (type)          params.set('type', type)
  if (remote !== null) params.set('remote', remote)
  const res = await fetch(`${BASE}/jobs?${params}`)
  if (!res.ok) throw new Error('Failed to fetch jobs')
  return res.json()
}

export async function fetchJob(id) {
  const res = await fetch(`${BASE}/jobs/${id}`)
  if (!res.ok) throw new Error('Job not found')
  return res.json()
}

export async function fetchStats() {
  const res = await fetch(`${BASE}/stats`)
  if (!res.ok) throw new Error('Failed to fetch stats')
  return res.json()
}

export async function triggerScraper(source) {
  const res = await fetch(`${BASE}/scraper/run/${source}`, { method: 'POST' })
  if (!res.ok) throw new Error('Failed to start scraper')
  return res.json()
}

// Batch match scores for job-card badges. Returns { [jobId]: score }; score -1 = no data yet.
export async function fetchMatchScores(jobIds) {
  if (!jobIds || jobIds.length === 0) return {}
  const res = await fetch(`${BASE}/match/${ACTIVE_USER_ID}/scores?jobs=${jobIds.join(',')}`)
  if (!res.ok) return {}
  return res.json()
}

// Generic get/post helpers — used by MatchPanel for the full match breakdown
// and the manual "extract now" trigger. Kept separate from the typed helpers
// above so existing call sites don't need to change.
export const api = {
  get(path) {
    return fetch(`${BASE}${path}`).then(res => {
      if (!res.ok) throw new Error(`GET ${path} failed`)
      return res.json()
    })
  },
  post(path, body) {
    return fetch(`${BASE}${path}`, {
      method: 'POST',
      headers: body ? { 'Content-Type': 'application/json' } : undefined,
      body: body ? JSON.stringify(body) : undefined,
    }).then(res => {
      if (!res.ok) throw new Error(`POST ${path} failed`)
      return res.json().catch(() => ({}))
    })
  },
}
