const FORBIDDEN_TAGS = new Set([
  'SCRIPT',
  'IFRAME',
  'OBJECT',
  'EMBED',
  'LINK',
  'META',
  'BASE',
  'FORM',
  'SVG',
  'MATH',
])

const URI_ATTRS = new Set(['href', 'src', 'xlink:href', 'poster'])

function isDangerousUrl(value: string): boolean {
  const trimmed = value.trim()
  const lower = trimmed.toLowerCase()
  return (
    lower.startsWith('javascript:') ||
    lower.startsWith('vbscript:') ||
    lower.startsWith('data:text/html')
  )
}

function sanitizeNode(node: Node): void {
  const children = Array.from(node.childNodes)
  for (const child of children) {
    if (child.nodeType === Node.ELEMENT_NODE) {
      const el = child as Element
      if (FORBIDDEN_TAGS.has(el.tagName)) {
        el.remove()
        continue
      }
      for (const attr of Array.from(el.attributes)) {
        const name = attr.name.toLowerCase()
        if (name.startsWith('on') || name === 'style') {
          el.removeAttribute(attr.name)
          continue
        }
        if (URI_ATTRS.has(name) && isDangerousUrl(attr.value)) {
          el.removeAttribute(attr.name)
        }
      }
      sanitizeNode(el)
    } else if (child.nodeType === Node.COMMENT_NODE) {
      child.parentNode?.removeChild(child)
    }
  }
}

export function sanitizeHtml(html: string): string {
  if (!html) return ''
  const doc = new DOMParser().parseFromString(html, 'text/html')
  sanitizeNode(doc.body)
  return doc.body.innerHTML
}
