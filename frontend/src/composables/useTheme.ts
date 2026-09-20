import { onMounted, onBeforeUnmount, ref } from 'vue'

export type ThemeName = 'light' | 'dark'

export function applyTheme(theme: ThemeName) {
  document.documentElement.setAttribute('data-theme', theme)
  document.documentElement.classList.toggle('dark', theme === 'dark')
  localStorage.setItem('theme', theme)
}

export function resolveInitialTheme(): ThemeName {
  const stored = localStorage.getItem('theme') as ThemeName | null
  if (stored === 'light' || stored === 'dark') return stored
  const prefersDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches
  return prefersDark ? 'dark' : 'light'
}

export function useTheme() {
  const isDark = ref(document.documentElement.getAttribute('data-theme') === 'dark')

  const syncFromDom = () => {
    isDark.value = document.documentElement.getAttribute('data-theme') === 'dark'
  }

  const toggleTheme = () => {
    applyTheme(isDark.value ? 'dark' : 'light')
  }

  onMounted(() => {
    window.addEventListener('storage', syncFromDom)
  })
  onBeforeUnmount(() => {
    window.removeEventListener('storage', syncFromDom)
  })

  return { isDark, toggleTheme, applyTheme }
}
