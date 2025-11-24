let timer: any = null
let remaining = 0

self.onmessage = (e: MessageEvent) => {
  const { type, payload } = e.data || {}
  if (type === 'start') {
    if (timer) clearInterval(timer)
    remaining = Number(payload?.seconds) || 0
    timer = setInterval(() => {
      remaining -= 1
      if (remaining > 0) {
        ;(self as any).postMessage({ type: 'tick', seconds: remaining })
      } else {
        clearInterval(timer)
        timer = null
        remaining = 0
        ;(self as any).postMessage({ type: 'done' })
      }
    }, 1000)
  } else if (type === 'pause') {
    if (timer) clearInterval(timer)
    timer = null
    ;(self as any).postMessage({ type: 'tick', seconds: remaining })
  } else if (type === 'reset') {
    if (timer) clearInterval(timer)
    timer = null
    remaining = Number(payload?.seconds) || 0
    ;(self as any).postMessage({ type: 'tick', seconds: remaining })
  }
}
