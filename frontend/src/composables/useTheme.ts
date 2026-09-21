import { onBeforeUnmount, onMounted, ref } from 'vue'
import { DiaryMood } from '@/utils/diaryDisplay'

export enum ThemeName {
  Light = 'light',
  Dark = 'dark',
}

export enum MoodName {
  Pool = 'pool',
  Ocean = 'ocean',
  Crystal = 'crystal',
  Rain = 'rain',
}

const MOODS: MoodName[] = [MoodName.Pool, MoodName.Ocean, MoodName.Crystal, MoodName.Rain]

function isBrowser() {
  return typeof document !== 'undefined' && typeof localStorage !== 'undefined'
}

export function applyTheme(theme: ThemeName) {
  if (!isBrowser()) return
  document.documentElement.setAttribute('data-theme', theme)
  document.documentElement.classList.toggle('dark', theme === ThemeName.Dark)
  localStorage.setItem('theme', theme)
}

export function applyMood(mood: MoodName) {
  if (!isBrowser()) return
  document.documentElement.setAttribute('data-mood', mood)
  localStorage.setItem('mood', mood)
}

export function resolveInitialTheme(): ThemeName {
  if (!isBrowser()) return ThemeName.Light
  const stored = localStorage.getItem('theme')
  if (stored === ThemeName.Light || stored === ThemeName.Dark) return stored
  const prefersDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches
  return prefersDark ? ThemeName.Dark : ThemeName.Light
}

export function resolveInitialMood(): MoodName {
  if (!isBrowser()) return MoodName.Ocean
  const stored = localStorage.getItem('mood')
  return MOODS.find((item) => item === stored) ?? MoodName.Ocean
}

export function moodFromDiary(mood: DiaryMood | string | undefined): MoodName {
  switch (mood) {
    case DiaryMood.Happy:
      return MoodName.Crystal
    case DiaryMood.Energetic:
      return MoodName.Ocean
    case DiaryMood.Sad:
    case DiaryMood.Tired:
      return MoodName.Rain
    case DiaryMood.Neutral:
      return MoodName.Pool
    default:
      return MoodName.Ocean
  }
}

export function useTheme() {
  const isDark = ref(
    isBrowser() && document.documentElement.getAttribute('data-theme') === ThemeName.Dark,
  )
  const mood = ref<MoodName>(resolveInitialMood())

  const syncFromDom = () => {
    if (!isBrowser()) return
    isDark.value = document.documentElement.getAttribute('data-theme') === ThemeName.Dark
    const current = document.documentElement.getAttribute('data-mood')
    mood.value = MOODS.find((item) => item === current) ?? MoodName.Ocean
  }

  const toggleTheme = () => {
    applyTheme(isDark.value ? ThemeName.Dark : ThemeName.Light)
  }

  const setMood = (next: MoodName) => {
    applyMood(next)
    mood.value = next
  }

  onMounted(() => {
    window.addEventListener('storage', syncFromDom)
  })
  onBeforeUnmount(() => {
    window.removeEventListener('storage', syncFromDom)
  })

  return { isDark, mood, toggleTheme, applyTheme, setMood, applyMood }
}
