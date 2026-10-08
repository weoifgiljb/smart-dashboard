import { describe, expect, it } from 'vitest'
import { disablePastDates } from '@/utils/dateLimits'

function atMidnight(offsetDays: number) {
  const d = new Date()
  d.setHours(0, 0, 0, 0)
  d.setDate(d.getDate() + offsetDays)
  return d
}

describe('disablePastDates（Issue #3 Bug 6）', () => {
  it('禁用昨天', () => {
    expect(disablePastDates(atMidnight(-1))).toBe(true)
  })

  it('禁用很久以前', () => {
    expect(disablePastDates(atMidnight(-365))).toBe(true)
  })

  it('不禁用今天', () => {
    expect(disablePastDates(atMidnight(0))).toBe(false)
  })

  it('不禁用明天与之后', () => {
    expect(disablePastDates(atMidnight(1))).toBe(false)
    expect(disablePastDates(atMidnight(30))).toBe(false)
  })

  it('今天的任意时刻都不算过去（只比较日期部分）', () => {
    const todayNoon = atMidnight(0)
    todayNoon.setHours(12, 30, 0, 0)
    expect(disablePastDates(todayNoon)).toBe(false)
  })
})
