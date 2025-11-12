import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import App from './App.vue'
import router from './router'
import './styles/tokens.css'
import { VueQueryPlugin, QueryClient, type VueQueryPluginOptions } from '@tanstack/vue-query'
import { initOfflineQueue } from './utils/offlineQueue'
import request from './api/request'
import * as Sentry from '@sentry/vue'

const app = createApp(App)
const pinia = createPinia()

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn })
// vue-query
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 2,
      staleTime: 10 * 1000,
      gcTime: 5 * 60 * 1000,
      refetchOnWindowFocus: false
    }
  }
})
app.use(VueQueryPlugin, { queryClient } as VueQueryPluginOptions)

// 应用主题：localStorage > 系统偏好
const applyTheme = (theme: 'light' | 'dark') => {
  document.documentElement.setAttribute('data-theme', theme)
  localStorage.setItem('theme', theme)
}
const stored = (localStorage.getItem('theme') as 'light' | 'dark' | null)
if (stored) {
  applyTheme(stored)
} else {
  const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches
  applyTheme(prefersDark ? 'dark' : 'light')
}

app.mount('#app')

// 初始化离线队列重放
initOfflineQueue(request)

// Sentry（可选，根据 DSN 启用）
const dsn = (import.meta as any).env?.VITE_SENTRY_DSN
if (dsn) {
  Sentry.init({
    app,
    dsn,
    integrations: [
      Sentry.browserTracingIntegration(),
      Sentry.replayIntegration()
    ],
    tracesSampleRate: 0.1,
    replaysSessionSampleRate: 0.1,
    replaysOnErrorSampleRate: 1.0
  })
}

// Service Worker：仅在生产环境注册；开发环境主动注销，避免拦截 Vite 模块与 HMR
if ('serviceWorker' in navigator) {
  if (import.meta.env.PROD) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('/sw.js').catch(() => {
        // ignore
      })
    })
  } else {
    // 开发环境清理已注册的 SW 与缓存，修复刷新后 500 / HMR 断开问题
    window.addEventListener('load', () => {
      navigator.serviceWorker.getRegistrations().then((regs) => {
        regs.forEach((r) => r.unregister())
      })
      if ('caches' in window) {
        caches.keys().then((keys) => keys.forEach((k) => caches.delete(k)))
      }
    })
  }
}





