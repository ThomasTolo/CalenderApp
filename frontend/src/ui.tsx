import { useEffect, useMemo, useRef, useState } from 'react'
import { api, getToken, isApiError, setToken } from './api'
import type {
  CalendarItemCreateRequest,
  CalendarItemResponse,
  CalendarItemType,
  CalendarMonthResponse,
  ImportanceLevel,
  NotificationResponse,
} from './types'
import {
  addMonths,
  endOfMonth,
  fromIsoDate,
  monthLabel,
  pad2,
  startOfMonth,
  toIsoDate,
  weekdayIndexMondayFirst,
} from './date'
import { useRawWebSocket } from './hooks/useRawWebSocket'

type Toast = { kind: 'error' | 'info'; message: string }

const ITEM_TYPES: CalendarItemType[] = ['SCHOOL', 'WORKOUT', 'MAIN_MEAL', 'JOB']
const IMPORTANCE: ImportanceLevel[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']

function classNames(...xs: Array<string | false | null | undefined>) {
  return xs.filter(Boolean).join(' ')
}

function toTimeInput(v: string | null): string {
  if (!v) return ''
  // backend might send HH:mm:ss
  return v.length >= 5 ? v.slice(0, 5) : v
}

function fromTimeInput(v: string): string | undefined {
  const t = v.trim()
  return t ? t : undefined
}

function importanceDot(level: ImportanceLevel): string {
  switch (level) {
    case 'LOW':
      return 'dot dot-low'
    case 'MEDIUM':
      return 'dot dot-med'
    case 'HIGH':
      return 'dot dot-high'
    case 'CRITICAL':
      return 'dot dot-crit'
  }
}

function compareItems(a: CalendarItemResponse, b: CalendarItemResponse): number {
  const d = a.date.localeCompare(b.date)
  if (d !== 0) return d
  const t = (a.startTime ?? '').localeCompare(b.startTime ?? '')
  if (t !== 0) return t
  return a.id - b.id
}

function upsertItem(list: CalendarItemResponse[], item: CalendarItemResponse): CalendarItemResponse[] {
  const idx = list.findIndex((x) => x.id === item.id)
  const next = idx >= 0 ? list.map((x) => (x.id === item.id ? item : x)) : [...list, item]
  next.sort(compareItems)
  return next
}

function removeItem(list: CalendarItemResponse[], id: number): CalendarItemResponse[] {
  const next = list.filter((x) => x.id !== id)
  next.sort(compareItems)
  return next
}

export function GlassApp() {
  const [token, setTokenState] = useState<string | null>(() => getToken())
  const [toast, setToast] = useState<Toast | null>(null)

  useEffect(() => {
    if (!toast) return
    const t = window.setTimeout(() => setToast(null), 3500)
    return () => window.clearTimeout(t)
  }, [toast])

  const onAuth = (t: string) => {
    setToken(t)
    setTokenState(t)
  }

  const onLogout = () => {
    setToken(null)
    setTokenState(null)
  }

  return (
    <div className="app-bg">
      <div className="app-shell">
        <header className="topbar glass">
          <div className="brand">
            <div className="brand-mark" />
            <div>
              <div className="brand-title">CalenderApp</div>
              <div className="brand-subtitle">Plan. Focus. Repeat.</div>
            </div>
          </div>
          <div className="topbar-right">
            {token ? (
              <button className="btn btn-ghost" onClick={onLogout}>
                Log out
              </button>
            ) : null}
          </div>
        </header>

        <main className="main">
          {token ? (
            <CalendarHome
              onToast={(t) => setToast(t)}
              token={token}
              onTokenInvalid={() => {
                setToast({ kind: 'error', message: 'Session expired. Please log in again.' })
                onLogout()
              }}
            />
          ) : (
            <AuthCard
              onToast={(t) => setToast(t)}
              onAuthed={(t) => onAuth(t)}
            />
          )}
        </main>

        {toast ? (
          <div className={classNames('toast', toast.kind === 'error' ? 'toast-error' : 'toast-info')}>
            {toast.message}
          </div>
        ) : null}
      </div>
    </div>
  )
}

function AuthCard({
  onAuthed,
  onToast,
}: {
  onAuthed: (token: string) => void
  onToast: (t: Toast) => void
}) {
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async () => {
    if (!username.trim() || !password.trim()) {
      onToast({ kind: 'error', message: 'Username and password required.' })
      return
    }

    setLoading(true)
    try {
      const res =
        mode === 'login'
          ? await api.login(username.trim(), password)
          : await api.register(username.trim(), password)
      onAuthed(res.token)
    } catch (e) {
      onToast({ kind: 'error', message: isApiError(e) ? e.message : 'Auth failed.' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-wrap">
      <div className="glass auth-card">
        <div className="auth-tabs">
          <button
            className={classNames('tab', mode === 'login' && 'tab-active')}
            onClick={() => setMode('login')}
            type="button"
          >
            Login
          </button>
          <button
            className={classNames('tab', mode === 'register' && 'tab-active')}
            onClick={() => setMode('register')}
            type="button"
          >
            Register
          </button>
        </div>

        <div className="auth-form">
          <label className="field">
            <span>Username</span>
            <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" />
          </label>
          <label className="field">
            <span>Password</span>
            <input
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              type="password"
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
            />
          </label>

          <button className="btn" onClick={submit} disabled={loading}>
            {loading ? 'Please wait…' : mode === 'login' ? 'Login' : 'Create account'}
          </button>

          <div className="auth-hint">
          </div>
        </div>
      </div>
    </div>
  )
}

function CalendarHome({
  token,
  onToast,
  onTokenInvalid,
}: {
  token: string
  onToast: (t: Toast) => void
  onTokenInvalid: () => void
}) {
  const [cursorMonth, setCursorMonth] = useState(() => startOfMonth(new Date()))
  const [selectedDate, setSelectedDate] = useState(() => toIsoDate(new Date()))
  const [monthData, setMonthData] = useState<CalendarMonthResponse | null>(null)
  const [dayItems, setDayItems] = useState<CalendarItemResponse[] | null>(null)
  const [notifications, setNotifications] = useState<NotificationResponse[]>([])
  const [bellOpen, setBellOpen] = useState(false)

  const pendingRefreshRef = useRef(false)

  const refreshMonth = async () => {
    const y = cursorMonth.getFullYear()
    const m = cursorMonth.getMonth() + 1
    try {
      const res = await api.getMonth(y, m)
      setMonthData(res)
    } catch (e) {
      if (isApiError(e) && (e.status === 401 || e.status === 403)) return onTokenInvalid()
      onToast({ kind: 'error', message: 'Failed loading month.' })
    }
  }

  const refreshDay = async (iso: string) => {
    try {
      const res = await api.getDay(iso)
      setDayItems(res)
    } catch (e) {
      if (isApiError(e) && (e.status === 401 || e.status === 403)) return onTokenInvalid()
      onToast({ kind: 'error', message: 'Failed loading day.' })
    }
  }

  const refreshNotifications = async () => {
    try {
      const unread = await api.listUnreadNotifications()
      setNotifications(unread)
    } catch (e) {
      if (isApiError(e) && (e.status === 401 || e.status === 403)) return onTokenInvalid()
      // ignore if backend not ready
    }
  }

  useEffect(() => {
    void refreshMonth()
    void refreshDay(selectedDate)
    void refreshNotifications()
  }, [cursorMonth.getFullYear(), cursorMonth.getMonth()])

  useEffect(() => {
    void refreshDay(selectedDate)
  }, [selectedDate])

  useEffect(() => {
    const t = window.setInterval(() => {
      void refreshNotifications()
    }, 20000)
    return () => window.clearInterval(t)
  }, [])

  useRawWebSocket({
    enabled: Boolean(token),
    onMessage: (_data) => {
      if (pendingRefreshRef.current) return
      pendingRefreshRef.current = true
      window.setTimeout(() => {
        pendingRefreshRef.current = false
        const data = _data as unknown

        const isObject = typeof data === 'object' && data !== null
        const has = (k: string) => isObject && k in (data as Record<string, unknown>)

        // The backend broadcasts 2 shapes:
        // - calendar item event: { eventId, type, userId, itemId, date, occurredAt }
        // - notification event: { eventId, type, userId, notificationId, ... }
        const isCalendarEvent = has('itemId') && has('date')
        const isNotificationEvent = has('notificationId')

        if (isNotificationEvent) {
          void refreshNotifications()
          return
        }

        if (isCalendarEvent) {
          void refreshMonth()
          void refreshDay(selectedDate)
          return
        }

        // Fallback: keep UI consistent if message is unknown.
        void refreshNotifications()
        void refreshMonth()
        void refreshDay(selectedDate)
      }, 150)
    },
  })

  const monthItemsByDate = useMemo(() => {
    const map = new Map<string, CalendarItemResponse[]>()
    const items = monthData?.items ?? []
    for (const it of items) {
      const arr = map.get(it.date) ?? []
      arr.push(it)
      map.set(it.date, arr)
    }
    for (const arr of map.values()) {
      arr.sort((a, b) => (a.startTime ?? '').localeCompare(b.startTime ?? ''))
    }
    return map
  }, [monthData])

  const grid = useMemo(() => {
    const start = startOfMonth(cursorMonth)
    const end = endOfMonth(cursorMonth)
    const leading = weekdayIndexMondayFirst(start)
    const daysInMonth = end.getDate()

    const cells: Array<{ iso: string | null; day: number | null }> = []
    for (let i = 0; i < leading; i++) cells.push({ iso: null, day: null })

    for (let day = 1; day <= daysInMonth; day++) {
      const d = new Date(cursorMonth.getFullYear(), cursorMonth.getMonth(), day)
      cells.push({ iso: toIsoDate(d), day })
    }

    while (cells.length % 7 !== 0) cells.push({ iso: null, day: null })
    while (cells.length < 42) cells.push({ iso: null, day: null })

    return cells
  }, [cursorMonth])

  const selectedDateObj = fromIsoDate(selectedDate)
  const selectedLabel = selectedDateObj.toLocaleDateString(undefined, {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })

  const createOrUpdate = async (payload: CalendarItemCreateRequest, editingId: number | null) => {
    try {
      if (editingId) {
        const updated = await api.updateItem(editingId, payload)
        if (updated.date === selectedDate) {
          setDayItems((prev) => upsertItem(prev ?? [], updated))
        }
        setMonthData((prev) => {
          if (!prev) return prev
          const monthPrefix = `${prev.year}-${pad2(prev.month)}`
          if (!updated.date.startsWith(monthPrefix)) return prev
          return { ...prev, items: upsertItem(prev.items ?? [], updated) }
        })
      } else {
        const created = await api.createItem(payload)
        if (created.date === selectedDate) {
          setDayItems((prev) => upsertItem(prev ?? [], created))
        }
        setMonthData((prev) => {
          if (!prev) return prev
          const monthPrefix = `${prev.year}-${pad2(prev.month)}`
          if (!created.date.startsWith(monthPrefix)) return prev
          return { ...prev, items: upsertItem(prev.items ?? [], created) }
        })
      }

      // Keep backend as source of truth, but don't block the UX on roundtrips.
      void refreshMonth()
      void refreshDay(selectedDate)
    } catch (e) {
      if (isApiError(e) && (e.status === 401 || e.status === 403)) return onTokenInvalid()
      onToast({ kind: 'error', message: isApiError(e) ? e.message : 'Save failed.' })
    }
  }

  const deleteItem = async (id: number) => {
    try {
      await api.deleteItem(id)
      setDayItems((prev) => removeItem(prev ?? [], id))
      setMonthData((prev) => {
        if (!prev) return prev
        return { ...prev, items: removeItem(prev.items ?? [], id) }
      })

      void refreshMonth()
      void refreshDay(selectedDate)
    } catch (e) {
      if (isApiError(e) && (e.status === 401 || e.status === 403)) return onTokenInvalid()
      onToast({ kind: 'error', message: 'Delete failed.' })
    }
  }

  const unreadCount = notifications.filter((n) => !n.read).length

  return (
    <div className="layout">
      <section className="glass panel calendar-panel">
        <div className="panel-head">
          <div className="month-nav">
            <button className="icon-btn" onClick={() => setCursorMonth((d) => addMonths(d, -1))}>
              ‹
            </button>
            <div className="month-title">{monthLabel(cursorMonth)}</div>
            <button className="icon-btn" onClick={() => setCursorMonth((d) => addMonths(d, 1))}>
              ›
            </button>
          </div>

          <div className="panel-actions">
            <div className="bell-wrap">
              <button
                className={classNames('icon-btn', 'bell-btn', bellOpen && 'icon-btn-active')}
                onClick={() => setBellOpen((v) => !v)}
                aria-label="Notifications"
              >
                🔔
                {unreadCount > 0 ? <span className="badge">{unreadCount}</span> : null}
              </button>
              {bellOpen ? (
                <div className="glass bell-pop">
                  <div className="bell-title">Notifications</div>
                  {notifications.length === 0 ? (
                    <div className="bell-empty">No unread notifications.</div>
                  ) : (
                    <div className="bell-list">
                      {notifications.slice(0, 12).map((n) => (
                        <button
                          key={n.id}
                          className="bell-item"
                          onClick={async () => {
                            try {
                              await api.markNotificationRead(n.id)
                              await refreshNotifications()
                            } catch {
                              // ignore
                            }
                          }}
                        >
                          <span className={classNames('pill', `pill-${n.importance.toLowerCase()}`)}>
                            {n.importance}
                          </span>
                          <span className="bell-msg">{n.message}</span>
                          <span className="bell-time">
                            {new Date(n.createdAt).toLocaleTimeString(undefined, {
                              hour: '2-digit',
                              minute: '2-digit',
                            })}
                          </span>
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              ) : null}
            </div>
          </div>
        </div>

        <div className="weekdays">
          {['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map((w) => (
            <div key={w} className="weekday">
              {w}
            </div>
          ))}
        </div>

        <div className="grid">
          {grid.map((cell, idx) => {
            if (!cell.iso) {
              return <div key={idx} className="day day-empty" />
            }

            const items = monthItemsByDate.get(cell.iso) ?? []
            const selected = cell.iso === selectedDate
            const today = cell.iso === toIsoDate(new Date())

            return (
              <button
                key={cell.iso}
                className={classNames('day', selected && 'day-selected', today && 'day-today')}
                onClick={() => setSelectedDate(cell.iso!)}
                type="button"
              >
                <div className="day-top">
                  <div className="day-num">{cell.day}</div>
                  {items.length ? <div className="day-count">{items.length}</div> : null}
                </div>
                <div className="day-dots">
                  {items.slice(0, 4).map((it) => (
                    <span key={it.id} className={importanceDot(it.importance)} />
                  ))}
                </div>
                <div className="day-mini">
                  {items.slice(0, 2).map((it) => (
                    <div key={it.id} className="mini-line">
                      <span className="mini-time">{it.startTime ? toTimeInput(it.startTime) : '--:--'}</span>
                      <span className="mini-title">{it.title}</span>
                    </div>
                  ))}
                </div>
              </button>
            )
          })}
        </div>
      </section>

      <section className="glass panel editor-panel">
        <div className="panel-head">
          <div>
            <div className="panel-title">{selectedLabel}</div>
            <div className="panel-subtitle">
              {pad2(selectedDateObj.getDate())}.{pad2(selectedDateObj.getMonth() + 1)}.{selectedDateObj.getFullYear()}
            </div>
          </div>
        </div>

        <DayEditor
          dateIso={selectedDate}
          items={dayItems ?? []}
          onSave={createOrUpdate}
          onDelete={deleteItem}
        />
      </section>
    </div>
  )
}

function DayEditor({
  dateIso,
  items,
  onSave,
  onDelete,
}: {
  dateIso: string
  items: CalendarItemResponse[]
  onSave: (payload: CalendarItemCreateRequest, editingId: number | null) => Promise<void>
  onDelete: (id: number) => Promise<void>
}) {
  const [editingId, setEditingId] = useState<number | null>(null)
  const [type, setType] = useState<CalendarItemType>('SCHOOL')
  const [importance, setImportance] = useState<ImportanceLevel>('MEDIUM')
  const [title, setTitle] = useState('')
  const [log, setLog] = useState('')
  const [startTime, setStartTime] = useState('')
  const [endTime, setEndTime] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    setEditingId(null)
    setType('SCHOOL')
    setImportance('MEDIUM')
    setTitle('')
    setLog('')
    setStartTime('')
    setEndTime('')
  }, [dateIso])

  const selectForEdit = (it: CalendarItemResponse) => {
    setEditingId(it.id)
    setType(it.type)
    setImportance(it.importance)
    setTitle(it.title)
    setLog(it.log ?? '')
    setStartTime(toTimeInput(it.startTime))
    setEndTime(toTimeInput(it.endTime))
  }

  const clearForm = () => {
    setEditingId(null)
    setType('SCHOOL')
    setImportance('MEDIUM')
    setTitle('')
    setLog('')
    setStartTime('')
    setEndTime('')
  }

  const submit = async () => {
    if (!title.trim()) return

    const payload: CalendarItemCreateRequest = {
      date: dateIso,
      startTime: fromTimeInput(startTime),
      endTime: fromTimeInput(endTime),
      type,
      importance,
      title: title.trim(),
      log: log.trim() ? log.trim() : undefined,
    }

    setSaving(true)
    try {
      await onSave(payload, editingId)
      clearForm()
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="editor">
      <div className="list">
        <div className="list-title">Items</div>
        {items.length === 0 ? <div className="empty">No items yet.</div> : null}
        <div className="list-scroll">
          {items.map((it) => (
            <div key={it.id} className="item-row">
              <button className="item-main" onClick={() => selectForEdit(it)} type="button">
                <div className="item-top">
                  <span className={classNames('pill', `pill-${it.importance.toLowerCase()}`)}>
                    {it.importance}
                  </span>
                  <span className="pill pill-type">{it.type}</span>
                </div>
                <div className="item-title">{it.title}</div>
                <div className="item-sub">
                  <span>{it.startTime ? toTimeInput(it.startTime) : '--:--'}</span>
                  <span>→</span>
                  <span>{it.endTime ? toTimeInput(it.endTime) : '--:--'}</span>
                </div>
                {it.log ? <div className="item-log">{it.log}</div> : null}
              </button>
              <button className="icon-btn icon-btn-danger" onClick={() => onDelete(it.id)} type="button">
                ✕
              </button>
            </div>
          ))}
        </div>
      </div>

      <div className="form">
        <div className="form-title">{editingId ? 'Edit item' : 'Add item'}</div>
        <div className="form-grid">
          <label className="field">
            <span>Type</span>
            <select value={type} onChange={(e) => setType(e.target.value as CalendarItemType)}>
              {ITEM_TYPES.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
          </label>

          <label className="field">
            <span>Importance</span>
            <select value={importance} onChange={(e) => setImportance(e.target.value as ImportanceLevel)}>
              {IMPORTANCE.map((l) => (
                <option key={l} value={l}>
                  {l}
                </option>
              ))}
            </select>
          </label>

          <label className="field">
            <span>Start</span>
            <input value={startTime} onChange={(e) => setStartTime(e.target.value)} placeholder="HH:MM" />
          </label>

          <label className="field">
            <span>End</span>
            <input value={endTime} onChange={(e) => setEndTime(e.target.value)} placeholder="HH:MM" />
          </label>

          <label className="field field-wide">
            <span>Title</span>
            <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="What’s happening?" />
          </label>

          <label className="field field-wide">
            <span>Log</span>
            <textarea value={log} onChange={(e) => setLog(e.target.value)} placeholder="Notes…" rows={4} />
          </label>
        </div>

        <div className="form-actions">
          <button className="btn btn-ghost" onClick={clearForm} type="button">
            Clear
          </button>
          <button className="btn" onClick={submit} disabled={saving || !title.trim()} type="button">
            {saving ? 'Saving…' : editingId ? 'Save changes' : 'Add'}
          </button>
        </div>
      </div>
    </div>
  )
}
