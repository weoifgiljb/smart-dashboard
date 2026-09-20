import { describe, expect, it } from 'vitest'
import { safeRedirect } from '@/utils/safeRedirect'

describe('safeRedirect', () => {
  it('keeps in-app paths and rejects open redirects', () => {
    expect(safeRedirect('/calendar')).toBe('/calendar')
    expect(safeRedirect('/diary?x=1')).toBe('/diary?x=1')
    expect(safeRedirect('https://evil.example')).toBe('/')
    expect(safeRedirect('//evil.example')).toBe('/')
    expect(safeRedirect(undefined)).toBe('/')
  })
})
