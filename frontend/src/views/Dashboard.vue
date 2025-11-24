<template>
  <div class="dashboard">
    <div class="filter-bar" style="margin-bottom: 20px; display: flex; justify-content: flex-end;">
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        :shortcuts="shortcuts"
        size="default"
      />
    </div>
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <div class="stat-value">{{ stats.checkInDays }}</div>
            <div class="stat-label">连续打卡天数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <div class="stat-value">{{ stats.wordCount }}</div>
            <div class="stat-label">已学单词数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <div class="stat-value">{{ stats.pomodoroCount }}</div>
            <div class="stat-label">完成番茄钟</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <div class="stat-value">{{ stats.totalDays }}</div>
            <div class="stat-label">总打卡天数</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="8">
        <el-card>
          <template #header>近30天热力值趋势</template>
          <BaseChart :option="heatValueOption" height="200px" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>近7天番茄完成数</template>
          <BaseChart :option="pomodoroOption" height="200px" @chart-click="(p) => handleChartClick('pomodoro', p)" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>近7天单词新增</template>
          <BaseChart :option="wordsOption" height="200px" @chart-click="(p) => handleChartClick('word', p)" />
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>今日任务</template>
          <div class="tasks-list">
            <div class="task-item" :class="{ completed: todayTasks.hasCheckedIn }">
              <el-icon><Calendar /></el-icon>
              <span>每日打卡</span>
              <el-tag v-if="todayTasks.hasCheckedIn" type="success" size="small">已完成</el-tag>
              <el-tag v-else type="info" size="small">未完成</el-tag>
            </div>
            <div class="task-item">
              <el-icon><Reading /></el-icon>
              <span>学习单词</span>
              <el-tag type="primary" size="small">{{ todayTasks.todayWordCount }} 个</el-tag>
            </div>
            <div class="task-item">
              <el-icon><Timer /></el-icon>
              <span>完成番茄钟</span>
              <el-tag type="warning" size="small">{{ todayTasks.todayPomodoroCount }} 个</el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>最近活动</template>
          <div class="activities-list" v-if="recentActivities.length > 0">
            <div v-for="(activity, index) in recentActivities" :key="index" class="activity-item">
              <div class="activity-icon" :class="activity.type">
                <el-icon v-if="activity.type === 'checkin'"><Calendar /></el-icon>
                <el-icon v-else-if="activity.type === 'pomodoro'"><Timer /></el-icon>
                <el-icon v-else><Reading /></el-icon>
              </div>
              <div class="activity-content">
                <div class="activity-title">{{ activity.title }}</div>
                <div class="activity-time">{{ formatActivityTime(activity.time) }}</div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无活动" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-table :data="dialogData" style="width: 100%" v-if="dialogType === 'pomodoro'">
        <el-table-column prop="startTime" label="开始时间" width="180">
          <template #default="{ row }">
            {{ row.startTime ? new Date(row.startTime).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="时长(分钟)" width="100">
          <template #default="{ row }">
            {{ Math.floor((row.duration || 0) / 60) }}
          </template>
        </el-table-column>
        <el-table-column prop="tag" label="标签" />
      </el-table>
      <el-table :data="dialogData" style="width: 100%" v-else>
        <el-table-column prop="word" label="单词" width="180" />
        <el-table-column prop="meaning" label="释义" />
        <el-table-column prop="createTime" label="添加时间" width="180">
          <template #default="{ row }">
            {{ row.createTime ? new Date(row.createTime).toLocaleString() : '-' }}
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Calendar, Reading, Timer } from '@element-plus/icons-vue'
import { getDashboardStats, getTodayTasks, getRecentActivities } from '@/api/dashboard'
import { getCheckInHistory, getCheckInHistoryWithHeatValue } from '@/api/checkin'
import { getCalendarData } from '@/api/calendar'
import { getPomodoroHistory } from '@/api/pomodoro'
import { getWords, getTodayWords } from '@/api/words'
import BaseChart from '@/components/charts/BaseChart.vue'

const stats = ref({
  checkInDays: 0,
  wordCount: 0,
  pomodoroCount: 0,
  totalDays: 0
})

const todayTasks = ref({
  hasCheckedIn: false,
  todayWordCount: 0,
  todayPomodoroCount: 0
})

const recentActivities = ref<any[]>([])

const heatValueOption = ref<any>({})
const pomodoroOption = ref<any>({})
const wordsOption = ref<any>({})

const dateRange = ref<[Date, Date] | null>(null)
const rawPomodoros = ref<any[]>([])
const rawWords = ref<any[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const dialogData = ref<any[]>([])
const dialogType = ref<'pomodoro' | 'word'>('pomodoro')

const shortcuts = [
  { text: '最近一周', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 3600 * 1000 * 24 * 7); return [start, end] } },
  { text: '最近一个月', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 3600 * 1000 * 24 * 30); return [start, end] } },
  { text: '最近三个月', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 3600 * 1000 * 24 * 90); return [start, end] } },
]

