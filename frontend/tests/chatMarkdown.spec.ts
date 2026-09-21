import { describe, expect, it } from 'vitest'
import { renderChatMarkdown } from '@/utils/chatMarkdown'

describe('renderChatMarkdown', () => {
  it('sanitizes markup after rendering markdown', () => {
    const html = renderChatMarkdown('<script>alert(1)</script> **ok**')
    expect(html.toLowerCase()).not.toContain('<script')
    expect(html).toContain('<strong>ok</strong>')
  })
})
