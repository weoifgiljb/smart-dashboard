<template>
  <div class="calendar-page">
    <div class="header-section">
      <div class="page-title">
        <h2>日历与打卡</h2>
        <p class="subtitle">记录每一天的努力与坚持</p>
      </div>
      <div class="actions">
        <el-button 
          type="primary" 
          size="large" 
          :disabled="hasCheckedIn"
          class="checkin-btn"
          @click="handleCheckIn"
        >
          <el-icon class="el-icon--left" v-if="hasCheckedIn"><Check /></el-icon>
          <el-icon class="el-icon--left" v-else><CalendarIcon /></el-icon>
          {{ hasCheckedIn ? '今日已打卡' : '立即打卡' }}
        </el-button>
      </div>
    </div>

    <!-- Bento Grid Layout -->
    <div class="bento-grid">
      <!-- 统计卡片区 -->
      <div class="bento-item stats-card" style="grid-area: stats">
        <div class="stat-group">
          <div class="stat-box">
            <div class="value">{{ consecutiveDays }}<span class="unit">天</span></div>
            <div class="label">连续打卡</div>
          </div>
          <el-divider direction="vertical" />
          <div class="stat-box">
            <div class="value">{{ totalDays }}<span class="unit">天</span></div>
            <div class="label">累计坚持</div>
          </div>
          <el-divider direction="vertical" />
          <div class="stat-box">
             <div class="value year-status">
               <span v-if="yearHeatmapAvailable" class="active">●</span>
               <span v-else class="loading">○</span>
             </div>
             <div class="label">热力图就绪</div>
          </div>
        </div>
      </div>

      <!-- 热力图区 -->
      <div class="bento-item heatmap-card" style="grid-area: heatmap">
        <div class="card-title">
          <span>年度活动热力</span>
          <el-tooltip content="颜色深浅代表综合活跃度：打卡+番茄+单词+任务" placement="top">
            <el-icon class="info-icon"><InfoFilled /></el-icon>
          </el-tooltip>
        </div>
        <div class="chart-wrapper">
          <BaseChart :option="calendarOption" height="180px" />
        </div>
      </div>

      <!-- 月历区 -->
      <div class="bento-item calendar-main" style="grid-area: main">
        <div class="calendar-toolbar">
          <div class="month-selector">
            <el-button circle size="small" @click="goPrevMonth"><el-icon><ArrowLeft /></el-icon></el-button>
            <span class="current-month">{{ currentYear }}年 {{ currentMonth + 1 }}月</span>
            <el-button circle size="small" @click="goNextMonth"><el-icon><ArrowRight /></el-icon></el-button>
            <el-button size="small" text bg @click="goToday">今天</el-button>
          </div>
          <div class="settings">
            <el-radio-group v-model="firstDayOfWeek" size="small">
              <el-radio-button :label="1">周一</el-radio-button>
              <el-radio-button :label="0">周日</el-radio-button>
            </el-radio-group>
          </div>
        </div>
        
        <div class="calendar-body">
          <div class="week-header">
            <div v-for="w in weekLabels" :key="w" class="week-day">{{ w }}</div>
          </div>
          <div class="day-grid">
            <div
              v-for="(cell, idx) in dayCells"
              :key="idx"
              class="day-cell"
              :class="{ 
                'is-other': !cell.inMonth, 
                'is-today': cell.key === todayKey,
                'has-events': hasEvents(cell.key)
              }"
              @click="cell.inMonth && openDay(cell.key)"
            >
              <div class="date-num">{{ cell.date.getDate() }}</div>
              <div class="dots-indicator">
                <span v-if="stats[cell.key]?.checkin" class="dot checkin"></span>
                <span v-if="stats[cell.key]?.pomodoro" class="dot pomodoro"></span>
                <span v-if="stats[cell.key]?.word" class="dot word"></span>
                <span v-if="stats[cell.key]?.task" class="dot task"></span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-drawer v-model="drawerOpen" :title="drawerTitle" size="400px" destroy-on-close class="day-detail-drawer">
      <div v-if="loadingDay" class="drawer-loading">
        <el-skeleton :rows="6" animated />
      </div>
      <div v-else class="drawer-content">
        <template v-if="dayDetails">
          <div class="summary-cards">
            <div class="summary-item">
              <div class="s-val">{{ (dayDetails.checkins || []).length }}</div>
              <div class="s-label">打卡</div>
            </div>
            <div class="summary-item">
              <div class="s-val">{{ (dayDetails.pomodoros || []).length }}</div>
              <div class="s-label">番茄</div>
            </div>
            <div class="summary-item">
              <div class="s-val">{{ (dayDetails.words || []).length }}</div>
              <div class="s-label">单词</div>
            </div>
             <div class="summary-item">
              <div class="s-val">{{ (dayDetails.tasks || []).length }}</div>
              <div class="s-label">任务</div>
            </div>
          </div>

          <div class="timeline-section">
            <h4 class="section-title">今日时间轴</h4>
            <el-timeline>
              <el-timeline-item
                v-for="(item, idx) in sortedTimelineEvents"
                :key="idx"
                :type="item.type"
                :color="item.color"
                :timestamp="item.time"
                placement="top"
              >
                <el-card class="timeline-card" shadow="hover">
                  <div class="t-header">
                    <span class="t-title">{{ item.title }}</span>
                    <el-tag size="small" :type="item.tagType" v-if="item.tag">{{ item.tag }}</el-tag>
                  </div>
                  <div class="t-desc" v-if="item.desc">{{ item.desc }}</div>
                </el-card>
              </el-timeline-item>
              <el-timeline-item v-if="sortedTimelineEvents.length === 0" timestamp="" color="#e4e7ed">
                <span style="color: #909399">暂无活动记录</span>
              </el-timeline-item>
            </el-timeline>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import { ArrowLeft, ArrowRight, InfoFilled, Calendar, Check } from '@element-plus/icons-vue'