// 兼容后端时间戳（秒/毫秒）与字符串
const toDate = (val: any): Date | null => {
  if (!val) return null
  if (typeof val === 'number') {
    // 小于 10^12 视为秒
    const ms = val < 1_000_000_000_000 ? val * 1000 : val
    return new Date(ms)
  }
  const d = new Date(val)
  return isNaN(d.getTime()) ? null : d
}

onMounted(async () => {
  try {
    // 近30天范围
    const today = new Date()
    const start = new Date(today)
    start.setDate(today.getDate() - 29)
    const toKey = (d: Date) => {
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${y}-${m}-${day}`
    }
    const [statsData, tasksData, activitiesData, checkins, historyWithHeat, calAgg, pomodoros, words, todayWords]: any[] = await Promise.all([
      getDashboardStats(),
      getTodayTasks(),
      getRecentActivities(),
      getCheckInHistory({ page: 0, size: 400 }),
      getCheckInHistoryWithHeatValue(),
      getCalendarData({ start: toKey(start), end: toKey(today) }),
      getPomodoroHistory(),
      getWords(),
      getTodayWords()
    ])
    
    rawPomodoros.value = pomodoros || []
    rawWords.value = words || []

    stats.value = statsData
    todayTasks.value = tasksData
    // 兜底修正：以“今日需背接口”的数量为准，避免服务端旧版本导致统计失真
    try {
      if (Array.isArray(todayWords)) {
        todayTasks.value.todayWordCount = todayWords.length
      }
    } catch {}
    recentActivities.value = activitiesData
    buildHeatValueOption(historyWithHeat, calAgg)
    
    // 初始化日期范围为最近7天
    const rangeEnd = new Date()
    const rangeStart = new Date()
    rangeStart.setDate(rangeEnd.getDate() - 6)
    dateRange.value = [rangeStart, rangeEnd]
    
    // 初始构建图表
    updateCharts()

    // 定义口径为“当前词库总单词数”，与 Words 列表保持一致，避免把“掌握(done)”口径误当成已学
    try {
      const totalWords = Array.isArray(words) ? (words as any[]).length : 0
      stats.value.wordCount = totalWords
    } catch {}
    // 兜底修正：基于前端数据计算 streak、总天数、今日/总番茄
    try {
      // 连续打卡天数与总天数
      const dateSet = new Set<string>((checkins || []).map((c: any) => c?.checkInDate).filter(Boolean))
      // 找最近一次打卡日期作为锚点
      const today = new Date()
      const getKey = (d: Date) => getDateKey(d)
      let anchor: Date | null = null
      for (let i = 0; i < 365; i++) {
        const d = new Date(today)
        d.setDate(today.getDate() - i)
        if (dateSet.has(getKey(d))) {
          anchor = d
          break
        }
      }
      let streak = 0
      if (anchor) {
        let currentAnchor = anchor
        while (dateSet.has(getKey(currentAnchor))) {
          streak++
          const prev = new Date(currentAnchor)
          prev.setDate(currentAnchor.getDate() - 1)
          currentAnchor = prev
        }
      }
      if (!Number.isFinite(stats.value.checkInDays) || stats.value.checkInDays < streak) {
        stats.value.checkInDays = streak
      }
      const total = dateSet.size
      if (!Number.isFinite(stats.value.totalDays) || stats.value.totalDays < total) {
        stats.value.totalDays = total
      }
      // 番茄统计
      const totalPomodoros = Array.isArray(pomodoros) ? pomodoros.length : 0
      if (!Number.isFinite(stats.value.pomodoroCount) || stats.value.pomodoroCount < totalPomodoros) {
        stats.value.pomodoroCount = totalPomodoros
      }
      // 今日番茄数量（以 startTime 当天计）
      const todayKey = getDateKey(new Date())
      const todayPomodoros = (pomodoros || []).filter((p: any) => {
        const d = toDate(p?.startTime)
        return d ? getDateKey(d) === todayKey : false
      }).length
      if (!Number.isFinite(todayTasks.value.todayPomodoroCount) || todayTasks.value.todayPomodoroCount !== todayPomodoros) {
        todayTasks.value.todayPomodoroCount = todayPomodoros
      }
    } catch {}
  } catch (error) {
    console.error('获取数据失败', error)
  }
})

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
  const arr = []
  for(let dt=new Date(start); dt<=end; dt.setDate(dt.getDate()+1)){
      arr.push(new Date(dt));
  }
  return arr;
}

const updateCharts = () => {
  if (!dateRange.value) return
  const [start, end] = dateRange.value
  
  const filteredPomodoros = rawPomodoros.value.filter(p => {
    const d = toDate(p?.startTime)
    return d && d >= start && d <= end
  })
  
  const filteredWords = rawWords.value.filter(w => {
    const d = toDate(w?.createTime)
    return d && d >= start && d <= end
  })
  
  buildPomodoroOption(filteredPomodoros, start, end)
  buildWordsOption(filteredWords, start, end)
}

watch(dateRange, () => {
  updateCharts()
})

const handleChartClick = (type: 'pomodoro' | 'word', params: any) => {
  if (!dateRange.value) return
  const days = getDaysArray(dateRange.value[0], dateRange.value[1])
  const date = days[params.dataIndex]
  if (!date) return
  
  const key = getDateKey(date)
  
  if (type === 'pomodoro') {
    dialogData.value = rawPomodoros.value.filter(p => {
      const d = toDate(p?.startTime)
      return d && getDateKey(d) === key
    })
    dialogTitle.value = `${key} 番茄钟记录`
  } else {
    dialogData.value = rawWords.value.filter(w => {
      const d = toDate(w?.createTime)
      return d && getDateKey(d) === key
    })
    dialogTitle.value = `${key} 新增单词`
  }
  
  dialogType.value = type
  dialogVisible.value = true
}

const buildHeatValueOption = (historyWithHeat: any[], calAgg: Record<string, any>) => {
  const days = lastNDays(30)
  const map = new Map<string, number>()
  ;(historyWithHeat || []).forEach((h: any) => {
    const key = h?.checkInDate
    const val = Number(h?.heatValue) || 0
    if (key) map.set(key, Math.max(val, map.get(key) || 0))
  })
  const labels = days.map(d => `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`)
  const values = days.map(d => {
    const key = getDateKey(d)
    const v = map.get(key) || 0
    const day = (calAgg || {})[key] || {}
    const fallback = 1 + (Number(day.pomodoro) || 0) * 2 + (Number(day.word) || 0) * 1 + (Number(day.task) || 0) * 3
    return Math.max(v, fallback > 0 ? fallback : 0)
  })
  const maxVal = Math.max(10, ...values)
  heatValueOption.value = {
    grid: { left: 10, right: 10, top: 10, bottom: 10, containLabel: false },
    xAxis: { type: 'category', data: labels, axisTick: { show: false }, axisLine: { show: false }, axisLabel: { show: false } },
    yAxis: { type: 'value', min: 0, max: maxVal, splitLine: { show: false }, axisLabel: { show: false }, axisLine: { show: false }, axisTick: { show: false } },
    series: [
      {
        data: values,
        type: 'bar',
        barWidth: '60%',
        itemStyle: { 
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: '#10b981' },
              { offset: 1, color: 'rgba(16, 185, 129, 0.2)' }
            ]
          }
        }
      }
    ],
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const p = params?.[0]
        return `${p.axisValue}<br/>热力值：${p.data}`
      }
    }
  }
}

const buildPomodoroOption = (pomodoros: any[], start?: Date, end?: Date) => {
  const days = (start && end) ? getDaysArray(start, end) : lastNDays(7)
  const map = new Map<string, number>()
  days.forEach(d => map.set(getDateKey(d), 0))
  ;(pomodoros || []).forEach((p: any) => {
    const d = toDate(p?.startTime)
    if (!d) return
    const key = getDateKey(d)
    if (map.has(key)) {
      map.set(key, (map.get(key) || 0) + 1)
    }
  })
  const labels = days.map(d => `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`)
  const values = days.map(d => map.get(getDateKey(d)) || 0)
  pomodoroOption.value = {
    grid: { left: 10, right: 10, top: 10, bottom: 10, containLabel: false },
    xAxis: { type: 'category', data: labels, axisTick: { show: false }, axisLine: { show: false }, axisLabel: { show: false } },
    yAxis: { type: 'value', splitLine: { show: false }, axisLabel: { show: false }, axisLine: { show: false }, axisTick: { show: false } },
    series: [
      {
        data: values,
        type: 'line',
        smooth: true,
        areaStyle: { color: 'rgba(59, 130, 246, 0.15)' },
        lineStyle: { color: '#3b82f6' },
        itemStyle: { color: '#3b82f6' }
      }
    ],
    tooltip: { trigger: 'axis' }
  }
}

const buildWordsOption = (words: any[], start?: Date, end?: Date) => {
  const days = (start && end) ? getDaysArray(start, end) : lastNDays(7)
  const mapAdd = new Map<string, number>()
  days.forEach(d => mapAdd.set(getDateKey(d), 0))
  ;(words || []).forEach((w: any) => {
    const d = toDate(w?.createTime)
    if (d) {
      const key = getDateKey(d)
      if (mapAdd.has(key)) {
        mapAdd.set(key, (mapAdd.get(key) || 0) + 1)
      }
    }
  })
  const labels = days.map(d => `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`)
  const values = days.map(d => mapAdd.get(getDateKey(d)) || 0)
  wordsOption.value = {
    grid: { left: 10, right: 10, top: 10, bottom: 10, containLabel: false },
    xAxis: { type: 'category', data: labels, axisTick: { show: false }, axisLine: { show: false }, axisLabel: { show: false } },
    yAxis: { type: 'value', splitLine: { show: false }, axisLabel: { show: false }, axisLine: { show: false }, axisTick: { show: false } },
    series: [
      {
        data: values,
        type: 'bar',
        barWidth: '60%',
        itemStyle: { color: '#10b981' }
      }
    ],
    tooltip: { trigger: 'axis' }
  }
}
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: var(--primary);
  margin-bottom: 10px;
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
}

.tasks-list {
  padding: 10px 0;
}

.task-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.task-item:last-child {
  border-bottom: none;
}

.task-item .el-icon {
  margin-right: 10px;
  font-size: 18px;
  color: var(--secondary);
}

.task-item span {
  flex: 1;
  font-size: 14px;
  color: var(--app-text);
}

.task-item.completed {
  opacity: 0.8;
}

.activities-list {
  max-height: 400px;
  overflow-y: auto;
}

.activity-item {
  display: flex;
  align-items: flex-start;
  padding: 12px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  flex-shrink: 0;
}

.activity-icon.checkin {
  background-color: var(--success-light);
  color: var(--success);
}

.activity-icon.pomodoro {
  background-color: var(--warning-light);
  color: var(--warning);
}

.activity-icon.word {
  background-color: var(--info-light);
  color: var(--info);
}

.activity-content {
  flex: 1;
}

.activity-title {
  font-size: 14px;
  color: var(--app-text);
  margin-bottom: 4px;
}

.activity-time {
  font-size: 12px;
  color: var(--text-light);
}
</style>



