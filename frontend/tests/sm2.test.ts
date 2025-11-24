import { describe, it, expect } from 'vitest'
import { initialSm2, sm2Next } from '@/utils/sm2'

describe('SM-2 simplified', () => {
  it('boosts interval with good quality', () => {
    const s1 = initialSm2()
    const s2 = sm2Next(s1, 4)
    expect(s2.repetition).toBe(1)
    expect(s2.interval).toBeGreaterThanOrEqual(1)
    const s3 = sm2Next(s2, 4)
    expect(s3.repetition).toBe(2)
    expect(s3.interval).toBe(6)
    const s4 = sm2Next(s3, 5)
    expect(s4.interval).toBeGreaterThan(6)
  })
})
