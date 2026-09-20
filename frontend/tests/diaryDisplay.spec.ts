import { describe, expect, it } from 'vitest'
import { asDownloadBlob } from '@/api/diary'
import {
  DiaryMood,
  formatUpdatedAt,
  inMonth,
  monthKey,
  monthKeyFromDate,
  moodLabel,
  parseDiaryMood,
  shiftMonth,
  todayDateKey,
} from '@/utils/diaryDisplay'

describe('diaryDisplay', () => {
  it('parses known moods and falls back to neutral', () => {
    expect(parseDiaryMood('happy')).toBe(DiaryMood.Happy)
    expect(parseDiaryMood('unknown')).toBe(DiaryMood.Neutral)
    expect(moodLabel(DiaryMood.Energetic)).toBe('充满活力')
  })

  it('shows clock time when updated on the diary date', () => {
    const sameDay = new Date(2026, 8, 20, 18, 31, 0)
    expect(formatUpdatedAt(sameDay.toISOString(), '2026-09-20')).toBe('更新于 18:31')
    const prevDay = new Date(2026, 8, 19, 18, 31, 0)
    expect(formatUpdatedAt(prevDay.toISOString(), '2026-09-20')).toMatch(/^更新于 /)
    expect(formatUpdatedAt(prevDay.toISOString(), '2026-09-20')).not.toBe('更新于 18:31')
  })

  it('formats today as YYYY-MM-DD', () => {
    expect(todayDateKey(new Date(2026, 8, 20, 8, 0, 0))).toBe('2026-09-20')
  })

  it('shifts and matches months', () => {
    expect(monthKey(new Date(2026, 8, 20))).toBe('2026-09')
    expect(monthKeyFromDate('2026-09-19')).toBe('2026-09')
    expect(shiftMonth('2026-09', -1)).toBe('2026-08')
    expect(shiftMonth('2026-01', -1)).toBe('2025-12')
    expect(inMonth('2026-09-19', '2026-09')).toBe(true)
    expect(inMonth('2026-08-01', '2026-09')).toBe(false)
  })
})

describe('asDownloadBlob', () => {
  it('rejects json error blobs', async () => {
    const blob = new Blob([JSON.stringify({ message: '导出失败' })], { type: 'application/json' })
    await expect(asDownloadBlob(blob, 'application/pdf')).rejects.toThrow('导出失败')
  })

  it('keeps pdf bytes', async () => {
    const blob = new Blob(['%PDF-1.4 mock'], { type: 'application/pdf' })
    const out = await asDownloadBlob(blob, 'application/pdf')
    expect(out.type).toBe('application/pdf')
    expect(out.size).toBeGreaterThan(0)
  })
})
