import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { enqueue } from '@/utils/offlineQueue'
import { refreshAuthToken } from '@/api/authRefresh'
import { clearAuthTokens, getAccessToken } from '@/api/authTokens'

const apiBase = (import.meta as { env?: { VITE_API_BASE?: string } }).env?.VITE_API_BASE || '/api'

const request: AxiosInstance = axios.create({
  baseURL: apiBase,
  timeout: 10000,
  withCredentials: true,
})

const MAX_RETRIES = 2
const RETRY_METHODS = new Set(['get', 'head', 'options'])
let isRefreshing = false
let refreshPromise: Promise<string | null> | null = null

interface RetryConfig extends InternalAxiosRequestConfig {
  __retryCount?: number
  __retriedAfterRefresh?: boolean
}

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function readErrorMessage(error: AxiosError, fallback: string) {
  const data = error.response?.data
  if (data instanceof Blob) {
    try {
      const parsed: unknown = JSON.parse(await data.text())
      if (parsed && typeof parsed === 'object' && 'message' in parsed) {
        const message = (parsed as { message?: unknown }).message
        if (typeof message === 'string' && message) return message
      }
    } catch {
      return fallback
    }
  }
  if (data && typeof data === 'object' && 'message' in data) {
    const message = (data as { message?: unknown }).message
    if (typeof message === 'string' && message) return message
  }
  return fallback
}

function requestUrl(config: InternalAxiosRequestConfig) {
  return String(config.url || '')
}

function isAuthRefreshRequest(config: InternalAxiosRequestConfig) {
  return requestUrl(config).includes('/auth/refresh')
}

function isSilentAuthProbe(config: InternalAxiosRequestConfig) {
  const url = requestUrl(config)
  return url.includes('/auth/me') || url.includes('/auth/logout')
}

function onPublicAuthPage() {
  const path = router.currentRoute.value.path
  return path === '/login' || path === '/register'
}

function expireSession(config: InternalAxiosRequestConfig) {
  clearAuthTokens()
  if (!isSilentAuthProbe(config) && !onPublicAuthPage()) {
    router.push('/login')
    ElMessage.error('登录已过期，请重新登录')
  }
}

request.interceptors.request.use(
  (config) => {
    const token = getAccessToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    if (config.responseType !== 'blob') {
      config.headers['Accept'] = 'application/json'
    }
    config.headers['Content-Type'] = 'application/json'
    return config
  },
  (error) => Promise.reject(error),
)

request.interceptors.response.use(
  (response) => response.data,
  async (error: AxiosError) => {
    const config = (error.config || {}) as RetryConfig
    const status = error.response?.status

    if (status === 401) {
      if (isAuthRefreshRequest(config) || config.__retriedAfterRefresh) {
        expireSession(config)
        return Promise.reject(error)
      }
      if (!isRefreshing) {
        isRefreshing = true
        refreshPromise = refreshAuthToken()
          .then((result) => result?.token ?? null)
          .finally(() => {
            isRefreshing = false
          })
      }
      const newToken = await (refreshPromise as Promise<string | null>)
      if (newToken) {
        config.__retriedAfterRefresh = true
        config.headers = config.headers || {}
        config.headers.Authorization = `Bearer ${newToken}`
        return request(config)
      }
      expireSession(config)
      return Promise.reject(error)
    }

    const shouldRetry =
      (!error.response || status === 429 || (status && status >= 500)) &&
      RETRY_METHODS.has(String(config.method || 'get').toLowerCase())
    config.__retryCount = config.__retryCount || 0
    if (shouldRetry && config.__retryCount < MAX_RETRIES) {
      config.__retryCount += 1
      const backoff = 300 * Math.pow(2, config.__retryCount - 1) + Math.random() * 100
      await sleep(backoff)
      return request(config)
    }

    const method = String(config.method || 'get').toLowerCase()
    const isWrite =
      method === 'post' || method === 'put' || method === 'patch' || method === 'delete'
    if (!error.response && isWrite && typeof navigator !== 'undefined' && !navigator.onLine) {
      try {
        enqueue({
          url: config.url || '',
          method,
          data: config.data,
          headers: config.headers as Record<string, string>,
        })
        ElMessage.info('当前离线，操作已加入队列，将在恢复网络后自动重试')
      } catch {
        // ignore
      }
    }

    if (error.response) {
      const msg = await readErrorMessage(error, `请求失败（${error.response.status}）`)
      ElMessage.error(msg)
    } else {
      ElMessage.error('网络错误，请稍后重试')
    }
    return Promise.reject(error)
  },
)

export default request
