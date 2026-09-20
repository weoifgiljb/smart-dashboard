import { describe, expect, it } from 'vitest'
import { sanitizeHtml } from '@/utils/sanitizeHtml'

describe('sanitizeHtml', () => {
  it('strips script tags and event handlers', () => {
    const dirty = '<p onclick="alert(1)">ok</p><script>alert(2)</script>'
    const clean = sanitizeHtml(dirty)
    expect(clean).toContain('<p>ok</p>')
    expect(clean.toLowerCase()).not.toContain('script')
    expect(clean.toLowerCase()).not.toContain('onclick')
  })

  it('drops javascript urls', () => {
    const dirty = '<a href="javascript:alert(1)">x</a>'
    const clean = sanitizeHtml(dirty)
    expect(clean.toLowerCase()).not.toContain('javascript:')
    expect(clean).toContain('<a>x</a>')
  })
})
