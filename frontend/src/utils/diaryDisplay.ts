import { marked } from 'marked'
import { sanitizeHtml } from '@/utils/sanitizeHtml'

export enum DiaryMood {
  Happy = 'happy',
  Neutral = 'neutral',
  Sad = 'sad',
  Energetic = 'energetic',
  Tired = 'tired',
}

export function parseDiaryMood(value: string | undefined): DiaryMood {
  switch (value) {
    case DiaryMood.Happy:
      return DiaryMood.Happy
    case DiaryMood.Neutral:
      return DiaryMood.Neutral
    case DiaryMood.Sad:
      return DiaryMood.Sad
    case DiaryMood.Energetic:
      return DiaryMood.Energetic
    case DiaryMood.Tired:
      return DiaryMood.Tired
    default:
      return DiaryMood.Neutral
  }
}

export function moodLabel(mood: DiaryMood): string {
  switch (mood) {
    case DiaryMood.Happy:
      return '开心'
    case DiaryMood.Neutral:
      return '平淡'
    case DiaryMood.Sad:
      return '难过'
    case DiaryMood.Energetic:
      return '充满活力'
    case DiaryMood.Tired:
      return '疲惫'
    default: {
      const exhaustive: never = mood
      return exhaustive
    }
  }
}

export function moodTagType(mood: DiaryMood): 'success' | 'info' | 'warning' | 'danger' {
  switch (mood) {
    case DiaryMood.Happy:
      return 'success'
    case DiaryMood.Neutral:
      return 'info'
    case DiaryMood.Sad:
      return 'info'
    case DiaryMood.Energetic:
      return 'warning'
    case DiaryMood.Tired:
      return 'danger'
    default: {
      const exhaustive: never = mood
      return exhaustive
    }
  }
}

export function moodColor(mood: DiaryMood): string {
  switch (mood) {
    case DiaryMood.Happy:
      return 'var(--color-success)'
    case DiaryMood.Neutral:
      return 'var(--color-text-muted)'
    case DiaryMood.Sad:
      return 'var(--color-text-secondary)'
    case DiaryMood.Energetic:
      return 'var(--color-warning)'
    case DiaryMood.Tired:
      return 'var(--color-danger)'
    default: {
      const exhaustive: never = mood
      return exhaustive
    }
  }
}

export function renderDiaryHtml(content: string) {
  if (!content) return ''
  try {
    return sanitizeHtml(marked.parse(content, { async: false }))
  } catch {
    return sanitizeHtml(content)
  }
}

export function moodEmoji(mood: DiaryMood): string {
  switch (mood) {
    case DiaryMood.Happy:
      return '😄'
    case DiaryMood.Neutral:
      return '😐'
    case DiaryMood.Sad:
      return '😭'
    case DiaryMood.Energetic:
      return '💪'
    case DiaryMood.Tired:
      return '😫'
    default: {
      const exhaustive: never = mood
      return exhaustive
    }
  }
}

function pad(n: number) {
  return String(n).padStart(2, '0')
}

export function todayDateKey(now = new Date()) {
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}

export function monthKey(now = new Date()) {
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}`
}

export function monthKeyFromDate(dateKey: string) {
  return dateKey.slice(0, 7)
}

export function shiftMonth(key: string, delta: number) {
  const [year, month] = key.split('-').map(Number)
  return monthKey(new Date(year, (month || 1) - 1 + delta, 1))
}

export function inMonth(dateKey: string, month: string) {
  return dateKey.startsWith(month)
}

export const DIARY_PAGE_SIZE = 5

export function clampPage(page: number, total: number, size: number) {
  if (total <= 0) return 1
  const maxPage = Math.ceil(total / Math.max(1, size))
  return Math.min(Math.max(1, page), maxPage)
}

export function paginateItems<T>(items: T[], page: number, size: number) {
  const safeSize = Math.max(1, size)
  const safePage = clampPage(page, items.length, safeSize)
  const start = (safePage - 1) * safeSize
  return items.slice(start, start + safeSize)
}

export function formatUpdatedAt(iso: string | undefined, diaryDate: string) {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  const key = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  if (key === diaryDate) {
    return `更新于 ${pad(d.getHours())}:${pad(d.getMinutes())}`
  }
  return `更新于 ${d.toLocaleString('zh-CN')}`
}
