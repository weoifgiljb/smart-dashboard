export type Sm2State = {
  repetition: number // 已复习次数
  interval: number // 距离下次复习的天数
  easeFactor: number // 记忆难度因子
}

// 简化 SM-2：仅基于质量评分(0-5)调整 easeFactor 与 interval
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
export type SM2State = {
  repetitions: number
  easeFactor: number
  interval: number // days
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
  const current: SM2State = getState(id) || { repetitions: 0, easeFactor: 2.5, interval: 0 }
  // 简化打分：正确=4，错误/超时=2
  const grade = isCorrect ? 4 : 2
  if (grade >= 3) {
    if (current.repetitions === 0) {
      current.interval = 1
    } else if (current.repetitions === 1) {
      current.interval = 6
    } else {
      current.interval = Math.round(current.interval * current.easeFactor)
    }
    current.repetitions += 1
  } else {
    current.repetitions = 0
    current.interval = 1
  }
  current.easeFactor = Math.max(
    1.3,
    current.easeFactor + (0.1 - (5 - grade) * (0.08 + (5 - grade) * 0.02)),
  )
  setState(id, current)
  return current
}
