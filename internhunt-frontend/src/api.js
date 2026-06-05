const BASE = '/api'

// ─── Generic fetch helpers ─────────────────────────────────────────────────────

async function request(method, path, body) {
  const opts = {
    method,
    headers: { 'Content-Type': 'application/json' },
  }
  if (body !== undefined) opts.body = JSON.stringify(body)
  const res = await fetch(BASE + path, opts)
  if (!res.ok) {
    let msg = `HTTP ${res.status}`
    try { const j = await res.json(); msg = j.error || j.message || msg } catch {}
    throw new Error(msg)
  }
  if (res.status === 204) return null
  return res.json()
}

export const api = {
  get:    (path)        => request('GET',    path),
  post:   (path, body)  => request('POST',   path, body),
  put:    (path, body)  => request('PUT',    path, body),
  delete: (path)        => request('DELETE', path),
}

// ─── Jobs ─────────────────────────────────────────────────────────────────────

export async function fetchJobs({
  page = 0, size = 20, search = '', source = '', type = '', remote = null
} = {}) {
  const params = new URLSearchParams({ page, size })
  if (search)          params.set('search', search)
  if (source)          params.set('source', source)
  if (type)            params.set('type', type)
  if (remote !== null) params.set('remote', remote)
  const res = await fetch(`${BASE}/jobs?${params}`)
  if (!res.ok) throw new Error('Failed to fetch jobs')
  return res.json()
}

export async function fetchJob(id) {
  return api.get(`/jobs/${id}`)
}

// ─── Stats ────────────────────────────────────────────────────────────────────

export async function fetchStats() {
  return api.get('/stats')
}

// ─── Scrapers ─────────────────────────────────────────────────────────────────

export async function triggerScraper(source) {
  return api.post(`/scraper/run/${source}`)
}

export async function triggerAllScrapers() {
  return api.post('/scraper/run-all')
}

// ─── Users ────────────────────────────────────────────────────────────────────

export async function fetchUsers()              { return api.get('/users') }
export async function fetchUser(id)             { return api.get(`/users/${id}`) }
export async function createUser(data)          { return api.post('/users', data) }
export async function updateUser(id, data)      { return api.put(`/users/${id}`, data) }
export async function deleteUser(id)            { return api.delete(`/users/${id}`) }

// ─── Skills ───────────────────────────────────────────────────────────────────

export async function fetchAllSkills()          { return api.get('/skills') }

// User skills
export async function fetchUserSkills(userId)   { return api.get(`/users/${userId}/skills`) }
export async function addUserSkill(userId, skillId, proficiency = 1) {
  return api.post(`/users/${userId}/skills`, { skillId, proficiency })
}
export async function updateUserSkill(userId, skillId, proficiency) {
  return api.put(`/users/${userId}/skills/${skillId}`, { proficiency })
}
export async function removeUserSkill(userId, skillId) {
  return api.delete(`/users/${userId}/skills/${skillId}`)
}

// ─── Applications ─────────────────────────────────────────────────────────────

export async function fetchApplications(userId) {
  return api.get(`/applications/user/${userId}`)
}
export async function createApplication(data) {
  return api.post('/applications', data)
}
export async function updateApplication(id, data) {
  return api.put(`/applications/${id}`, data)
}
export async function deleteApplication(id) {
  return api.delete(`/applications/${id}`)
}

// ─── Notifications ────────────────────────────────────────────────────────────

export async function fetchNotifications(userId) {
  return api.get(`/notifications/user/${userId}`)
}
export async function fetchUnreadNotifications(userId) {
  return api.get(`/notifications/user/${userId}/unread`)
}
export async function markNotificationRead(id) {
  return api.put(`/notifications/${id}/read`)
}

// ─── Scam Reports ─────────────────────────────────────────────────────────────

export async function fetchScamReports()        { return api.get('/scam-reports') }
export async function checkCompanyScam(company) { return api.get(`/scam-reports/check?company=${encodeURIComponent(company)}`) }
