export type DayActivity = {
  checkin?: number
  pomodoro?: number
  word?: number
  task?: number
}

export type HeatCell = {
  key: string
  date: string | null
  heat: number
  level: 0 | 1 | 2 | 3 | 4
}

export function dayHeat(day: DayActivity | undefined): number {
  if (!day) return 0
  return (
    (Number(day.checkin) || 0) * 1 +
    (Number(day.pomodoro) || 0) * 2 +
    (Number(day.word) || 0) * 1 +
    (Number(day.task) || 0) * 3
  )
}

export function heatLevel(heat: number, maxHeat: number): HeatCell['level'] {
  if (heat <= 0) return 0
  const cap = Math.max(maxHeat, 1)
  const ratio = heat / cap
  if (ratio <= 0.25) return 1
  if (ratio <= 0.5) return 2
  if (ratio <= 0.75) return 3
  return 4
}

function pad2(n: number) {
  return String(n).padStart(2, '0')
}

export function toDateKey(d: Date): string {
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
}

function weekdayIndex(date: Date, weekStartsOn: number): number {
  const day = date.getDay()
  if (weekStartsOn === 1) {
    return (day + 6) % 7
  }
  return day
}

export function buildYearHeatmap(
  year: number,
  activity: Record<string, DayActivity>,
  weekStartsOn: number,
): { weeks: HeatCell[][]; monthLabels: string[]; activeDays: number; maxHeat: number } {
  const heatByDate: Record<string, number> = {}
  let maxHeat = 1
  let activeDays = 0
  for (const [key, day] of Object.entries(activity)) {
    const heat = dayHeat(day)
    heatByDate[key] = heat
    if (heat > 0) {
      activeDays += 1
      maxHeat = Math.max(maxHeat, heat)
    }
  }

  const jan1 = new Date(year, 0, 1)
  const dec31 = new Date(year, 11, 31)
  const cells: HeatCell[] = []
  const leading = weekdayIndex(jan1, weekStartsOn)
  for (let i = 0; i < leading; i++) {
    cells.push({ key: `pad-${i}`, date: null, heat: 0, level: 0 })
  }
  for (let cursor = new Date(jan1); cursor <= dec31; cursor.setDate(cursor.getDate() + 1)) {
    const key = toDateKey(cursor)
    const heat = heatByDate[key] || 0
    cells.push({
      key,
      date: key,
      heat,
      level: heatLevel(heat, maxHeat),
    })
  }
  while (cells.length % 7 !== 0) {
    cells.push({ key: `trail-${cells.length}`, date: null, heat: 0, level: 0 })
  }

  const weeks: HeatCell[][] = []
  for (let i = 0; i < cells.length; i += 7) {
    weeks.push(cells.slice(i, i + 7))
  }

  const monthLabels = weeks.map((week) => {
    const start = week.find((cell) => cell.date && Number(cell.date.slice(8, 10)) === 1)
    return start?.date ? `${Number(start.date.slice(5, 7))}月` : ''
  })

  return { weeks, monthLabels, activeDays, maxHeat }
}
