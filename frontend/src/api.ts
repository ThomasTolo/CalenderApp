import type {
  AuthResponse,
  CalendarItemCreateRequest,
  CalendarItemResponse,
  CalendarItemUpdateRequest,
  CalendarMonthResponse,
  NotificationResponse,
} from './types'

const TOKEN_KEY = 'calenderapp.jwt'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string | null) {
  if (!token) localStorage.removeItem(TOKEN_KEY)
  else localStorage.setItem(TOKEN_KEY, token)
}

function apiBase(): string {
  const fromEnv = import.meta.env.VITE_API_BASE_URL as string | undefined
  return fromEnv?.trim() ? fromEnv.trim().replace(/\/$/, '') : ''
}

class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

async function request<T>(
  path: string,
  options: RequestInit & { auth?: boolean } = {},
): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('Content-Type', 'application/json')

  if (options.auth) {
    const token = getToken()
    if (token) headers.set('Authorization', `Bearer ${token}`)
  }

  const res = await fetch(`${apiBase()}${path}`, {
    ...options,
    headers,
  })

  if (!res.ok) {
    const raw = await res.text().catch(() => '')
    const message = (() => {
      const t = raw?.trim()
      if (!t) return res.statusText
      if (t.startsWith('{') || t.startsWith('[')) {
        try {
          const parsed = JSON.parse(t) as unknown
          if (parsed && typeof parsed === 'object' && 'message' in parsed) {
            const m = (parsed as { message?: unknown }).message
            if (typeof m === 'string' && m.trim()) return m.trim()
          }
        } catch {
          // ignore
        }
      }
      return t
    })()

    throw new ApiError(res.status, message || res.statusText)
  }

  // 204
  if (res.status === 204) return undefined as T

  return (await res.json()) as T
}

export const api = {
  async register(username: string, password: string): Promise<AuthResponse> {
    return request<AuthResponse>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    })
  },

  async login(username: string, password: string): Promise<AuthResponse> {
    return request<AuthResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    })
  },

  async getMonth(year: number, month: number): Promise<CalendarMonthResponse> {
    const qs = new URLSearchParams({ year: String(year), month: String(month) })
    return request<CalendarMonthResponse>(`/api/calendar/month?${qs.toString()}`, {
      method: 'GET',
      auth: true,
    })
  },

  async getDay(date: string): Promise<CalendarItemResponse[]> {
    const qs = new URLSearchParams({ date })
    return request<CalendarItemResponse[]>(`/api/calendar/day?${qs.toString()}`, {
      method: 'GET',
      auth: true,
    })
  },

  async createItem(payload: CalendarItemCreateRequest): Promise<CalendarItemResponse> {
    return request<CalendarItemResponse>('/api/calendar/items', {
      method: 'POST',
      auth: true,
      body: JSON.stringify(payload),
    })
  },

  async updateItem(id: number, payload: CalendarItemUpdateRequest): Promise<CalendarItemResponse> {
    return request<CalendarItemResponse>(`/api/calendar/items/${id}`, {
      method: 'PUT',
      auth: true,
      body: JSON.stringify(payload),
    })
  },

  async deleteItem(id: number): Promise<void> {
    return request<void>(`/api/calendar/items/${id}`, {
      method: 'DELETE',
      auth: true,
    })
  },

  async listUnreadNotifications(): Promise<NotificationResponse[]> {
    return request<NotificationResponse[]>('/api/notifications/unread', {
      method: 'GET',
      auth: true,
    })
  },

  async listAllNotifications(): Promise<NotificationResponse[]> {
    return request<NotificationResponse[]>('/api/notifications', {
      method: 'GET',
      auth: true,
    })
  },

  async markNotificationRead(id: number): Promise<NotificationResponse> {
    return request<NotificationResponse>(`/api/notifications/${id}/read`, {
      method: 'POST',
      auth: true,
    })
  },
}

export function isApiError(err: unknown): err is ApiError {
  return err instanceof ApiError
}
