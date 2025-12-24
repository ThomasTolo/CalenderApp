export type CalendarItemType = 'SCHOOL' | 'WORKOUT' | 'MAIN_MEAL' | 'JOB' | 'FIXED_COST' | 'OTHER'
export type ImportanceLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type NotificationType = 'ITEM_CREATED' | 'ITEM_UPDATED' | 'ITEM_DELETED' | 'UPCOMING'

export type FixedCostFrequency = 'WEEKLY' | 'MONTHLY' | 'YEARLY'

export type SchoolItemKind = 'LECTURE' | 'COMPULSORY'

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
  done: boolean
  amount: number | null
  schoolKind: SchoolItemKind | null
  fixedCostFrequency: FixedCostFrequency | null
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
  done?: boolean
  amount?: number
  schoolKind?: SchoolItemKind
  fixedCostFrequency?: FixedCostFrequency
}

export type CalendarItemUpdateRequest = CalendarItemCreateRequest

export type ExerciseResponse = {
  id: number
  name: string
}

export type WorkoutEntryRequest = {
  exerciseId: number
  sets: number
  reps: number
  weight?: number
}

export type WorkoutEntryResponse = {
  exerciseId: number
  exerciseName: string
  sets: number
  reps: number
  weight: number | null
}

export type WorkoutTemplateCreateRequest = {
  title: string
  entries: WorkoutEntryRequest[]
}

export type WorkoutTemplateResponse = {
  id: number
  title: string
  entries: WorkoutEntryResponse[]
  createdAt: string
  updatedAt: string
}

export type WorkoutSessionUpdateRequest = {
  entries: WorkoutEntryRequest[]
}

export type WorkoutSessionResponse = {
  calendarItemId: number
  entries: WorkoutEntryResponse[]
  createdAt: string
  updatedAt: string
}
