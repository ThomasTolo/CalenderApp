export type CalendarItemType = 'SCHOOL' | 'WORKOUT' | 'MAIN_MEAL' | 'JOB'
export type ImportanceLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type NotificationType = 'ITEM_CREATED' | 'ITEM_UPDATED' | 'ITEM_DELETED' | 'UPCOMING'

export type AuthResponse = { token: string }

export type CalendarItemResponse = {
  id: number
  date: string // YYYY-MM-DD
  startTime: string | null // HH:mm:ss or HH:mm
  endTime: string | null
  type: CalendarItemType
  importance: ImportanceLevel
  title: string
  log: string | null
  createdAt: string
  updatedAt: string
}

export type CalendarMonthResponse = {
  year: number
  month: number
  items: CalendarItemResponse[]
}

export type NotificationResponse = {
  id: number
  type: NotificationType
  importance: ImportanceLevel
  message: string
  calendarItemId: number | null
  read: boolean
  createdAt: string
}

export type CalendarItemCreateRequest = {
  date: string
  startTime?: string
  endTime?: string
  type: CalendarItemType
  importance: ImportanceLevel
  title: string
  log?: string
}

export type CalendarItemUpdateRequest = CalendarItemCreateRequest
