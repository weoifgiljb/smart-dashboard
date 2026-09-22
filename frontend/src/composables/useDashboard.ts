import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import {
  getDashboardStats,
  getTodayTasks,
  getRecentActivities,
  getTodayRhythm,
} from '@/api/dashboard'
import { getCheckInHistory, getCheckInHistoryWithHeatValue } from '@/api/checkin'
import { getCalendarData } from '@/api/calendar'
import { getPomodoroHistory } from '@/api/pomodoro'
import { getWords, getTodayWords } from '@/api/words'
import { chartPalette } from '@/utils/themeTokens'

export interface DashboardStats {
  checkInDays: number
  wordCount: number
  pomodoroCount: number
  totalDays: number
}

export interface TodayTaskSummary {
  hasCheckedIn: boolean
  todayWordCount: number
  todayPomodoroCount: number
}

export type RhythmNextAction =
  | 'CHECK_IN'
  | 'REVIEW_WORDS'
  | 'FOCUS_TASK'
  | 'FOCUS_FREE'
  | 'WRITE_DIARY'

export interface TodayRhythm {
  nextAction: RhythmNextAction
  reason: string
  ctaLabel: string
  ctaPath: string
  hasCheckedIn: boolean
  dueWordCount: number
  heat: {
    checkIn: number
    pomodoro: number
    word: number
    task: number
    total: number
    pomodoroCount: number
    wordCount: number
    taskCount: number
  }
  dueWords: Array<{ id: string; word: string; translation?: string }>
  focusTask: { id: string; title: string } | null
}

export interface ActivityItem {
  type: string
  title: string
  time: string
}

type ChartClickParams = { dataIndex: number }

function asData<T>(value: unknown): T {
  return value as T
}

