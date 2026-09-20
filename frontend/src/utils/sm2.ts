export type Sm2State = {
  repetition: number
  interval: number
  easeFactor: number
}

export type SM2State = {
  repetitions: number
  easeFactor: number
  interval: number
}

export function sm2Next(state: Sm2State, quality: 0 | 1 | 2 | 3 | 4 | 5): Sm2State {
  let { repetition, interval, easeFactor } = state
  easeFactor = Math.max(1.3, easeFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)))
  if (quality < 3) {
    repetition = 0
    interval = 1
  } else {
    repetition += 1
    if (repetition === 1) interval = 1
    else if (repetition === 2) interval = 6
    else interval = Math.round(interval * easeFactor)
  }
  return { repetition, interval, easeFactor }
}

export function initialSm2(): Sm2State {
  return { repetition: 0, interval: 1, easeFactor: 2.5 }
}

const STORAGE_KEY = 'sm2State'

function loadAll(): Record<string, SM2State> {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

function saveAll(map: Record<string, SM2State>) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(map))
}

export function getState(id: string): SM2State | null {
  const map = loadAll()
  return map[id] || null
}

export function setState(id: string, state: SM2State) {
  const map = loadAll()
  map[id] = state
  saveAll(map)
}

export function applyReview(id: string, isCorrect: boolean): SM2State {
  const current = getState(id) || { repetitions: 0, easeFactor: 2.5, interval: 0 }
  const next = sm2Next(
    {
      repetition: current.repetitions,
      interval: Math.max(current.interval, 1),
      easeFactor: current.easeFactor,
    },
    isCorrect ? 4 : 2,
  )
  const state: SM2State = {
    repetitions: next.repetition,
    easeFactor: next.easeFactor,
    interval: next.interval,
  }
  setState(id, state)
  return state
}
