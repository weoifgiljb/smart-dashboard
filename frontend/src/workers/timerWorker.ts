export function remainingFromDeadline(deadlineMs: number, nowMs: number): number {
  return Math.max(0, Math.ceil((deadlineMs - nowMs) / 1000))
}

let timer: ReturnType<typeof setInterval> | null = null
let remaining = 0
let deadline = 0

function clearTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function post(message: { type: string; seconds?: number }) {
  ;(self as unknown as { postMessage: (data: unknown) => void }).postMessage(message)
}

function tick(now = Date.now()) {
  remaining = remainingFromDeadline(deadline, now)
  if (remaining > 0) {
    post({ type: 'tick', seconds: remaining })
    return
  }
  clearTimer()
  remaining = 0
  post({ type: 'done' })
}

self.onmessage = (e: MessageEvent) => {
  const { type, payload } = e.data || {}
  if (type === 'start') {
    clearTimer()
    remaining = Number(payload?.seconds) || 0
    deadline = Date.now() + remaining * 1000
    timer = setInterval(() => tick(), 1000)
  } else if (type === 'pause') {
    clearTimer()
    remaining = remainingFromDeadline(deadline, Date.now())
    post({ type: 'tick', seconds: remaining })
  } else if (type === 'reset') {
    clearTimer()
    remaining = Number(payload?.seconds) || 0
    deadline = Date.now() + remaining * 1000
    post({ type: 'tick', seconds: remaining })
  }
}
