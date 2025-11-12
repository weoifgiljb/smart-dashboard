<template>
  <div class="calendar-page">
    <!-- 打卡功能卡片 -->
    <el-card style="margin-bottom: 20px">
      <template #header>
        <div class="card-header">
          <h3>每日打卡</h3>
        </div>
      </template>
      <div class="checkin-content">
        <el-button 
          type="primary" 
          size="large" 
          :disabled="hasCheckedIn"
          @click="handleCheckIn"
          style="margin-right: 20px"
        >
          {{ hasCheckedIn ? '今日已打卡' : '立即打卡' }}
        </el-button>
        <el-row :gutter="20">
          <el-col :xs="8" :sm="6" :md="4">
            <el-statistic title="连续打卡" :value="consecutiveDays" />
          </el-col>
          <el-col :xs="8" :sm="6" :md="4">
            <el-statistic title="总打卡天数" :value="totalDays" />
          </el-col>
          <el-col :xs="8" :sm="6" :md="4">
            <el-statistic title="年度热力图" :value="yearHeatmapAvailable ? '已生成' : '加载中'" />
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 年度热力图卡片 -->
    <el-card style="margin-bottom: 20px">
      <template #header>
        <div class="card-header">
          <h3>年度打卡热力图</h3>
          <span style="font-size: 12px; color: #909399;">
            颜色深浅代表该天的热力值：基础分1 + 番茄×2 + 单词×1 + 任务×3
          </span>
        </div>
      </template>
      <BaseChart :option="calendarOption" height="240px" />
    </el-card>

    <!-- 日历卡片 -->
    <el-card>
      <template #header>
        <div class="toolbar">
          <div class="left">
            <el-button-group>
              <el-button size="small" @click="goPrevMonth">上个月</el-button>
              <el-button size="small" @click="goToday">今天</el-button>
              <el-button size="small" @click="goNextMonth">下个月</el-button>
            </el-button-group>
          </div>
          <div class="center">
            <span class="month-title">{{ currentYear }} 年 {{ currentMonth + 1 }} 月</span>
          </div>
          <div class="right">
            <el-select v-model="firstDayOfWeek" size="small" style="width: 140px">
              <el-option :value="1" label="周一开始" />
              <el-option :value="0" label="周日开始" />
            </el-select>
          </div>
        </div>
      </template>
      <div class="week-headers">
        <div v-for="w in weekLabels" :key="w" class="week-cell">{{ w }}</div>
      </div>
      <div class="day-grid">
        <div
          v-for="(cell, idx) in dayCells"
          :key="idx"
          class="day-cell"
          :class="{ 'is-other': !cell.inMonth, 'is-today': cell.key === todayKey }"
          @click="cell.inMonth && openDay(cell.key)"
        >
          <div class="date-num">{{ cell.date.getDate() }}</div>
          <div class="badges">
            <span v-if="stats[cell.key]?.checkin" class="badge checkin">打卡</span>
            <span v-if="stats[cell.key]?.pomodoro" class="badge pomodoro">{{ stats[cell.key].pomodoro }}</span>
            <span v-if="stats[cell.key]?.word" class="badge word">{{ stats[cell.key].word }}</span>
            <span v-if="stats[cell.key]?.task" class="badge task">{{ stats[cell.key].task }}</span>
          </div>
        </div>
      </div>
    </el-card>

    <el-drawer v-model="drawerOpen" :title="drawerTitle" size="40%">
      <div v-if="loadingDay" class="drawer-loading">
        <el-skeleton :rows="6" animated />
      </div>
      <div v-else>
        <template v-if="dayDetails">
          <el-descriptions title="概览" :column="2" size="small" border style="margin-bottom: 12px">
            <el-descriptions-item label="打卡">{{ (dayDetails.checkins || []).length }}</el-descriptions-item>
            <el-descriptions-item label="番茄">{{ (dayDetails.pomodoros || []).length }}</el-descriptions-item>
            <el-descriptions-item label="单词">{{ (dayDetails.words || []).length }}</el-descriptions-item>
            <el-descriptions-item label="任务">{{ (dayDetails.tasks || []).length }}</el-descriptions-item>
          </el-descriptions>

          <el-collapse>
            <el-collapse-item name="checkin" title="打卡">
              <el-empty v-if="!(dayDetails.checkins || []).length" description="无打卡" />
              <ul v-else class="list">
                <li v-for="c in dayDetails.checkins" :key="c.id">
                  ✅ 已打卡
                </li>
              </ul>
            </el-collapse-item>
            <el-collapse-item name="pomodoro" title="番茄钟">
              <el-empty v-if="!(dayDetails.pomodoros || []).length" description="无番茄钟" />
              <ul v-else class="list">
                <li v-for="p in dayDetails.pomodoros" :key="p.id">
                  <el-tag size="small" type="warning" style="margin-right: 6px">{{ p.type || 'work' }}</el-tag>
                  {{ formatTime(p.startTime) }} - {{ formatTime(p.endTime) }}（{{ p.duration || 0 }} 分钟）
                </li>
              </ul>
            </el-collapse-item>
            <el-collapse-item name="words" title="单词">
              <el-empty v-if="!(dayDetails.words || []).length" description="无单词活动" />
              <ul v-else class="list">
                <li v-for="w in dayDetails.words" :key="w.id">
                  <strong>{{ w.word }}</strong>
                  <el-tag v-if="w.status" size="small" style="margin-left: 6px">{{ w.status }}</el-tag>
                </li>
              </ul>
            </el-collapse-item>
            <el-collapse-item name="tasks" title="任务">
              <el-empty v-if="!(dayDetails.tasks || []).length" description="无任务" />
              <ul v-else class="list">
                <li v-for="t in dayDetails.tasks" :key="t.id">
                  <el-tag size="small" :type="taskStatusType(t.status)" style="margin-right: 6px">{{ t.status }}</el-tag>
                  <strong>{{ t.title }}</strong>
                  <el-tag size="small" :type="priorityType(t.priority)" style="margin-left: 6px">{{ t.priority }}</el-tag>
                </li>
              </ul>
            </el-collapse-item>
          </el-collapse>
        </template>
      </div>
    </el-drawer>
  </div>
  </template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCalendarData, getCalendarDay } from '@/api/calendar'
