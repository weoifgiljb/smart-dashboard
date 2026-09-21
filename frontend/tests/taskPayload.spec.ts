import { describe, expect, it } from 'vitest'
import { buildTaskPayload, minutesProgress } from '@/utils/taskPayload'
import type { Task } from '@/types/task'

describe('task payload', () => {
  it('keeps startDate and dueDate for the gantt range', () => {
    const payload = buildTaskPayload({
      title: '写计划',
      startDate: '2026-09-21',
      dueDate: '2026-09-22',
      estimateMinutes: 50,
      actualMinutes: 25,
    })
    expect(payload.startDate).toBe('2026-09-21T00:00:00')
    expect(payload.dueDate).toBe('2026-09-22T00:00:00')
    expect(payload.estimateMinutes).toBe(50)
    expect(payload.actualMinutes).toBe(25)
  })

  it('omits blank dates so gantt can stay empty on purpose', () => {
    const payload = buildTaskPayload({ title: '临时想法', startDate: '', dueDate: '' })
    expect(payload.startDate).toBeUndefined()
    expect(payload.dueDate).toBeUndefined()
  })

  it('shows pomodoro actual minutes against the estimate', () => {
    const task: Task = { title: '专注', estimateMinutes: 50, actualMinutes: 25 }
    expect(minutesProgress(task)).toBe('25 / 50 分钟')
  })
})
