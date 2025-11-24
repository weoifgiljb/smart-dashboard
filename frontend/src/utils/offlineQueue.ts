import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'

type QueuedItem = {
  config: AxiosRequestConfig
  timestamp: number
}

const STORAGE_KEY = 'offlineWriteQueue'
const WRITE_METHODS = new Set(['post', 'put', 'patch', 'delete'])

function loadQueue(): QueuedItem[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return []
    const list: QueuedItem[] = JSON.parse(raw)
    return Array.isArray(list) ? list : []
  } catch {
    return []
  }
}

function saveQueue(list: QueuedItem[]) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(list))
  } catch {
    // ignore quota errors
  }
}

export function enqueue(config: AxiosRequestConfig) {
  const list = loadQueue()
  list.push({ config, timestamp: Date.now() })
  saveQueue(list)
}

async function replayQueue(client: AxiosInstance) {
  const list = loadQueue()
  if (list.length === 0) return
  const remain: QueuedItem[] = []
  for (const item of list) {
    try {
      await client(item.config as AxiosRequestConfig)
    } catch {
      // 保留失败项，稍后重放
      remain.push(item)
    }
  }
  saveQueue(remain)
}

export function initOfflineQueue(client: AxiosInstance) {
  // 离线时写操作入队
  client.interceptors.request.use((config) => {
    const method = String(config.method || 'get').toLowerCase()
    if (WRITE_METHODS.has(method) && typeof navigator !== 'undefined' && !navigator.onLine) {
      enqueue(config)
      // 伪造响应，提示已入队
      const fake: AxiosResponse = {
        data: { queued: true },
        status: 202,
        statusText: 'Accepted',
        headers: {},
        config,
      }
      return Promise.reject({ isEnqueued: true, response: fake })
    }
    return config
  })

  // 网络恢复后自动重放
  window.addEventListener('online', () => {
    replayQueue(client)
  })
  // 应用启动时尝试重放
  replayQueue(client)
}
