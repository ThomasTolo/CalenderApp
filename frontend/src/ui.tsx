import { useEffect, useMemo, useRef, useState } from 'react'
import { api, getToken, isApiError, setToken } from './api'
import type {
  CalendarItemCreateRequest,
  CalendarItemResponse,
  CalendarItemType,
  CalendarMonthResponse,
  ExerciseResponse,
  FixedCostFrequency,
  ImportanceLevel,
  NotificationResponse,
  SchoolItemKind,
  WorkoutEntryRequest,
  WorkoutTemplateResponse,
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

const ITEM_TYPES: CalendarItemType[] = ['SCHOOL', 'WORKOUT', 'MAIN_MEAL', 'JOB', 'FIXED_COST', 'BIRTHDAY', 'OTHER']
const IMPORTANCE: ImportanceLevel[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']

const PAGES: Array<{ label: string; type: CalendarItemType | null }> = [
  { label: 'Main', type: null },
  { label: 'Fixed Costs', type: 'FIXED_COST' },
  { label: 'Food', type: 'MAIN_MEAL' },
  { label: 'Workout', type: 'WORKOUT' },
  { label: 'School', type: 'SCHOOL' },
  { label: 'Work/Job', type: 'JOB' },
  { label: 'Birthdays', type: 'BIRTHDAY' },
  { label: 'Other', type: 'OTHER' },
]

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

function typeDot(type: CalendarItemType): string {
  return `dot dot-type-${type.toLowerCase()}`
}

function typeLine(type: CalendarItemType): string {
  return `mini-line mini-line-type-${type.toLowerCase()}`
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

function toNumberOrUndefined(v: string): number | undefined {
  const t = v.trim()
  if (!t) return undefined
  const n = Number(t)
  return Number.isFinite(n) ? n : undefined
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
  const [viewMode, setViewMode] = useState<'compact' | 'tablet' | 'detailed' | 'list' | 'week'>('detailed')
  const [typeFilter, setTypeFilter] = useState<CalendarItemType | null>(null)
  const [cursorMonth, setCursorMonth] = useState(() => startOfMonth(new Date()))
  const [selectedDate, setSelectedDate] = useState(() => toIsoDate(new Date()))
  const [monthData, setMonthData] = useState<CalendarMonthResponse | null>(null)
  const [weekMonths, setWeekMonths] = useState<Record<string, CalendarMonthResponse>>({})
  const [dayItems, setDayItems] = useState<CalendarItemResponse[] | null>(null)
  const [notifications, setNotifications] = useState<NotificationResponse[]>([])
  const [bellOpen, setBellOpen] = useState(false)

  const [workoutExercises, setWorkoutExercises] = useState<ExerciseResponse[]>([])
  const [workoutTemplates, setWorkoutTemplates] = useState<WorkoutTemplateResponse[]>([])

  const pendingRefreshRef = useRef(false)

  const refreshMonth = async () => {
    const y = cursorMonth.getFullYear()
    const m = cursorMonth.getMonth() + 1
    try {
      const res = await api.getMonth(y, m, typeFilter ?? undefined)
      setMonthData(res)
    } catch (e) {
      if (isApiError(e) && (e.status === 401 || e.status === 403)) return onTokenInvalid()
      onToast({ kind: 'error', message: 'Failed loading month.' })
    }
  }

  const refreshDay = async (iso: string) => {
    try {
      const res = await api.getDay(iso, typeFilter ?? undefined)
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

  const refreshWorkoutLibrary = async () => {
    try {
      const [ex, templates] = await Promise.all([api.listWorkoutExercises(), api.listWorkoutTemplates()])
      setWorkoutExercises(ex)
      setWorkoutTemplates(templates)
    } catch (e) {
      if (isApiError(e) && (e.status === 401 || e.status === 403)) return onTokenInvalid()
      // keep quiet unless user is actively using workout features
    }
  }

  useEffect(() => {
    void refreshMonth()
    void refreshNotifications()
    if (typeFilter === 'WORKOUT') void refreshWorkoutLibrary()
  }, [cursorMonth.getFullYear(), cursorMonth.getMonth(), typeFilter])

  useEffect(() => {
    void refreshDay(selectedDate)
  }, [selectedDate, typeFilter])

  useEffect(() => {
    setWeekMonths({})
  }, [typeFilter])

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

  const weekGrid = useMemo(() => {
    const d = fromIsoDate(selectedDate)
    const idx = weekdayIndexMondayFirst(d) // 0..6
    const start = new Date(d)
    start.setDate(d.getDate() - idx)
    const days: Array<{ iso: string; label: string }> = []
    for (let i = 0; i < 7; i++) {
      const day = new Date(start)
      day.setDate(start.getDate() + i)
      const iso = toIsoDate(day)
      const label = day.toLocaleDateString(undefined, { weekday: 'short', day: '2-digit', month: '2-digit' })
      days.push({ iso, label })
    }
    return days
  }, [selectedDate])

  useEffect(() => {
    if (viewMode !== 'week') return
    setCursorMonth((prev) => {
      const d = fromIsoDate(selectedDate)
      if (prev.getFullYear() === d.getFullYear() && prev.getMonth() === d.getMonth()) return prev
      return startOfMonth(d)
    })
  }, [viewMode, selectedDate])

  useEffect(() => {
    if (viewMode !== 'week') return

    const requiredKeys = Array.from(new Set(weekGrid.map((d) => d.iso.slice(0, 7))))
    const missing = requiredKeys.filter((k) => !(k in weekMonths))
    if (missing.length === 0) return

    void (async () => {
      try {
        const results = await Promise.all(
          missing.map(async (k) => {
            const y = Number(k.slice(0, 4))
            const m = Number(k.slice(5, 7))
            const res = await api.getMonth(y, m, typeFilter ?? undefined)
            return [k, res] as const
          }),
        )
        setWeekMonths((prev) => {
          const next = { ...prev }
          for (const [k, res] of results) next[k] = res
          return next
        })
      } catch (e) {
        if (isApiError(e) && (e.status === 401 || e.status === 403)) return onTokenInvalid()
        // keep week UI usable even if adjacent-month fetch fails
      }
    })()
  }, [viewMode, weekGrid, weekMonths, typeFilter, onTokenInvalid])

  const weekItemsByDate = useMemo(() => {
    const map = new Map<string, CalendarItemResponse[]>()
    for (const month of Object.values(weekMonths)) {
      for (const it of month.items ?? []) {
        const arr = map.get(it.date) ?? []
        arr.push(it)
        map.set(it.date, arr)
      }
    }
    for (const arr of map.values()) {
      arr.sort((a, b) => (a.startTime ?? '').localeCompare(b.startTime ?? ''))
    }
    return map
  }, [weekMonths])

  const matchesFilter = (it: CalendarItemResponse) => (typeFilter ? it.type === typeFilter : true)

  const applyUpdatedItem = (it: CalendarItemResponse) => {
    if (it.date === selectedDate) {
      setDayItems((prev) => {
        const list = prev ?? []
        return matchesFilter(it) ? upsertItem(list, it) : removeItem(list, it.id)
      })
    }

    setMonthData((prev) => {
      if (!prev) return prev
      const monthPrefix = `${prev.year}-${pad2(prev.month)}`
      if (!it.date.startsWith(monthPrefix)) return prev
      const nextItems = matchesFilter(it) ? upsertItem(prev.items ?? [], it) : removeItem(prev.items ?? [], it.id)
      return { ...prev, items: nextItems }
    })

    setWeekMonths((prev) => {
      const key = it.date.slice(0, 7)
      const month = prev[key]
      if (!month) return prev
      const nextItems = matchesFilter(it) ? upsertItem(month.items ?? [], it) : removeItem(month.items ?? [], it.id)
      return { ...prev, [key]: { ...month, items: nextItems } }
    })
  }

  const createOrUpdate = async (payload: CalendarItemCreateRequest, editingId: number | null): Promise<CalendarItemResponse | null> => {
    try {
      if (editingId) {
        const updated = await api.updateItem(editingId, payload)
        applyUpdatedItem(updated)
        return updated
      } else {
        const created = await api.createItem(payload)
        applyUpdatedItem(created)
        return created
      }

      // Keep backend as source of truth, but don't block the UX on roundtrips.
      void refreshMonth()
      void refreshDay(selectedDate)
    } catch (e) {
      if (isApiError(e) && (e.status === 401 || e.status === 403)) {
        onTokenInvalid()
        return null
      }
      onToast({ kind: 'error', message: isApiError(e) ? e.message : 'Save failed.' })
    }

    return null
  }

  const deleteItem = async (id: number) => {
    try {
      await api.deleteItem(id)
      setDayItems((prev) => removeItem(prev ?? [], id))
      setMonthData((prev) => {
        if (!prev) return prev
        return { ...prev, items: removeItem(prev.items ?? [], id) }
      })

      setWeekMonths((prev) => {
        const next: Record<string, CalendarMonthResponse> = { ...prev }
        for (const k of Object.keys(next)) {
          next[k] = { ...next[k], items: removeItem(next[k].items ?? [], id) }
        }
        return next
      })

      void refreshMonth()
      void refreshDay(selectedDate)
    } catch (e) {
      if (isApiError(e) && (e.status === 401 || e.status === 403)) return onTokenInvalid()
      onToast({ kind: 'error', message: isApiError(e) ? e.message : 'Delete failed.' })
    }
  }

  const toggleDone = async (it: CalendarItemResponse, done: boolean) => {
    try {
      const payload: CalendarItemCreateRequest = {
        date: it.date,
        startTime: fromTimeInput(toTimeInput(it.startTime)),
        endTime: fromTimeInput(toTimeInput(it.endTime)),
        type: it.type,
        importance: it.importance,
        title: it.title,
        log: it.log ?? undefined,
        done,
        amount: it.amount ?? undefined,
        schoolKind: it.schoolKind ?? undefined,
        fixedCostFrequency: it.fixedCostFrequency ?? undefined,
      }
      const updated = await api.updateItem(it.id, payload)
      applyUpdatedItem(updated)
      void refreshMonth()
      void refreshDay(selectedDate)
    } catch (e) {
      if (isApiError(e) && (e.status === 401 || e.status === 403)) return onTokenInvalid()
      onToast({ kind: 'error', message: isApiError(e) ? e.message : 'Update failed.' })
    }
  }

  const unreadCount = notifications.filter((n) => !n.read).length

  const modeDots = viewMode === 'compact' ? 4 : viewMode === 'tablet' ? 3 : 4
  const modeLines = viewMode === 'compact' ? 0 : viewMode === 'tablet' ? 1 : 2

  return (
    <div>
      <div className="page-tabs">
        {PAGES.map((p) => (
          <button
            key={p.label}
            className={classNames('tab', p.type === typeFilter && 'tab-active')}
            onClick={() => setTypeFilter(p.type)}
            type="button"
          >
            {p.label}
          </button>
        ))}
      </div>

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
            <label className="mode-select">
              <span>View</span>
              <select value={viewMode} onChange={(e) => setViewMode(e.target.value as typeof viewMode)}>
                <option value="compact">Compact</option>
                <option value="tablet">Tablet</option>
                <option value="detailed">Detailed</option>
                <option value="list">List</option>
                <option value="week">Week</option>
              </select>
            </label>

            {viewMode === 'week' ? (
              <div className="week-nav">
                <button
                  className="icon-btn"
                  type="button"
                  aria-label="Previous week"
                  onClick={() => {
                    const d = fromIsoDate(selectedDate)
                    const prev = new Date(d)
                    prev.setDate(d.getDate() - 7)
                    setSelectedDate(toIsoDate(prev))
                  }}
                >
                  ‹
                </button>
                <button
                  className="icon-btn"
                  type="button"
                  aria-label="Next week"
                  onClick={() => {
                    const d = fromIsoDate(selectedDate)
                    const next = new Date(d)
                    next.setDate(d.getDate() + 7)
                    setSelectedDate(toIsoDate(next))
                  }}
                >
                  ›
                </button>
              </div>
            ) : null}

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

        {viewMode === 'week' ? (
          <div className="week-grid">
            {weekGrid.map((d) => {
              const items = weekItemsByDate.get(d.iso) ?? []
              const selected = d.iso === selectedDate
              const today = d.iso === toIsoDate(new Date())
              return (
                <button
                  key={d.iso}
                  className={classNames('week-day', selected && 'week-day-selected', today && 'week-day-today')}
                  onClick={() => setSelectedDate(d.iso)}
                  type="button"
                >
                  <div className="week-day-head">
                    <div className="week-day-label">{d.label}</div>
                    {items.length ? <div className="day-count">{items.length}</div> : null}
                  </div>
                  <div className="week-day-items">
                    {items.slice(0, 6).map((it) => (
                      <div key={it.id} className={typeLine(it.type)}>
                        <span className="mini-time">{it.startTime ? toTimeInput(it.startTime) : '--:--'}</span>
                        <span className="mini-title">{it.title}</span>
                      </div>
                    ))}
                  </div>
                </button>
              )
            })}
          </div>
        ) : viewMode !== 'list' ? (
          <>
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
                const allDone = items.length > 0 && items.every((it) => it.done)

                return (
                  <button
                    key={cell.iso}
                    className={classNames('day', selected && 'day-selected', today && 'day-today')}
                    onClick={() => setSelectedDate(cell.iso!)}
                    type="button"
                  >
                    <div className="day-top">
                      <div className="day-num">{cell.day}</div>
                      <div className="day-top-right">
                        {allDone ? <div className="day-confirmed">✓</div> : null}
                        {items.length ? <div className="day-count">{items.length}</div> : null}
                      </div>
                    </div>
                    <div className="day-dots">
                      {items.slice(0, modeDots).map((it) => (
                        <span key={it.id} className={typeDot(it.type)} />
                      ))}
                    </div>
                    {modeLines > 0 ? (
                      <div className="day-mini">
                        {items.slice(0, modeLines).map((it) => (
                          <div key={it.id} className={typeLine(it.type)}>
                            <span className="mini-time">{it.startTime ? toTimeInput(it.startTime) : '--:--'}</span>
                            <span className="mini-title">{it.title}</span>
                          </div>
                        ))}
                      </div>
                    ) : null}
                  </button>
                )
              })}
            </div>
          </>
        ) : (
          <div className="month-list">
            {Array.from(monthItemsByDate.entries())
              .sort((a, b) => a[0].localeCompare(b[0]))
              .map(([iso, items]) => {
                const d = fromIsoDate(iso)
                const label = d.toLocaleDateString(undefined, { weekday: 'short', day: '2-digit', month: '2-digit' })
                const selected = iso === selectedDate
                const allDone = items.length > 0 && items.every((it) => it.done)
                return (
                  <button
                    key={iso}
                    className={classNames('month-list-day', selected && 'month-list-day-selected')}
                    onClick={() => setSelectedDate(iso)}
                    type="button"
                  >
                    <div className="month-list-head">
                      <div className="month-list-label">{label}</div>
                      <div className="month-list-right">
                        {allDone ? <div className="day-confirmed">✓</div> : null}
                        <div className="month-list-count">{items.length}</div>
                      </div>
                    </div>
                    <div className="month-list-items">
                      {items.slice(0, 6).map((it) => (
                        <div key={it.id} className={typeLine(it.type)}>
                          <span className="mini-time">{it.startTime ? toTimeInput(it.startTime) : '--:--'}</span>
                          <span className="mini-title">{it.title}</span>
                        </div>
                      ))}
                    </div>
                  </button>
                )
              })}
          </div>
        )}
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
          onToggleDone={toggleDone}
          defaultType={typeFilter ?? 'SCHOOL'}
          typeLocked={typeFilter !== null}
          onToast={onToast}
          onTokenInvalid={onTokenInvalid}
          workoutExercises={workoutExercises}
          workoutTemplates={workoutTemplates}
          refreshWorkoutLibrary={refreshWorkoutLibrary}
        />
      </section>
      </div>
    </div>
  )
}

function DayEditor({
  dateIso,
  items,
  onSave,
  onDelete,
  onToggleDone,
  defaultType,
  typeLocked,
  onToast,
  onTokenInvalid,
  workoutExercises,
  workoutTemplates,
  refreshWorkoutLibrary,
}: {
  dateIso: string
  items: CalendarItemResponse[]
  onSave: (payload: CalendarItemCreateRequest, editingId: number | null) => Promise<CalendarItemResponse | null>
  onDelete: (id: number) => Promise<void>
  onToggleDone: (it: CalendarItemResponse, done: boolean) => Promise<void>
  defaultType: CalendarItemType
  typeLocked: boolean
  onToast: (t: Toast) => void
  onTokenInvalid: () => void
  workoutExercises: ExerciseResponse[]
  workoutTemplates: WorkoutTemplateResponse[]
  refreshWorkoutLibrary: () => Promise<void>
}) {
  const [editingId, setEditingId] = useState<number | null>(null)
  const [type, setType] = useState<CalendarItemType>(defaultType)
  const [schoolKind, setSchoolKind] = useState<SchoolItemKind>('LECTURE')
  const [importance, setImportance] = useState<ImportanceLevel>('MEDIUM')
  const [title, setTitle] = useState('')
  const [log, setLog] = useState('')
  const [startTime, setStartTime] = useState('')
  const [endTime, setEndTime] = useState('')
  const [done, setDone] = useState(false)
  const [amount, setAmount] = useState('')
  const [fixedCostFrequency, setFixedCostFrequency] = useState<FixedCostFrequency>('MONTHLY')
  const [saving, setSaving] = useState(false)
  const [itemSearch, setItemSearch] = useState('')

  const [workoutEntries, setWorkoutEntries] = useState<
    Array<{ exerciseId: string; sets: string; reps: string; weight: string }>
  >([])
  const [workoutTemplatePick, setWorkoutTemplatePick] = useState<string>('')
  const [newExerciseName, setNewExerciseName] = useState('')
  const [newTemplateTitle, setNewTemplateTitle] = useState('')

  useEffect(() => {
    setEditingId(null)
    setType(defaultType)
    setSchoolKind('LECTURE')
    setImportance('MEDIUM')
    setTitle('')
    setLog('')
    setStartTime('')
    setEndTime('')
    setDone(false)
    setAmount('')
    setFixedCostFrequency('MONTHLY')
    setWorkoutEntries([])
    setWorkoutTemplatePick('')
    setItemSearch('')
  }, [dateIso, defaultType])

  const filteredItems = useMemo(() => {
    const q = itemSearch.trim().toLowerCase()
    if (!q) return items
    return items.filter((it) => {
      const title = it.title.toLowerCase()
      const log = (it.log ?? '').toLowerCase()
      return title.includes(q) || log.includes(q)
    })
  }, [items, itemSearch])

  const isHolidayItem = (it: CalendarItemResponse) =>
    it.type === 'OTHER' && (it.title.startsWith('Merkedag:') || it.title.startsWith('Helligdag:'))

  const selectForEdit = (it: CalendarItemResponse) => {
    if (isHolidayItem(it)) {
      onToast({ kind: 'info', message: 'Merkedager/helligdager kan ikke endres.' })
      return
    }
    setEditingId(it.id)
    setType(it.type)
    setSchoolKind(it.schoolKind ?? 'LECTURE')
    setImportance(it.importance)
    setTitle(it.title)
    setLog(it.log ?? '')
    setStartTime(toTimeInput(it.startTime))
    setEndTime(toTimeInput(it.endTime))
    setDone(it.done)
    setAmount(it.amount != null ? String(it.amount) : '')
    setFixedCostFrequency(it.fixedCostFrequency ?? 'MONTHLY')

    if (it.type === 'WORKOUT') {
      void (async () => {
        try {
          await refreshWorkoutLibrary()
          const session = await api.getWorkoutSession(it.id)
          setWorkoutEntries(
            session.entries.map((e) => ({
              exerciseId: String(e.exerciseId),
              sets: String(e.sets),
              reps: String(e.reps),
              weight: e.weight != null ? String(e.weight) : '',
            })),
          )
        } catch (e) {
          if (isApiError(e) && (e.status === 401 || e.status === 403)) return onTokenInvalid()
          onToast({ kind: 'error', message: isApiError(e) ? e.message : 'Failed loading workout session.' })
        }
      })()
    } else {
      setWorkoutEntries([])
      setWorkoutTemplatePick('')
    }
  }

  const clearForm = () => {
    setEditingId(null)
    setType(defaultType)
    setSchoolKind('LECTURE')
    setImportance('MEDIUM')
    setTitle('')
    setLog('')
    setStartTime('')
    setEndTime('')
    setDone(false)
    setAmount('')
    setFixedCostFrequency('MONTHLY')
    setWorkoutEntries([])
    setWorkoutTemplatePick('')
  }

  const showTimes = type !== 'FIXED_COST' && type !== 'MAIN_MEAL' && type !== 'BIRTHDAY'
  const isSchool = type === 'SCHOOL'
  const isCompulsory = isSchool && schoolKind === 'COMPULSORY'
  const isWorkout = type === 'WORKOUT'
  const isWorkoutTab = typeLocked && defaultType === 'WORKOUT'

  const supportsAllDay = type === 'OTHER'
  const [allDay, setAllDay] = useState(false)

  useEffect(() => {
    if (!supportsAllDay) {
      setAllDay(false)
      return
    }
    setAllDay(!startTime.trim() && !endTime.trim())
  }, [supportsAllDay, startTime, endTime])

  useEffect(() => {
    if (type === 'WORKOUT') void refreshWorkoutLibrary()
  }, [type])

  const applyTemplate = (templateId: string) => {
    setWorkoutTemplatePick(templateId)
    const idNum = Number(templateId)
    if (!Number.isFinite(idNum)) return
    const tpl = workoutTemplates.find((t) => t.id === idNum)
    if (!tpl) return
    setWorkoutEntries(
      tpl.entries.map((e) => ({
        exerciseId: String(e.exerciseId),
        sets: String(e.sets),
        reps: String(e.reps),
        weight: e.weight != null ? String(e.weight) : '',
      })),
    )
  }

  const addWorkoutRow = () => {
    setWorkoutEntries((prev) => [...prev, { exerciseId: '', sets: '3', reps: '10', weight: '' }])
  }

  const addExerciseToWorkout = (exerciseId: number) => {
    const id = String(exerciseId)
    setWorkoutEntries((prev) => {
      // Avoid duplicates when building templates quickly.
      if (prev.some((r) => r.exerciseId === id)) return prev
      return [...prev, { exerciseId: id, sets: '3', reps: '10', weight: '' }]
    })
  }

  const updateWorkoutRow = (idx: number, patch: Partial<(typeof workoutEntries)[number]>) => {
    setWorkoutEntries((prev) => prev.map((r, i) => (i === idx ? { ...r, ...patch } : r)))
  }

  const removeWorkoutRow = (idx: number) => {
    setWorkoutEntries((prev) => prev.filter((_, i) => i !== idx))
  }

  const toWorkoutRequestEntries = (): WorkoutEntryRequest[] => {
    return workoutEntries
      .map((r) => {
        const exerciseId = Number(r.exerciseId)
        const sets = Number(r.sets)
        const reps = Number(r.reps)
        const weight = toNumberOrUndefined(r.weight)
        if (!Number.isFinite(exerciseId) || !Number.isFinite(sets) || !Number.isFinite(reps)) return null
        return {
          exerciseId,
          sets,
          reps,
          ...(weight != null ? { weight } : {}),
        }
      })
      .filter(Boolean) as WorkoutEntryRequest[]
  }

  const submit = async () => {
    if (!title.trim()) return

    const payload: CalendarItemCreateRequest = {
      date: dateIso,
      startTime: showTimes && !isCompulsory && !(supportsAllDay && allDay) ? fromTimeInput(startTime) : undefined,
      endTime: showTimes ? fromTimeInput(endTime) : undefined,
      type,
      importance: isCompulsory ? importance : 'MEDIUM',
      title: title.trim(),
      log: log.trim() ? log.trim() : undefined,
      done,
      amount: amount.trim() ? Number(amount.trim()) : undefined,
      schoolKind: isSchool ? schoolKind : undefined,
      fixedCostFrequency: type === 'FIXED_COST' ? fixedCostFrequency : undefined,
    }

    setSaving(true)
    try {
      const saved = await onSave(payload, editingId)
      if (!saved) return

      if (saved.type === 'WORKOUT') {
        try {
          const entries = toWorkoutRequestEntries()
          await api.updateWorkoutSession(saved.id, { entries })
        } catch (e) {
          if (isApiError(e) && (e.status === 401 || e.status === 403)) return onTokenInvalid()
          onToast({ kind: 'error', message: isApiError(e) ? e.message : 'Failed saving workout session.' })
        }
      }

      clearForm()
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="editor">
      <div className="list">
        <div className="list-title">Items</div>

        {isWorkoutTab ? (
          <div className="workout-library">
            <div className="workout-lib-title">Workout library</div>

            <div className="workout-lib-block">
              <div className="workout-lib-sub">Exercises</div>
              <div className="workout-lib-row">
                <input
                  value={newExerciseName}
                  onChange={(e) => setNewExerciseName(e.target.value)}
                  placeholder="Add exercise…"
                />
                <button
                  className="btn"
                  type="button"
                  onClick={async () => {
                    const name = newExerciseName.trim()
                    if (!name) return
                    try {
                      await api.createWorkoutExercise(name)
                      setNewExerciseName('')
                      await refreshWorkoutLibrary()
                    } catch (e) {
                      if (isApiError(e) && (e.status === 401 || e.status === 403)) return onTokenInvalid()
                      onToast({ kind: 'error', message: isApiError(e) ? e.message : 'Failed adding exercise.' })
                    }
                  }}
                >
                  Add
                </button>
              </div>
              <div className="workout-lib-list">
                {workoutExercises.map((ex) => (
                  <div key={ex.id} className="workout-lib-item">
                    <button
                      className="btn btn-ghost"
                      type="button"
                      onClick={() => addExerciseToWorkout(ex.id)}
                      title="Add to workout/template"
                    >
                      {ex.name}
                    </button>
                    <div className="workout-lib-actions">
                      <button
                        className="icon-btn"
                        type="button"
                        onClick={() => addExerciseToWorkout(ex.id)}
                        aria-label="Add exercise to workout/template"
                        title="Add"
                      >
                        +
                      </button>
                      <button
                        className="icon-btn icon-btn-danger"
                        type="button"
                        onClick={async () => {
                          try {
                            await api.deleteWorkoutExercise(ex.id)
                            await refreshWorkoutLibrary()
                          } catch (e) {
                            if (isApiError(e) && (e.status === 401 || e.status === 403)) return onTokenInvalid()
                            onToast({ kind: 'error', message: isApiError(e) ? e.message : 'Failed deleting exercise.' })
                          }
                        }}
                        aria-label="Delete exercise"
                        title="Delete"
                      >
                        ✕
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="workout-lib-block">
              <div className="workout-lib-sub">Templates</div>
              <div className="workout-lib-row">
                <input
                  value={newTemplateTitle}
                  onChange={(e) => setNewTemplateTitle(e.target.value)}
                  placeholder="Template title…"
                />
                <button
                  className="btn"
                  type="button"
                  onClick={async () => {
                    const title = newTemplateTitle.trim()
                    if (!title) return
                    try {
                      const entries = toWorkoutRequestEntries()
                      if (entries.length === 0) {
                        onToast({ kind: 'error', message: 'Add at least one exercise row first.' })
                        return
                      }
                      await api.createWorkoutTemplate({ title, entries })
                      setNewTemplateTitle('')
                      await refreshWorkoutLibrary()
                    } catch (e) {
                      if (isApiError(e) && (e.status === 401 || e.status === 403)) return onTokenInvalid()
                      onToast({ kind: 'error', message: isApiError(e) ? e.message : 'Failed saving template.' })
                    }
                  }}
                >
                  Save
                </button>
              </div>
              <div className="workout-lib-list">
                {workoutTemplates.map((t) => (
                  <div key={t.id} className="workout-lib-item">
                    <button className="btn btn-ghost" type="button" onClick={() => applyTemplate(String(t.id))}>
                      {t.title}
                    </button>
                    <button
                      className="icon-btn icon-btn-danger"
                      type="button"
                      onClick={async () => {
                        try {
                          await api.deleteWorkoutTemplate(t.id)
                          await refreshWorkoutLibrary()
                        } catch (e) {
                          if (isApiError(e) && (e.status === 401 || e.status === 403)) return onTokenInvalid()
                          onToast({ kind: 'error', message: isApiError(e) ? e.message : 'Failed deleting template.' })
                        }
                      }}
                      aria-label="Delete template"
                    >
                      ✕
                    </button>
                  </div>
                ))}
              </div>
            </div>
          </div>
        ) : null}

        <label className="field">
          <span>Search</span>
          <input value={itemSearch} onChange={(e) => setItemSearch(e.target.value)} placeholder="Title or log…" />
        </label>

        {filteredItems.length === 0 ? (
          <div className="empty">{items.length === 0 ? 'No items yet.' : 'No matches.'}</div>
        ) : null}
        <div className="list-scroll">
          {filteredItems.map((it) => (
            <div key={it.id} className="item-row">
              <button className="item-main" onClick={() => selectForEdit(it)} type="button">
                <div className="item-top">
                  <input
                    className="done-box"
                    type="checkbox"
                    checked={it.done}
                    disabled={isHolidayItem(it)}
                    onClick={(e) => e.stopPropagation()}
                    onChange={(e) => {
                      if (isHolidayItem(it)) return
                      onToggleDone(it, e.target.checked)
                    }}
                    aria-label="Mark done"
                  />
                  {it.type === 'SCHOOL' && it.schoolKind === 'COMPULSORY' ? (
                    <span className={classNames('pill', `pill-${it.importance.toLowerCase()}`)}>{it.importance}</span>
                  ) : null}
                  <span className="pill pill-type">{it.type}</span>
                  {it.type === 'SCHOOL' && it.schoolKind ? <span className="pill">{it.schoolKind}</span> : null}
                  {it.amount != null ? <span className="pill">{it.amount}</span> : null}
                </div>
                <div className={classNames('item-title', it.done && 'item-title-done')}>{it.title}</div>
                <div className="item-sub">
                  {it.type === 'FIXED_COST' || it.type === 'MAIN_MEAL' || it.type === 'BIRTHDAY' ? (
                    <span>No time</span>
                  ) : it.type === 'SCHOOL' && it.schoolKind === 'COMPULSORY' ? (
                    <span>Deadline: {it.endTime ? toTimeInput(it.endTime) : '--:--'}</span>
                  ) : (
                    <>
                      <span>{it.startTime ? toTimeInput(it.startTime) : '--:--'}</span>
                      <span>→</span>
                      <span>{it.endTime ? toTimeInput(it.endTime) : '--:--'}</span>
                    </>
                  )}
                </div>
                {it.log ? <div className="item-log">{it.log}</div> : null}
              </button>
              {isHolidayItem(it) ? null : (
                <button className="icon-btn icon-btn-danger" onClick={() => onDelete(it.id)} type="button">
                  ✕
                </button>
              )}
            </div>
          ))}
        </div>
      </div>

      <div className="form">
        <div className="form-title">{editingId ? 'Edit item' : 'Add item'}</div>
        <div className="form-grid">
          {!typeLocked ? (
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
          ) : (
            <div className="field">
              <span>Type</span>
              <div className="pill">{type}</div>
            </div>
          )}

          {isCompulsory ? (
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
          ) : (
            <div className="field">
              <span>Importance</span>
              <div className="pill">MEDIUM</div>
            </div>
          )}

          {isSchool ? (
            <label className="field">
              <span>School</span>
              <select value={schoolKind} onChange={(e) => setSchoolKind(e.target.value as SchoolItemKind)}>
                <option value="LECTURE">Lecture</option>
                <option value="COMPULSORY">Compulsory</option>
              </select>
            </label>
          ) : null}

          {showTimes && !isCompulsory ? (
            <label className="field">
              <span>Start</span>
              <input
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                placeholder={supportsAllDay && allDay ? '' : 'HH:MM'}
                disabled={supportsAllDay && allDay}
              />
            </label>
          ) : null}

          {showTimes ? (
            <label className="field">
              <span>{isCompulsory ? 'Deadline' : 'End'}</span>
              <input
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
                placeholder={supportsAllDay && allDay ? '' : 'HH:MM'}
                disabled={supportsAllDay && allDay && !isCompulsory}
              />
            </label>
          ) : null}

          {supportsAllDay && showTimes && !isCompulsory ? (
            <label className="check-field">
              <span>All-day</span>
              <input
                className="check"
                type="checkbox"
                checked={allDay}
                onChange={(e) => {
                  const v = e.target.checked
                  setAllDay(v)
                  if (v) {
                    setStartTime('')
                    setEndTime('')
                  }
                }}
              />
            </label>
          ) : null}

          <label className="check-field">
            <span>Done</span>
            <input className="check" type="checkbox" checked={done} onChange={(e) => setDone(e.target.checked)} />
          </label>

          {type === 'FIXED_COST' ? (
            <label className="field">
              <span>Amount</span>
              <input value={amount} onChange={(e) => setAmount(e.target.value)} placeholder="0.00" inputMode="decimal" />
            </label>
          ) : null}

          {type === 'FIXED_COST' ? (
            <label className="field">
              <span>Frequency</span>
              <select
                value={fixedCostFrequency}
                onChange={(e) => setFixedCostFrequency(e.target.value as FixedCostFrequency)}
              >
                <option value="MONTHLY">Monthly</option>
                <option value="WEEKLY">Weekly</option>
                <option value="YEARLY">Yearly</option>
              </select>
            </label>
          ) : null}

          {isWorkout ? (
            <div className="field field-wide">
              <span>Workout details</span>

              <div className="workout-actions">
                <select
                  value={workoutTemplatePick}
                  onChange={(e) => applyTemplate(e.target.value)}
                  aria-label="Apply template"
                >
                  <option value="">Apply template…</option>
                  {workoutTemplates.map((t) => (
                    <option key={t.id} value={String(t.id)}>
                      {t.title}
                    </option>
                  ))}
                </select>
                <button className="btn btn-ghost" type="button" onClick={addWorkoutRow}>
                  + Add exercise
                </button>
              </div>

              {workoutEntries.length === 0 ? <div className="muted">No exercises added yet.</div> : null}
              <div className="workout-rows">
                {workoutEntries.map((row, idx) => (
                  <div key={idx} className="workout-row">
                    <select
                      value={row.exerciseId}
                      onChange={(e) => updateWorkoutRow(idx, { exerciseId: e.target.value })}
                      aria-label="Exercise"
                    >
                      <option value="">Exercise…</option>
                      {workoutExercises.map((ex) => (
                        <option key={ex.id} value={String(ex.id)}>
                          {ex.name}
                        </option>
                      ))}
                    </select>
                    <input
                      value={row.sets}
                      onChange={(e) => updateWorkoutRow(idx, { sets: e.target.value })}
                      placeholder="Sets"
                      inputMode="numeric"
                    />
                    <input
                      value={row.reps}
                      onChange={(e) => updateWorkoutRow(idx, { reps: e.target.value })}
                      placeholder="Reps"
                      inputMode="numeric"
                    />
                    <input
                      value={row.weight}
                      onChange={(e) => updateWorkoutRow(idx, { weight: e.target.value })}
                      placeholder="Weight"
                      inputMode="decimal"
                    />
                    <button className="icon-btn icon-btn-danger" type="button" onClick={() => removeWorkoutRow(idx)}>
                      ✕
                    </button>
                  </div>
                ))}
              </div>
            </div>
          ) : null}

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
