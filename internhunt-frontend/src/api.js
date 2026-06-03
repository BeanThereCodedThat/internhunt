const BASE = '/api'

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
