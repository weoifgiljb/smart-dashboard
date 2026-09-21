import { describe, expect, it } from 'vitest'
import { buildYearHeatmap, dayHeat, heatLevel } from '@/utils/yearHeatmap'

describe('yearHeatmap', () => {
  it('uses published heat weights', () => {
    expect(dayHeat({ checkin: 1, pomodoro: 2, word: 1, task: 1 })).toBe(1 + 4 + 1 + 3)
    expect(dayHeat(undefined)).toBe(0)
  })

  it('maps heat into four visible levels', () => {
    expect(heatLevel(0, 12)).toBe(0)
    expect(heatLevel(2, 12)).toBe(1)
    expect(heatLevel(6, 12)).toBe(2)
    expect(heatLevel(9, 12)).toBe(3)
    expect(heatLevel(12, 12)).toBe(4)
  })

  it('pads 2026 to Monday and labels January on the first week', () => {
    const built = buildYearHeatmap(2026, { '2026-01-01': { checkin: 1 } }, 1)
    expect(built.weeks[0][0].date).toBeNull()
    expect(built.weeks[0][3].date).toBe('2026-01-01')
    expect(built.weeks[0][3].heat).toBe(1)
    expect(built.monthLabels[0]).toBe('1月')
    expect(built.activeDays).toBe(1)
  })
})
