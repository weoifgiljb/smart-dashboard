import axios, { AxiosInstance, AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { enqueue } from '@/utils/offlineQueue'

const apiBase = (import.meta as any).env?.VITE_API_BASE || '/api'

const request: AxiosInstance = axios.create({
  baseURL: apiBase,
  timeout: 10000,
  withCredentials: true,
})

const MAX_RETRIES = 2
const RETRY_METHODS = new Set(['get', 'head', 'options'])
let isRefreshing = false
let refreshPromise: Promise<string | null> | null = null

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function refreshAuthToken(): Promise<string | null> {
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) return null
  try {
    const res = await axios.post(`${apiBase}/auth/refresh`, null, {
      headers: { 'Refresh-Token': refreshToken },
    })
    const newToken = (res.data?.token || res.data?.accessToken) as string | undefined
    if (newToken) {
      localStorage.setItem('token', newToken)
      return newToken
    }
    return null
  } catch {
    return null
  }
}

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    config.headers['Content-Type'] = 'application/json'
    config.headers['Accept'] = 'application/json'
    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

request.interceptors.response.use(
  (response): any => {
    return response.data
  },
  async (error: AxiosError) => {
    const config: any = error.config || {}
    const status = error.response?.status

    // 1) 未授权：尝试刷新一次 Token，失败则跳登录
    if (status === 401) {
      if (!isRefreshing) {
        isRefreshing = true
        refreshPromise = refreshAuthToken().finally(() => {
          isRefreshing = false
        })
      }
      const newToken = await (refreshPromise as Promise<string | null>)
      if (newToken) {
        config.headers = config.headers || {}
        config.headers.Authorization = `Bearer ${newToken}`
        return request(config)
      }
      localStorage.removeItem('token')
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
      return Promise.reject(error)
    }

    // 2) 网络/超时或 5xx/429：对 GET 等幂等方法进行指数退避重试
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

    // 2.5) 离线：将写操作加入离线队列
    const method = String(config.method || 'get').toLowerCase()
    const isWrite =
      method === 'post' || method === 'put' || method === 'patch' || method === 'delete'
    if (!error.response && isWrite && typeof navigator !== 'undefined' && !navigator.onLine) {
      try {
        enqueue({
          url: config.url || '',
          method,
          data: config.data,
          headers: config.headers as any,
        })
        ElMessage.info('当前离线，操作已加入队列，将在恢复网络后自动重试')
      } catch {
        // ignore
      }
    }

    // 3) 统一错误提示
    if (error.response) {
      const msg = (error.response.data as any)?.message || `请求失败（${error.response.status}）`
      ElMessage.error(msg)
    } else {
      ElMessage.error('网络错误，请稍后重试')
    }
    return Promise.reject(error)
  },
)

export default request
