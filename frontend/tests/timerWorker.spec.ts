import { describe, expect, it } from 'vitest'
import { remainingFromDeadline } from '@/workers/timerWorker'

describe('remainingFromDeadline', () => {
  it('uses wall clock so throttled ticks still catch up', () => {
    const deadline = 1_000_000
    expect(remainingFromDeadline(deadline, 1_000_000 - 25_000)).toBe(25)
    expect(remainingFromDeadline(deadline, 1_000_000 - 1)).toBe(1)
    expect(remainingFromDeadline(deadline, 1_000_000 + 5_000)).toBe(0)
  })
})
