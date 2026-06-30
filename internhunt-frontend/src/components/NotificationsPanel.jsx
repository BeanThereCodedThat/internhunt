import { useState, useEffect } from 'react'
import { api } from '../api'

/**
 * Notifications Panel — shows deadline alerts and unread notifications.
 * Requires a userId prop (the logged-in user's ID).
 *
 * Usage: <NotificationsPanel userId={1} onClose={() => setShowNotifs(false)} />
 */
export default function NotificationsPanel({ userId, onClose }) {
  const [notifications, setNotifications] = useState([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState('unread') // 'all' | 'unread'

  useEffect(() => {
    loadNotifications()
  }, [userId])

  async function loadNotifications() {
    setLoading(true)
    try {
      const data = await api.get(`/notifications/user/${userId}`)
      setNotifications(data)
    } catch (e) {
      console.error(e)
    }
    setLoading(false)
  }

  async function markRead(id) {
    try {
      await api.put(`/notifications/${id}/read`)
      setNotifications(ns => ns.map(n => n.id === id ? { ...n, isRead: true } : n))
    } catch (e) { console.error(e) }
  }

  async function markAllRead() {
    const unread = notifications.filter(n => !n.isRead)
    for (const n of unread) {
      try { await api.put(`/notifications/${n.id}/read`) } catch (e) {}
    }
    setNotifications(ns => ns.map(n => ({ ...n, isRead: true })))
  }

  const displayed = filter === 'unread'
    ? notifications.filter(n => !n.isRead)
    : notifications

  const unreadCount = notifications.filter(n => !n.isRead).length

  return (
    <div style={{
      position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)',
      display: 'flex', alignItems: 'flex-start', justifyContent: 'flex-end',
      zIndex: 200, padding: '70px 24px 24px',
    }} onClick={onClose}>
      <div style={{
        background: 'var(--bg2)', border: '1px solid var(--border)',
        borderRadius: 14, width: 420, maxHeight: '80vh',
        overflow: 'hidden', display: 'flex', flexDirection: 'column',
        boxShadow: '0 20px 60px rgba(0,0,0,0.5)',
      }} onClick={e => e.stopPropagation()}>

        {/* Header */}
        <div style={{
          padding: '16px 20px', borderBottom: '1px solid var(--border)',
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <span style={{ fontFamily: 'var(--font-head)', fontWeight: 700 }}>🔔 Notifications</span>
            {unreadCount > 0 && (
              <span style={{
                background: 'var(--accent)', color: '#fff',
                borderRadius: 20, padding: '1px 8px', fontSize: '0.72rem', fontWeight: 700,
              }}>{unreadCount}</span>
            )}
          </div>
          <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
            {unreadCount > 0 && (
              <button onClick={markAllRead} style={{
                background: 'none', color: 'var(--accent)', border: 'none',
                fontSize: '0.78rem', cursor: 'pointer', fontWeight: 600,
              }}>Mark all read</button>
            )}
            <button onClick={onClose} style={{ background: 'none', color: 'var(--muted)', border: 'none', fontSize: '1.2rem', cursor: 'pointer' }}>✕</button>
          </div>
        </div>

        {/* Filter tabs */}
        <div style={{
          display: 'flex', borderBottom: '1px solid var(--border)', padding: '0 20px',
        }}>
          {['unread', 'all'].map(f => (
            <button key={f} onClick={() => setFilter(f)} style={{
              background: 'none', border: 'none', padding: '10px 0', marginRight: 20,
              color: filter === f ? 'var(--text)' : 'var(--muted)',
              borderBottom: `2px solid ${filter === f ? 'var(--accent)' : 'transparent'}`,
              cursor: 'pointer', fontSize: '0.85rem', fontWeight: filter === f ? 600 : 400,
              transition: 'all 0.15s',
            }}>
              {f === 'unread' ? `Unread (${unreadCount})` : `All (${notifications.length})`}
            </button>
          ))}
        </div>

        {/* List */}
        <div style={{ overflowY: 'auto', flex: 1 }}>
          {loading ? (
            <div style={{ padding: 28, color: 'var(--muted)', textAlign: 'center' }}>Loading...</div>
          ) : displayed.length === 0 ? (
            <div style={{ padding: 40, textAlign: 'center' }}>
              <div style={{ fontSize: '2.5rem', marginBottom: 10 }}>
                {filter === 'unread' ? '✅' : '🔔'}
              </div>
              <div style={{ color: 'var(--muted)', fontSize: '0.88rem' }}>
                {filter === 'unread' ? "You're all caught up!" : 'No notifications yet.'}
              </div>
              {filter === 'unread' && notifications.length > 0 && (
                <button onClick={() => setFilter('all')} style={{
                  marginTop: 12, background: 'none', color: 'var(--accent)',
                  border: '1px solid var(--accent)', borderRadius: 8,
                  padding: '6px 14px', fontSize: '0.8rem', cursor: 'pointer',
                }}>View all</button>
              )}
            </div>
          ) : (
            displayed.map(n => (
              <NotifItem key={n.id} notification={n} onRead={markRead} />
            ))
          )}
        </div>
      </div>
    </div>
  )
}

function NotifItem({ notification: n, onRead }) {
  const isDeadline = n.message.includes('⏰') || n.message.toLowerCase().includes('deadline')
  const accent = isDeadline ? 'var(--yellow)' : 'var(--accent)'

  function timeAgo(dateStr) {
    if (!dateStr) return ''
    const diff = Date.now() - new Date(dateStr).getTime()
    const h = Math.floor(diff / 3600000)
    const d = Math.floor(h / 24)
    if (d > 0) return `${d}d ago`
    if (h > 0) return `${h}h ago`
    return 'just now'
  }

  return (
    <div style={{
      padding: '14px 20px', borderBottom: '1px solid var(--border)',
      background: n.isRead ? 'transparent' : `${accent}08`,
      display: 'flex', gap: 12, alignItems: 'flex-start',
      cursor: n.isRead ? 'default' : 'pointer',
      transition: 'background 0.15s',
    }} onClick={() => !n.isRead && onRead(n.id)}>

      {/* Unread dot */}
      <div style={{
        width: 8, height: 8, borderRadius: '50%',
        background: n.isRead ? 'transparent' : accent,
        marginTop: 5, flexShrink: 0,
      }} />

      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          fontSize: '0.85rem', color: n.isRead ? 'var(--muted)' : 'var(--text)',
          lineHeight: 1.45,
        }}>
          {n.message}
        </div>
        <div style={{ display: 'flex', gap: 10, marginTop: 5, alignItems: 'center' }}>
          <span style={{ color: 'var(--muted)', fontSize: '0.75rem' }}>{timeAgo(n.createdAt)}</span>
          {n.job && (
            <a href={n.job.sourceUrl || '#'} target="_blank" rel="noreferrer"
              onClick={e => e.stopPropagation()}
              style={{ color: accent, fontSize: '0.75rem', fontWeight: 600 }}>
              View listing →
            </a>
          )}
        </div>
      </div>

      {!n.isRead && (
        <button onClick={e => { e.stopPropagation(); onRead(n.id) }}
          style={{
            background: 'none', color: 'var(--muted)', border: 'none',
            fontSize: '0.75rem', cursor: 'pointer', whiteSpace: 'nowrap',
            flexShrink: 0,
          }}>
          Mark read
        </button>
      )}
    </div>
  )
}
