import { describe, expect, it } from 'vitest'
import { unwrapList } from '@/api/hooks/unwrap'

describe('task list unwrap', () => {
  it('accepts raw arrays', () => {
    expect(unwrapList([1, 2])).toEqual([1, 2])
  })

  it('accepts wrapped data', () => {
    expect(unwrapList({ data: [{ id: '1' }] })).toEqual([{ id: '1' }])
  })

  it('falls back to empty list', () => {
    expect(unwrapList(null)).toEqual([])
  })
})