export function useDashboard() {
  const stats = ref<DashboardStats>({
    checkInDays: 0,
    wordCount: 0,
    pomodoroCount: 0,
    totalDays: 0,
  })

  const todayTasks = ref<TodayTaskSummary>({
    hasCheckedIn: false,
    todayWordCount: 0,
    todayPomodoroCount: 0,
  })

  const rhythm = ref<TodayRhythm>({
    nextAction: 'CHECK_IN',
    reason: '今天还没打卡，先记下这一天的起点。',
    ctaLabel: '立即打卡',
    ctaPath: '/calendar',
    hasCheckedIn: false,
    dueWordCount: 0,
    heat: {
      checkIn: 0,
      pomodoro: 0,
      word: 0,
      task: 0,
      total: 0,
      pomodoroCount: 0,
      wordCount: 0,
      taskCount: 0,
    },
    dueWords: [],
    focusTask: null,
  })

  const recentActivities = ref<ActivityItem[]>([])
  const heatValueOption = ref<Record<string, unknown>>({})
  const pomodoroOption = ref<Record<string, unknown>>({})
  const wordsOption = ref<Record<string, unknown>>({})
  const dateRange = ref<[Date, Date] | null>(null)
  const rawPomodoros = ref<Array<Record<string, unknown>>>([])
  const rawWords = ref<Array<Record<string, unknown>>>([])
  const dialogVisible = ref(false)
  const dialogTitle = ref('')
  const dialogData = ref<Array<Record<string, unknown>>>([])
  const dialogType = ref<'pomodoro' | 'word'>('pomodoro')
  const loadError = ref('')
  const longTermOpen = ref(false)

  const shortcuts = [
    {
      text: '最近一周',
      value: () => {
        const end = new Date()
        const start = new Date()
        start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
        return [start, end]
      },
    },
    {
      text: '最近一个月',
      value: () => {
        const end = new Date()
        const start = new Date()
        start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
        return [start, end]
      },
    },
    {
      text: '最近三个月',
      value: () => {
        const end = new Date()
        const start = new Date()
        start.setTime(start.getTime() - 3600 * 1000 * 24 * 90)
        return [start, end]
      },
    },
  ]

  const toDate = (val: unknown): Date | null => {
    if (!val) return null
    if (typeof val === 'number') {
      const ms = val < 1_000_000_000_000 ? val * 1000 : val
      return new Date(ms)
    }
    const d = new Date(String(val))
    return Number.isNaN(d.getTime()) ? null : d
  }

  const getDateKey = (d: Date) => {
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${y}-${m}-${day}`
  }

  const lastNDays = (n: number) => {
    const arr: Date[] = []
    const today = new Date()
    for (let i = n - 1; i >= 0; i--) {
      const d = new Date(today)
      d.setDate(today.getDate() - i)
      arr.push(d)
    }
    return arr
  }

  const getDaysArray = (start: Date, end: Date) => {
    const arr: Date[] = []
    for (let dt = new Date(start); dt <= end; dt.setDate(dt.getDate() + 1)) {
      arr.push(new Date(dt))
    }
    return arr
  }

  const buildHeatValueOption = (
    historyWithHeat: Array<Record<string, unknown>>,
    calAgg: Record<string, Record<string, number>>,
  ) => {
    const days = lastNDays(30)
    const map = new Map<string, number>()
    ;(historyWithHeat || []).forEach((h) => {
      const key = String(h?.checkInDate || '')
      const val = Number(h?.heatValue) || 0
      if (key) map.set(key, Math.max(val, map.get(key) || 0))
    })
    const labels = days.map(
      (d) => `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`,
    )
    const values = days.map((d) => {
      const key = getDateKey(d)
      const v = map.get(key) || 0
      const day = calAgg?.[key] || {}
      const fallback =
        1 +
        (Number(day.pomodoro) || 0) * 2 +
        (Number(day.word) || 0) * 1 +
        (Number(day.task) || 0) * 3
      return Math.max(v, fallback > 0 ? fallback : 0)
    })
    const maxVal = Math.max(10, ...values)
    heatValueOption.value = {
      grid: { left: 10, right: 10, top: 10, bottom: 20, containLabel: false },
      xAxis: {
        type: 'category',
        data: labels,
        axisTick: { show: false },
        axisLine: { show: false },
        axisLabel: { show: true, interval: 6, color: chartPalette().text, fontSize: 10 },
      },
      yAxis: {
        type: 'value',
        min: 0,
        max: maxVal,
        splitLine: { show: false },
        axisLabel: { show: false },
        axisLine: { show: false },
        axisTick: { show: false },
      },
      series: [
        {
          data: values,
          type: 'bar',
          barWidth: '50%',
          itemStyle: {
            borderRadius: [4, 4, 0, 0],
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                { offset: 0, color: chartPalette().primary },
                { offset: 1, color: 'rgba(16, 185, 129, 0.2)' },
              ],
            },
          },
        },
      ],
      tooltip: {
        trigger: 'axis',
        formatter: (params: Array<{ axisValue: string; data: number }>) => {
          const p = params?.[0]
          return `<div style="font-size:12px">${p.axisValue}<br/><span style="color:${chartPalette().primary}">●</span> 热力值：${p.data}</div>`
        },
      },
    }
  }

  const buildPomodoroOption = (
    pomodoros: Array<Record<string, unknown>>,
    start?: Date,
    end?: Date,
  ) => {
    const days = start && end ? getDaysArray(start, end) : lastNDays(7)
    const map = new Map<string, number>()
    days.forEach((d) => map.set(getDateKey(d), 0))
    pomodoros.forEach((p) => {
      const d = toDate(p?.startTime)
      if (!d) return
      const key = getDateKey(d)
      if (map.has(key)) map.set(key, (map.get(key) || 0) + 1)
    })
    pomodoroOption.value = {
      grid: { left: 10, right: 10, top: 10, bottom: 20, containLabel: false },
      xAxis: {
        type: 'category',
        data: days.map(
          (d) =>
            `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`,
        ),
        axisTick: { show: false },
        axisLine: { show: false },
        axisLabel: { show: true, fontSize: 10, color: chartPalette().text },
      },
      yAxis: {
        type: 'value',
        splitLine: { show: false },
        axisLabel: { show: false },
        axisLine: { show: false },
        axisTick: { show: false },
      },
      series: [
        {
          data: days.map((d) => map.get(getDateKey(d)) || 0),
          type: 'line',
          smooth: true,
          symbolSize: 6,
          areaStyle: { color: 'rgba(59, 130, 246, 0.1)' },
          lineStyle: { color: chartPalette().secondary, width: 3 },
          itemStyle: {
            color: chartPalette().secondary,
            borderWidth: 2,
            borderColor: chartPalette().bg,
          },
        },
      ],
      tooltip: { trigger: 'axis' },
    }
  }

  const buildWordsOption = (words: Array<Record<string, unknown>>, start?: Date, end?: Date) => {
    const days = start && end ? getDaysArray(start, end) : lastNDays(7)
    const mapAdd = new Map<string, number>()
    days.forEach((d) => mapAdd.set(getDateKey(d), 0))
    words.forEach((w) => {
      const d = toDate(w?.createTime)
      if (!d) return
      const key = getDateKey(d)
      if (mapAdd.has(key)) mapAdd.set(key, (mapAdd.get(key) || 0) + 1)
    })
    wordsOption.value = {
      grid: { left: 10, right: 10, top: 10, bottom: 20, containLabel: false },
      xAxis: {
        type: 'category',
        data: days.map(
          (d) =>
            `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`,
        ),
        axisTick: { show: false },
        axisLine: { show: false },
        axisLabel: { show: true, fontSize: 10, color: chartPalette().text },
      },
      yAxis: {
        type: 'value',
        splitLine: { show: false },
        axisLabel: { show: false },
        axisLine: { show: false },
        axisTick: { show: false },
      },
      series: [
        {
          data: days.map((d) => mapAdd.get(getDateKey(d)) || 0),
          type: 'bar',
          barWidth: '50%',
          itemStyle: { color: chartPalette().primary, borderRadius: [4, 4, 0, 0] },
        },
      ],
      tooltip: { trigger: 'axis' },
    }
  }

  const updateCharts = () => {
    if (!dateRange.value) return
    const [start, end] = dateRange.value
    const filteredPomodoros = rawPomodoros.value.filter((p) => {
      const d = toDate(p?.startTime)
      return d && d >= start && d <= end
    })
    const filteredWords = rawWords.value.filter((w) => {
      const d = toDate(w?.createTime)
      return d && d >= start && d <= end
    })
    buildPomodoroOption(filteredPomodoros, start, end)
    buildWordsOption(filteredWords, start, end)
  }

  const handleChartClick = (type: 'pomodoro' | 'word', params: ChartClickParams) => {
    if (!dateRange.value) return
    const days = getDaysArray(dateRange.value[0], dateRange.value[1])
    const date = days[params.dataIndex]
    if (!date) return
    const key = getDateKey(date)
    if (type === 'pomodoro') {
      dialogData.value = rawPomodoros.value.filter((p) => {
        const d = toDate(p?.startTime)
        return d && getDateKey(d) === key
      })
      dialogTitle.value = `${key} 番茄钟记录`
    } else {
      dialogData.value = rawWords.value.filter((w) => {
        const d = toDate(w?.createTime)
        return d && getDateKey(d) === key
      })
      dialogTitle.value = `${key} 新增单词`
    }
    dialogType.value = type
    dialogVisible.value = true
  }

  const formatActivityTime = (timeStr: string) => {
    const date = new Date(timeStr)
    const now = new Date()
    const diff = now.getTime() - date.getTime()
    const minutes = Math.floor(diff / 60000)
    const hours = Math.floor(minutes / 60)
    const days = Math.floor(hours / 24)
    if (minutes < 1) return '刚刚'
    if (minutes < 60) return `${minutes}分钟前`
    if (hours < 24) return `${hours}小时前`
    if (days < 7) return `${days}天前`
    return date.toLocaleDateString('zh-CN')
  }

  onMounted(async () => {
    try {
      loadError.value = ''
      const today = new Date()
      const start = new Date(today)
      start.setDate(today.getDate() - 29)
      const toKey = (d: Date) => getDateKey(d)
      const [
        statsData,
        tasksData,
        activitiesData,
        rhythmData,
        checkins,
        historyWithHeat,
        calAgg,
        pomodoros,
        words,
        todayWords,
      ] = await Promise.all([
        getDashboardStats(),
        getTodayTasks(),
        getRecentActivities(),
        getTodayRhythm(),
        getCheckInHistory({ page: 0, size: 400 }),
        getCheckInHistoryWithHeatValue(),
        getCalendarData({ start: toKey(start), end: toKey(today) }),
        getPomodoroHistory(),
        getWords(),
        getTodayWords(),
      ])

      rawPomodoros.value = asData<Array<Record<string, unknown>>>(pomodoros) || []
      rawWords.value = asData<Array<Record<string, unknown>>>(words) || []
      stats.value = asData<DashboardStats>(statsData)
      todayTasks.value = asData<TodayTaskSummary>(tasksData)
      const loadedRhythm = asData<TodayRhythm>(rhythmData)
      if (loadedRhythm?.nextAction) {
        rhythm.value = {
          ...rhythm.value,
          ...loadedRhythm,
          heat: { ...rhythm.value.heat, ...(loadedRhythm.heat || {}) },
          dueWords: loadedRhythm.dueWords || [],
        }
      }
      if (Array.isArray(todayWords)) {
        todayTasks.value.todayWordCount = todayWords.length
      }
      recentActivities.value = asData<ActivityItem[]>(activitiesData) || []
      buildHeatValueOption(
        asData<Array<Record<string, unknown>>>(historyWithHeat) || [],
        asData<Record<string, Record<string, number>>>(calAgg) || {},
      )

      const rangeEnd = new Date()
      const rangeStart = new Date()
      rangeStart.setDate(rangeEnd.getDate() - 6)
      dateRange.value = [rangeStart, rangeEnd]
      updateCharts()

      const dateSet = new Set<string>(
        (asData<Array<{ checkInDate?: string }>>(checkins) || [])
          .map((c) => c?.checkInDate)
          .filter((v): v is string => Boolean(v)),
      )
      let anchor: Date | null = null
      for (let i = 0; i < 365; i++) {
        const d = new Date(today)
        d.setDate(today.getDate() - i)
        if (dateSet.has(getDateKey(d))) {
          anchor = d
          break
        }
      }
      let streak = 0
      if (anchor) {
        let currentAnchor = anchor
        while (dateSet.has(getDateKey(currentAnchor))) {
          streak++
          const prev = new Date(currentAnchor)
          prev.setDate(currentAnchor.getDate() - 1)
          currentAnchor = prev
        }
      }
      if (!Number.isFinite(stats.value.checkInDays) || stats.value.checkInDays < streak) {
        stats.value.checkInDays = streak
      }
      if (!Number.isFinite(stats.value.totalDays) || stats.value.totalDays < dateSet.size) {
        stats.value.totalDays = dateSet.size
      }
      if (
        !Number.isFinite(stats.value.pomodoroCount) ||
        stats.value.pomodoroCount < rawPomodoros.value.length
      ) {
        stats.value.pomodoroCount = rawPomodoros.value.length
      }
      const todayKey = getDateKey(new Date())
      const todayPomodoros = rawPomodoros.value.filter((p) => {
        const d = toDate(p?.startTime)
        return d ? getDateKey(d) === todayKey : false
      }).length
      todayTasks.value.todayPomodoroCount = todayPomodoros
    } catch {
      loadError.value = '首页数据加载失败，请刷新重试'
    }
  })

  watch(dateRange, () => updateCharts())

  let themeObserver: MutationObserver | null = null
  onMounted(() => {
    themeObserver = new MutationObserver(() => updateCharts())
    themeObserver.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ['data-theme', 'class'],
    })
  })
  onBeforeUnmount(() => themeObserver?.disconnect())

  return {
    stats,
    todayTasks,
    rhythm,
    recentActivities,
    heatValueOption,
    pomodoroOption,
    wordsOption,
    dateRange,
    dialogVisible,
    dialogTitle,
    dialogData,
    dialogType,
    shortcuts,
    handleChartClick,
    formatActivityTime,
    loadError,
    longTermOpen,
  }
}
