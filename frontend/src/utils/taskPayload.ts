import type { Task } from '@/types/task'

function asDayStart(value?: string) {
  if (!value) return undefined
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) return `${value}T00:00:00`
  return value
}

export function buildTaskPayload(form: Task): Partial<Task> {
  const payload: Partial<Task> = { ...form }
  const startDate = asDayStart(payload.startDate)
  const dueDate = asDayStart(payload.dueDate)
  if (!startDate) delete payload.startDate
  else payload.startDate = startDate
  if (!dueDate) delete payload.dueDate
  else payload.dueDate = dueDate
  if (!payload.remindAt) delete payload.remindAt
  if (payload.actualMinutes == null) delete payload.actualMinutes
  return payload
}

export function minutesProgress(task: Pick<Task, 'actualMinutes' | 'estimateMinutes'>) {
  const actual = Number(task.actualMinutes) || 0
  const estimate = Number(task.estimateMinutes) || 0
  if (!actual && !estimate) return ''
  return `${actual} / ${estimate} 分钟`
}