import { checkIn, getCheckInStats } from '@/api/checkin'
import BaseChart from '@/components/charts/BaseChart.vue'

type StatsMap = Record<string, { checkin?: number; pomodoro?: number; word?: number; task?: number }>

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
    ElMessage.success('打卡成功')
    hasCheckedIn.value = true
    await loadCheckInStats()
    await loadRange() // 刷新日历数据
  } catch (error) {
    ElMessage.error('打卡失败')
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

    // 并行请求：历史热力值 + 当年活动聚合（用于兜底计算）
    const year = new Date().getFullYear()
    const start = `${year}-01-01`
    const end = `${year}-12-31`
    const [historyResp, calendarAgg]: any[] = await Promise.all([
      getCheckInHistoryWithHeatValue(),
      getCalendarData({ start, end })
    ])
    const historyData = historyResp?.data || historyResp || []
    const agg = calendarAgg || {}

    // 找出最大热力值用于色阶映射（带兜底：若后端为0，则用聚合数据按新公式重算）
    let maxHeat = 10 // 最小色阶范围
    const data = (Array.isArray(historyData) ? historyData : []).map((c: any) => {
      const key = c.checkInDate
      const raw = Number(c.heatValue) || 0
      const day = agg?.[key] || {}
      const fallback = 1 + (Number(day.pomodoro) || 0) * 2 + (Number(day.word) || 0) * 1 + (Number(day.task) || 0) * 3
      const heatValue = raw > 0 ? raw : (fallback > 0 ? fallback : 0)
      maxHeat = Math.max(maxHeat, heatValue)
      return [key, heatValue]
    })

    calendarOption.value = {
      tooltip: {
        formatter: (p: any) => {
          const date = p.data?.[0]
          const heat = p.data?.[1]
          return heat ? `${date}：热力值 ${heat}` : `${date}：未打卡`
        }
      },
      visualMap: {
        show: true,
        min: 0,
        max: maxHeat,
        inRange: {
          // 使用渐进色系：白 -> 浅蓝 -> 中蓝 -> 深蓝 -> 靛蓝
          color: ['#f0f5ff', '#d4e9ff', '#85c4ff', '#409eff', '#0052cc']
        },
        textStyle: {
          color: '#666'
        }
      },
      calendar: {
        range: `${year}`,
        cellSize: ['auto', 18],
        splitLine: { show: false },
        itemStyle: { borderWidth: 0.5, borderColor: '#f0f0f0' },
        yearLabel: { show: false },
        monthLabel: { nameMap: 'cn' },
        dayLabel: { nameMap: 'cn' }
      },
      series: [
        {
          type: 'heatmap',
          coordinateSystem: 'calendar',
          data
        }
      ]
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
    stats.value = (data || {})
  } catch (e) {
    ElMessage.error('获取日历数据失败')
  }
}

const drawerOpen = ref(false)
const drawerTitle = ref('')
const loadingDay = ref(false)
const dayDetails = ref<any>(null)

async function openDay(key: string) {
  drawerTitle.value = key
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

function taskStatusType(status?: string) {
  const s = (status || '').toLowerCase()
  if (s === 'done') return 'success'
  if (s === 'blocked') return 'danger'
  if (s === 'in_progress') return 'warning'
  return 'info'
}
function priorityType(p?: string) {
  const x = (p || '').toLowerCase()
  if (x === 'urgent') return 'danger'
  if (x === 'high') return 'warning'
  if (x === 'med') return 'primary'
  return 'info'
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
  padding: 20px;
}

/* 打卡卡片样式 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.checkin-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 日历样式 */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
}

.month-title {
  font-weight: 600;
  font-size: 18px;
  color: #303133;
}

.week-headers {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  font-weight: 600;
  color: #606266;
  padding: 12px 0;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 10px;
}

.week-cell {
  padding: 8px 0;
  font-size: 14px;
}

.day-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 10px;
  margin-top: 10px;
}

.day-cell {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  min-height: 100px;
  padding: 10px;
  cursor: pointer;
  background: #fff;
  transition: all .2s ease;
}

.day-cell:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  transform: translateY(-2px);
}

.day-cell.is-other {
  opacity: 0.4;
  background: #fafafa;
}

.day-cell.is-today {
  border-color: #409eff;
  border-width: 2px;
  background: #ecf5ff;
}

.date-num {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.day-cell.is-other .date-num {
  color: #909399;
}

.day-cell.is-today .date-num {
  color: #409eff;
}

.badges {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 8px;
}

.badge {
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 12px;
  line-height: 1.2;
  color: #fff;
  font-weight: 500;
}

.badge.checkin { background: #67c23a; }
.badge.pomodoro { background: #e6a23c; }
.badge.word { background: #409eff; }
.badge.task { background: #909399; }

.list {
  margin: 0;
  padding-left: 16px;
}

.drawer-loading {
  padding: 12px;
}
</style>


