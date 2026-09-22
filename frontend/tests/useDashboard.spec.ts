import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useDashboard } from '@/composables/useDashboard'
import {
  getDashboardStats,
  getRecentActivities,
  getTodayRhythm,
  getTodayTasks,
} from '@/api/dashboard'
import { getCheckInHistory, getCheckInHistoryWithHeatValue } from '@/api/checkin'
import { getCalendarData } from '@/api/calendar'
import { getPomodoroHistory } from '@/api/pomodoro'
import { getTodayWords, getWords } from '@/api/words'

vi.mock('@/api/dashboard', () => ({
  getDashboardStats: vi.fn(),
  getTodayTasks: vi.fn(),
  getRecentActivities: vi.fn(),
  getTodayRhythm: vi.fn(),
}))

vi.mock('@/api/checkin', () => ({
  getCheckInHistory: vi.fn(),
  getCheckInHistoryWithHeatValue: vi.fn(),
}))

vi.mock('@/api/calendar', () => ({
  getCalendarData: vi.fn(),
}))

vi.mock('@/api/pomodoro', () => ({
  getPomodoroHistory: vi.fn(),
}))

vi.mock('@/api/words', () => ({
  getWords: vi.fn(),
  getTodayWords: vi.fn(),
}))

function dateKey(d: Date) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function rangeStart() {
  const start = new Date()
  start.setDate(start.getDate() - 6)
  start.setHours(12, 0, 0, 0)
  return start
}

async function mountDashboard() {
  const Comp = defineComponent({
    setup() {
      return useDashboard()
    },
    template: '<div />',
  })
  const wrapper = mount(Comp)
  await flushPromises()
  return wrapper
}

describe('useDashboard', () => {
  beforeEach(() => {
    vi.mocked(getDashboardStats).mockResolvedValue({
      checkInDays: 2,
      wordCount: 1,
      pomodoroCount: 1,
      totalDays: 3,
    })
    vi.mocked(getTodayTasks).mockResolvedValue({
      hasCheckedIn: true,
      todayWordCount: 0,
      todayPomodoroCount: 0,
    })
    vi.mocked(getRecentActivities).mockResolvedValue([])
    vi.mocked(getTodayRhythm).mockResolvedValue({
      nextAction: 'FOCUS_FREE',
      reason: '今天该接的都接上了，自由专注也很好。',
      ctaLabel: '自由专注',
      ctaPath: '/pomodoro',
      hasCheckedIn: true,
      dueWordCount: 0,
      heat: {
        checkIn: 1,
        pomodoro: 4,
        word: 2,
        task: 9,
        total: 16,
        pomodoroCount: 2,
        wordCount: 2,
        taskCount: 3,
      },
      dueWords: [],
      focusTask: null,
    })
    vi.mocked(getCheckInHistory).mockResolvedValue([])
    vi.mocked(getCheckInHistoryWithHeatValue).mockResolvedValue([])
    vi.mocked(getCalendarData).mockResolvedValue({})
    vi.mocked(getPomodoroHistory).mockResolvedValue([
      { startTime: `${dateKey(rangeStart())}T12:00:00` },
    ])
    vi.mocked(getWords).mockResolvedValue([
      { id: 'w1', word: 'one' },
      { id: 'w2', word: 'two' },
      { id: 'w3', word: 'three' },
    ])
    vi.mocked(getTodayWords).mockResolvedValue([])
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('loads rhythm from the dashboard API', async () => {
    const wrapper = await mountDashboard()
    expect(wrapper.vm.rhythm.nextAction).toBe('FOCUS_FREE')
    expect(wrapper.vm.rhythm.ctaLabel).toBe('自由专注')
    expect(wrapper.vm.rhythm.heat.total).toBe(16)
  })

  it('keeps mastered word count from dashboard stats', async () => {
    const wrapper = await mountDashboard()
    expect(wrapper.vm.stats.wordCount).toBe(1)
  })

  it('formats activity time as just now or minutes ago', async () => {
    const wrapper = await mountDashboard()
    expect(wrapper.vm.formatActivityTime(new Date().toISOString())).toBe('刚刚')
    const fiveMinutesAgo = new Date(Date.now() - 5 * 60 * 1000).toISOString()
    expect(wrapper.vm.formatActivityTime(fiveMinutesAgo)).toBe('5分钟前')
  })

  it('opens the chart dialog for the clicked day', async () => {
    const wrapper = await mountDashboard()
    wrapper.vm.handleChartClick('pomodoro', { dataIndex: 0 })
    expect(wrapper.vm.dialogVisible).toBe(true)
    expect(wrapper.vm.dialogType).toBe('pomodoro')
    expect(wrapper.vm.dialogTitle).toContain('番茄钟记录')
    expect(wrapper.vm.dialogData).toHaveLength(1)
  })

  it('keeps the empty dashboard when APIs fail', async () => {
    vi.mocked(getDashboardStats).mockRejectedValue(new Error('network'))
    vi.mocked(getTodayTasks).mockRejectedValue(new Error('network'))
    vi.mocked(getRecentActivities).mockRejectedValue(new Error('network'))
    vi.mocked(getTodayRhythm).mockRejectedValue(new Error('network'))
    vi.mocked(getCheckInHistory).mockRejectedValue(new Error('network'))
    vi.mocked(getCheckInHistoryWithHeatValue).mockRejectedValue(new Error('network'))
    vi.mocked(getCalendarData).mockRejectedValue(new Error('network'))
    vi.mocked(getPomodoroHistory).mockRejectedValue(new Error('network'))
    vi.mocked(getWords).mockRejectedValue(new Error('network'))
    vi.mocked(getTodayWords).mockRejectedValue(new Error('network'))

    const wrapper = await mountDashboard()

    expect(wrapper.vm.rhythm.nextAction).toBe('CHECK_IN')
    expect(wrapper.vm.rhythm.ctaLabel).toBe('立即打卡')
    expect(wrapper.vm.stats.checkInDays).toBe(0)
    expect(wrapper.vm.recentActivities).toEqual([])
    expect(wrapper.vm.loadError).toBe('首页数据加载失败，请刷新重试')
  })

  it('keeps long-term metrics collapsed after a successful load', async () => {
    const wrapper = await mountDashboard()
    expect(wrapper.vm.longTermOpen).toBe(false)
    expect(wrapper.vm.loadError).toBe('')
  })
})
