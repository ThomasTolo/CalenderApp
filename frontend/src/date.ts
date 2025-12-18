export function pad2(n: number): string {
  return String(n).padStart(2, '0')
}

export function toIsoDate(d: Date): string {
  const y = d.getFullYear()
  const m = pad2(d.getMonth() + 1)
  const day = pad2(d.getDate())
  return `${y}-${m}-${day}`
}

export function fromIsoDate(iso: string): Date {
  const [y, m, d] = iso.split('-').map((x) => Number(x))
  return new Date(y, (m ?? 1) - 1, d ?? 1)
}

export function monthLabel(d: Date): string {
  return d.toLocaleString(undefined, { month: 'long', year: 'numeric' })
}

export function startOfMonth(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth(), 1)
}

export function endOfMonth(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth() + 1, 0)
}

// Monday-first index: 0=Mon ... 6=Sun
export function weekdayIndexMondayFirst(d: Date): number {
  const js = d.getDay() // 0=Sun..6=Sat
  return (js + 6) % 7
}

export function addMonths(d: Date, delta: number): Date {
  return new Date(d.getFullYear(), d.getMonth() + delta, 1)
}