import { getCalendarData, getCalendarDay } from '@/api/calendar'
import { checkIn, getCheckInStats } from '@/api/checkin'
import BaseChart from '@/components/charts/BaseChart.vue'

// Avoid conflict with local component registration
const CalendarIcon = Calendar

type StatsMap = Record<
  string,
  { checkin?: number; pomodoro?: number; word?: number; task?: number }
>

// 日历相关
const today = new Date()
const todayKey = getKey(today)
const current = ref(new Date(today))
const firstDayOfWeek = ref<number>(1) // 1=Mon, 0=Sun

const currentYear = computed(() => current.value.getFullYear())
const currentMonth = computed(() => current.value.getMonth())

const stats = ref<StatsMap>({})

// 打卡相关
const hasCheckedIn = ref(false)
const consecutiveDays = ref(0)
const totalDays = ref(0)
const calendarOption = ref<any>({})
const yearHeatmapAvailable = ref(false)

const weekLabels = computed(() => {
  const sunFirst = ['日', '一', '二', '三', '四', '五', '六']
  if (firstDayOfWeek.value === 1) {
    return ['一', '二', '三', '四', '五', '六', '日']
  }
  return sunFirst
})

const dayCells = computed(() => {
  const year = currentYear.value
  const month = currentMonth.value
  const first = new Date(year, month, 1)
  const last = new Date(year, month + 1, 0)
  let leading = first.getDay() // 0..6 (Sun..Sat)
  if (firstDayOfWeek.value === 1) {
    leading = (leading + 6) % 7 // shift so Monday=0
  }
  const totalDays = last.getDate()
  const cells: Array<{ date: Date; key: string; inMonth: boolean }> = []
  // leading blanks
  for (let i = 0; i < leading; i++) {
    const d = new Date(first)
    d.setDate(first.getDate() - (leading - i))
    cells.push({ date: d, key: getKey(d), inMonth: false })
  }
  // month days
  for (let d = 1; d <= totalDays; d++) {
    const date = new Date(year, month, d)
    cells.push({ date, key: getKey(date), inMonth: true })
  }
  // trailing to fill full weeks (42 cells)
  while (cells.length % 7 !== 0) {
    const lastDate = cells[cells.length - 1].date
    const d = new Date(lastDate)
    d.setDate(lastDate.getDate() + 1)
    cells.push({ date: d, key: getKey(d), inMonth: false })
  }
  return cells
})

