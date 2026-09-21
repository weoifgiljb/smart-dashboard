import { sanitizeHtml } from './sanitizeHtml'

export function renderChatMarkdown(text: string) {
  if (!text) return ''
  let html = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  html = html.replace(
    /```([\s\S]*?)```/g,
    (_, code: string) => `<pre><code>${code.trim()}</code></pre>`,
  )
  html = html.replace(/`([^`\n]+?)`/g, '<code>$1</code>')
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/\n/g, '<br/>')
  return sanitizeHtml(html)
}
