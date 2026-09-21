import { beforeEach, describe, expect, it } from 'vitest'
import { DiaryMood } from '@/utils/diaryDisplay'
import {
  MoodName,
  ThemeName,
  applyMood,
  applyTheme,
  moodFromDiary,
  resolveInitialMood,
  resolveInitialTheme,
} from './useTheme'

describe('useTheme mood', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.removeAttribute('data-theme')
    document.documentElement.removeAttribute('data-mood')
    document.documentElement.classList.remove('dark')
  })

  it('applyMood sets data-mood and persists', () => {
    applyMood(MoodName.Pool)
    expect(document.documentElement.getAttribute('data-mood')).toBe('pool')
    expect(localStorage.getItem('mood')).toBe('pool')
  })

  it('resolveInitialMood defaults to ocean and rejects unknown values', () => {
    expect(resolveInitialMood()).toBe(MoodName.Ocean)
    localStorage.setItem('mood', 'neon')
    expect(resolveInitialMood()).toBe(MoodName.Ocean)
    localStorage.setItem('mood', 'rain')
    expect(resolveInitialMood()).toBe(MoodName.Rain)
  })

  it('applyTheme still respects storage and dark class', () => {
    applyTheme(ThemeName.Dark)
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
    expect(document.documentElement.classList.contains('dark')).toBe(true)
    expect(resolveInitialTheme()).toBe(ThemeName.Dark)
  })

  it('maps diary mood onto a liquid mood', () => {
    expect(moodFromDiary(DiaryMood.Happy)).toBe(MoodName.Crystal)
    expect(moodFromDiary(DiaryMood.Energetic)).toBe(MoodName.Ocean)
    expect(moodFromDiary(DiaryMood.Tired)).toBe(MoodName.Rain)
    expect(moodFromDiary(DiaryMood.Neutral)).toBe(MoodName.Pool)
  })
})