const hasEvents = (key: string) => {
  const s = stats.value[key]
  if (!s) return false
  return (s.checkin || 0) + (s.pomodoro || 0) + (s.word || 0) + (s.task || 0) > 0
}

const goPrevMonth = () => {
  const d = new Date(current.value)
  d.setMonth(d.getMonth() - 1)
  current.value = d
}
const goNextMonth = () => {
  const d = new Date(current.value)
  d.setMonth(d.getMonth() + 1)
  current.value = d
}
const goToday = () => {
  current.value = new Date()
}

// 打卡处理函数
const handleCheckIn = async () => {
  try {
    await checkIn()
    ElNotification({
      title: '打卡成功！',
      message: '坚持就是胜利，今天也向前迈进了一步。',
      type: 'success',
    })
    hasCheckedIn.value = true
    // 尝试刷新数据，即使失败也不影响打卡成功的状态
    try {
      await loadCheckInStats()
      await loadRange() 
    } catch (e) {
      console.warn('刷新日历数据失败', e)
    }
  } catch (error: any) {
    console.error('打卡请求失败', error)
    ElMessage.error(error.response?.data?.message || '打卡失败，请稍后重试')
  }
}

// 加载打卡统计
const loadCheckInStats = async () => {
  try {
    const stats: any = await getCheckInStats()
    consecutiveDays.value = stats.consecutiveDays || 0
    totalDays.value = stats.totalDays || 0
    hasCheckedIn.value = stats.hasCheckedInToday || false
  } catch (error) {
    console.error('获取打卡统计失败', error)
  }
}

// 加载打卡热力图
const loadCheckinHeatmap = async () => {
  try {
    const { getCheckInHistoryWithHeatValue } = await import('@/api/checkin')
    const { getCalendarData } = await import('@/api/calendar')

    const year = new Date().getFullYear()
    const start = `${year}-01-01`
    const end = `${year}-12-31`
    const [historyResp, calendarAgg]: any[] = await Promise.all([
      getCheckInHistoryWithHeatValue(),
      getCalendarData({ start, end }),
    ])
    const historyData = historyResp?.data || historyResp || []
    const agg = calendarAgg || {}

    let maxHeat = 10 
    const data = (Array.isArray(historyData) ? historyData : []).map((c: any) => {
      const key = c.checkInDate
      const raw = Number(c.heatValue) || 0
      const day = agg?.[key] || {}
      const fallback =
        1 +
        (Number(day.pomodoro) || 0) * 2 +
        (Number(day.word) || 0) * 1 +
        (Number(day.task) || 0) * 3
      const heatValue = raw > 0 ? raw : fallback > 0 ? fallback : 0
      maxHeat = Math.max(maxHeat, heatValue)
      return [key, heatValue]
    })

    calendarOption.value = {
      tooltip: {
        formatter: (p: any) => {
          const date = p.data?.[0]
          const heat = p.data?.[1]
          return heat ? `${date}：热力值 ${heat}` : `${date}：未打卡`
        },
      },
      visualMap: {
        show: false, // 隐藏图例以节省空间，颜色已足够说明
        min: 0,
        max: maxHeat,
        inRange: {
          color: ['#ebedf0', '#9be9a8', '#40c463', '#30a14e', '#216e39'], // GitHub Green Style
        },
      },
      calendar: {
        top: 25,
        left: 30,
        right: 30,
        range: `${year}`,
        cellSize: ['auto', 14],
        splitLine: { show: false },
        itemStyle: { borderWidth: 3, borderColor: '#fff' }, // 白色边框模拟间距
        yearLabel: { show: false },
        monthLabel: { nameMap: 'cn', fontSize: 10, color: '#9ca3af' },
        dayLabel: { nameMap: 'cn', fontSize: 10, color: '#9ca3af' },
      },
      series: [
        {
          type: 'heatmap',
          coordinateSystem: 'calendar',
          data,
          itemStyle: {
            borderRadius: 2
          }
        },
      ],
    }
    yearHeatmapAvailable.value = true
  } catch (error) {
    console.error('获取打卡热力图失败', error)
  }
}

watch([current, firstDayOfWeek], async () => {
  await loadRange()
})

onMounted(async () => {
  await loadCheckInStats()
  await loadCheckinHeatmap()
  await loadRange()
})

async function loadRange() {
  try {
    const year = currentYear.value
    const month = currentMonth.value
    const start = `${year}-${String(month + 1).padStart(2, '0')}-01`
    const lastDay = new Date(year, month + 1, 0).getDate()
    const end = `${year}-${String(month + 1).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`
    const data: any = await getCalendarData({ start, end })
    stats.value = data || {}
  } catch (e) {
    ElMessage.error('获取日历数据失败')
  }
}

const drawerOpen = ref(false)
const drawerTitle = ref('')
const loadingDay = ref(false)
const dayDetails = ref<any>(null)

// 转换详情数据为时间轴格式
const sortedTimelineEvents = computed(() => {
  if (!dayDetails.value) return []
  const d = dayDetails.value
  const events: any[] = []

  // Checkins
  ;(d.checkins || []).forEach((c: any) => {
    events.push({
      time: c.checkInTime ? formatTime(c.checkInTime) : '',
      timestamp: new Date(c.checkInTime || 0).getTime(),
      title: '每日打卡',
      type: 'success',
      color: '#10b981',
      tag: 'Check-in',
      tagType: 'success'
    })
  })

  // Pomodoros
  ;(d.pomodoros || []).forEach((p: any) => {
    events.push({
      time: p.startTime ? formatTime(p.startTime) : '',
      timestamp: new Date(p.startTime || 0).getTime(),
      title: p.type === 'work' ? '专注时间' : '休息时间',
      desc: `时长：${p.duration}分钟`,
      type: p.type === 'work' ? 'primary' : 'warning',
      color: p.type === 'work' ? '#3b82f6' : '#f59e0b',
      tag: p.type === 'work' ? 'Focus' : 'Break',
      tagType: p.type === 'work' ? '' : 'warning'
    })
  })

  // Words - 汇总显示还是单条？太多单词会刷屏，这里汇总显示
  if ((d.words || []).length > 0) {
    // 找最早的时间
    const firstWord = d.words[0]
    events.push({
      time: firstWord.createTime ? formatTime(firstWord.createTime) : '',
      timestamp: new Date(firstWord.createTime || 0).getTime(),
      title: `学习了 ${d.words.length} 个单词`,
      desc: d.words.map((w: any) => w.word).slice(0, 5).join(', ') + (d.words.length > 5 ? '...' : ''),
      type: 'info',
      color: '#06b6d4',
      tag: 'Vocabulary',
      tagType: 'info'
    })
  }

  // Tasks - 完成的任务
  ;(d.tasks || []).filter((t:any) => t.status === 'done').forEach((t: any) => {
    events.push({
      time: '', // 任务往往没有精确完成时间，放最后
      timestamp: 9999999999999,
      title: `完成任务：${t.title}`,
      type: 'success',
      color: '#8b5cf6',
      tag: 'Task',
      tagType: ''
    })
  })

  return events.sort((a, b) => a.timestamp - b.timestamp)
})

async function openDay(key: string) {
  drawerTitle.value = `${key} 活动详情`
  drawerOpen.value = true
  loadingDay.value = true
  try {
    dayDetails.value = await getCalendarDay(key)
  } catch (e) {
    ElMessage.error('获取当天详情失败')
  } finally {
    loadingDay.value = false
  }
}

function getKey(d: Date) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}
function formatTime(s?: string | null) {
  if (!s) return '-'
  const d = new Date(s)
  if (isNaN(d.getTime())) return '-'
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${hh}:${mm}`
}
</script>

<style scoped>
.calendar-page {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

/* Header */
.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.page-title h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--app-text);
  margin: 0 0 8px 0;
}

.subtitle {
  color: var(--text-secondary);
  margin: 0;
  font-size: 14px;
}

.checkin-btn {
  font-weight: 600;
  box-shadow: var(--shadow-colored);
}

/* Bento Grid */
.bento-grid {
  display: grid;
  grid-template-columns: 300px 1fr;
  grid-template-rows: auto auto;
  grid-template-areas:
    "stats main"
    "heatmap main";
  gap: 24px;
}

.bento-item {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border);
  overflow: hidden;
}

/* Stats Card */
.stats-card {
  padding: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-group {
  display: flex;
  width: 100%;
  justify-content: space-between;
}

.stat-box {
  text-align: center;
  flex: 1;
}

.stat-box .value {
  font-size: 24px;
  font-weight: 800;
  color: var(--app-text);
  line-height: 1.2;
}

.stat-box .unit {
  font-size: 12px;
  font-weight: normal;
  color: var(--text-light);
  margin-left: 2px;
}

.stat-box .label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.year-status .active { color: var(--success); }
.year-status .loading { color: var(--text-light); animation: blink 1s infinite; }

@keyframes blink { 50% { opacity: 0.5; } }

/* Heatmap Card */
.heatmap-card {
  padding: 20px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--app-text);
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.info-icon {
  color: var(--text-light);
  cursor: help;
  font-size: 14px;
}

/* Calendar Main */
.calendar-main {
  padding: 24px;
  display: flex;
  flex-direction: column;
}

.calendar-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.month-selector {
  display: flex;
  align-items: center;
  gap: 12px;
}

.current-month {
  font-size: 18px;
  font-weight: 700;
  color: var(--app-text);
  min-width: 120px;
  text-align: center;
}

.week-header {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  margin-bottom: 12px;
}

.week-day {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-light);
  text-transform: uppercase;
}

.day-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
  flex: 1;
}

.day-cell {
  aspect-ratio: 1;
  border-radius: var(--radius-md);
  border: 1px solid transparent;
  padding: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.day-cell:hover {
  background-color: var(--app-bg);
  border-color: var(--border);
}

.day-cell.is-other {
  opacity: 0.3;
}

.day-cell.is-today {
  background-color: var(--primary-light);
  color: var(--primary);
  font-weight: 700;
}

.date-num {
  font-size: 14px;
}

.dots-indicator {
  display: flex;
  gap: 4px;
  justify-content: center;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.dot.checkin { background-color: var(--success); }
.dot.pomodoro { background-color: var(--warning); }
.dot.word { background-color: var(--info); }
.dot.task { background-color: #8b5cf6; }

/* Drawer Styles */
.summary-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-bottom: 24px;
}

.summary-item {
  background: var(--app-bg);
  padding: 12px 4px;
  border-radius: 8px;
  text-align: center;
}

.s-val {
  font-weight: 700;
  font-size: 18px;
  color: var(--app-text);
}

.s-label {
  font-size: 11px;
  color: var(--text-light);
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 16px;
  padding-left: 8px;
  border-left: 3px solid var(--primary);
}

.timeline-card {
  border: none;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  border-radius: 8px;
}

.t-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.t-title {
  font-weight: 600;
  font-size: 14px;
}

.t-desc {
  font-size: 12px;
  color: var(--text-secondary);
}

@media (max-width: 992px) {
  .bento-grid {
    grid-template-columns: 1fr;
    grid-template-areas:
      "stats"
      "main"
      "heatmap";
  }
}
</style>