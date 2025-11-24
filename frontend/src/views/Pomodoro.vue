<template>
  <div class="pomodoro-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>番茄钟</h3>
        </div>
      </template>
      <div class="pomodoro-content">
        <div class="timer">
          <div class="progress-wrapper">
            <svg class="progress-svg" viewBox="0 0 120 120">
              <circle class="progress-bg" cx="60" cy="60" r="54" />
              <circle
                class="progress-bar"
                cx="60"
                cy="60"
                r="54"
                :stroke-dasharray="circumference"
                :stroke-dashoffset="dashOffset"
                :style="{ stroke: timerType === 'work' ? '#409eff' : '#67c23a' }"
              />
            </svg>
            <div class="time-display">{{ formatTime(timeLeft) }}</div>
          </div>
          <div class="timer-controls">
            <el-button type="primary" :disabled="isRunning" @click="startTimer">开始</el-button>
            <el-button :disabled="!isRunning" @click="pauseTimer">暂停</el-button>
            <el-button @click="resetTimer">重置</el-button>
          </div>
          <div class="timer-type">
            <el-radio-group v-model="timerType">
              <el-radio label="work">工作</el-radio>
              <el-radio label="break">休息</el-radio>
            </el-radio-group>
          </div>
          <div class="timer-config">
            <div class="config-row">
              <span class="config-label">工作时长(分钟)：</span>
              <el-input-number
                v-model="workMinutes"
                :min="1"
                :max="240"
                :step="1"
                :disabled="isRunning"
              />
            </div>
            <div class="config-row">
              <span class="config-label">休息时长(分钟)：</span>
              <el-input-number
                v-model="breakMinutes"
                :min="1"
                :max="120"
                :step="1"
                :disabled="isRunning"
              />
            </div>
          </div>
        </div>
        <div class="stats" style="margin-top: 30px">
          <el-statistic title="今日完成" :value="todayCount" />
          <el-statistic title="总完成数" :value="totalCount" />
        </div>
      </div>
    </el-card>
    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <h3>近7天工作/休息</h3>
        </div>
      </template>
      <BaseChart :option="weeklyOption" height="220px" />
    </el-card>
    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <h3>最近30次时长波动</h3>
        </div>
      </template>
      <BaseChart :option="fluctuationOption" height="240px" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { startPomodoro, getPomodoroStats, getPomodoroHistory } from '@/api/pomodoro'
import BaseChart from '@/components/charts/BaseChart.vue'

const workMinutes = ref(25)
const breakMinutes = ref(5)
const timeLeft = ref(workMinutes.value * 60) // 单位：秒
const isRunning = ref(false)
const timerType = ref('work')
let worker: Worker | null = null
const todayCount = ref(0)
const totalCount = ref(0)
const weeklyOption = ref<any>({})
const fluctuationOption = ref<any>({})

// 进度计算（基于当前模式总秒数与剩余秒数）
const totalSeconds = computed(() =>
  timerType.value === 'work' ? workMinutes.value * 60 : breakMinutes.value * 60,
)
const progress = computed(() => {
  if (totalSeconds.value <= 0) return 0
  return 1 - timeLeft.value / totalSeconds.value
})
const radius = 54
const circumference = 2 * Math.PI * radius
const dashOffset = computed(() => circumference * (1 - progress.value))

// 监听计时器类型变化，自动重置时间
watch(timerType, (newType) => {
  if (!isRunning.value) {
    timeLeft.value = newType === 'work' ? workMinutes.value * 60 : breakMinutes.value * 60
  }
})

// 自定义分钟变化时，若未在计时中，实时更新剩余时间
watch(workMinutes, (val) => {
  if (!isRunning.value && timerType.value === 'work') {
    timeLeft.value = val * 60
  }
})
watch(breakMinutes, (val) => {
  if (!isRunning.value && timerType.value === 'break') {
    timeLeft.value = val * 60
  }
})

onMounted(async () => {
  // 初始化 Worker
  worker = new Worker(new URL('@/workers/timerWorker.ts', import.meta.url), { type: 'module' })
  worker.onmessage = (e: MessageEvent) => {
    const { type, seconds } = e.data || {}
    if (type === 'tick' && typeof seconds === 'number') {
      timeLeft.value = seconds
    } else if (type === 'done') {
      finishTimer()
    }
  }
  await loadStats()
  await loadWeekly()
  await loadFluctuation()
})

onUnmounted(() => {
  if (worker) {
    worker.terminate()
    worker = null
  }
})

const loadStats = async () => {
  try {
    const stats: any = await getPomodoroStats()
    todayCount.value = stats.todayCount
    totalCount.value = stats.totalCount
  } catch (error) {
    console.error('获取番茄钟统计失败', error)
  }
}

const loadWeekly = async () => {
  try {
    const list: any[] = (await getPomodoroHistory()) as any
    const days: Date[] = []
    const today = new Date()
    for (let i = 6; i >= 0; i--) {
      const d = new Date(today)
      d.setDate(today.getDate() - i)
      days.push(d)
    }
    const key = (d: Date) =>
      `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    const labels = days.map(
      (d) => `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`,
    )
    const workMap = new Map<string, number>()
    const breakMap = new Map<string, number>()
    days.forEach((d) => {
      workMap.set(key(d), 0)
      breakMap.set(key(d), 0)
    })
    ;(list || []).forEach((p: any) => {
      if (!p.startTime) return
      const k = key(new Date(p.startTime))
      if (workMap.has(k) || breakMap.has(k)) {
        if (p.type === 'work') {
          workMap.set(k, (workMap.get(k) || 0) + 1)
        } else if (p.type === 'break') {
          breakMap.set(k, (breakMap.get(k) || 0) + 1)
        }
      }
    })
    const workVals = days.map((d) => workMap.get(key(d)) || 0)
    const breakVals = days.map((d) => breakMap.get(key(d)) || 0)
    weeklyOption.value = {
      grid: { left: 20, right: 10, top: 10, bottom: 20, containLabel: false },
      xAxis: {
        type: 'category',
        data: labels,
        axisTick: { show: false },
        axisLine: { show: false },
      },
      yAxis: {
        type: 'value',
        splitLine: { show: false },
        axisLine: { show: false },
        axisTick: { show: false },
      },
      legend: { data: ['工作', '休息'] },
      series: [
        {
          name: '工作',
          type: 'bar',
          stack: 'total',
          data: workVals,
          itemStyle: { color: '#409eff' },
          barWidth: '45%',
        },
        {
          name: '休息',
          type: 'bar',
          stack: 'total',
          data: breakVals,
          itemStyle: { color: '#67c23a' },
          barWidth: '45%',
        },
      ],
      tooltip: { trigger: 'axis' },
    }
  } catch (error) {
    console.error('获取番茄历史失败', error)
  }
}

const loadFluctuation = async () => {
  try {
    const list: any[] = (await getPomodoroHistory()) as any
    const latest = (list || []).slice(0, 30) // 后端按开始时间倒序
    const workPoints: any[] = []
    const breakPoints: any[] = []
    latest.forEach((p: any) => {
      if (!p.startTime || !p.duration) return
      const point = [p.startTime, p.duration]
      if (p.type === 'work') {
        workPoints.push(point)
      } else if (p.type === 'break') {
        breakPoints.push(point)
      }
    })
    fluctuationOption.value = {
      grid: { left: 40, right: 10, top: 20, bottom: 30 },
      xAxis: { type: 'time', axisLine: { show: false }, axisTick: { show: false } },
      yAxis: {
        type: 'value',
        name: '分钟',
        splitLine: { show: true },
        axisLine: { show: false },
        axisTick: { show: false },
      },
      legend: { data: ['工作', '休息'] },
      tooltip: {
        trigger: 'axis',
      },
      series: [
        {
          name: '工作',
          type: 'line',
          smooth: true,
          showSymbol: false,
          data: workPoints,
          itemStyle: { color: '#409eff' },
        },
        {
          name: '休息',
          type: 'line',
          smooth: true,
          showSymbol: false,
          data: breakPoints,
          itemStyle: { color: '#67c23a' },
        },
      ],
    }
  } catch (error) {
    console.error('获取时长波动失败', error)
  }
}

const formatTime = (seconds: number) => {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
}

const startTimer = () => {
  isRunning.value = true
  worker?.postMessage({ type: 'start', payload: { seconds: timeLeft.value } })
}

const pauseTimer = () => {
  isRunning.value = false
  worker?.postMessage({ type: 'pause' })
}

const resetTimer = () => {
  pauseTimer()
  timeLeft.value = timerType.value === 'work' ? workMinutes.value * 60 : breakMinutes.value * 60
  worker?.postMessage({ type: 'reset', payload: { seconds: timeLeft.value } })
}

const finishTimer = async () => {
  pauseTimer()
  try {
    await startPomodoro({
      duration: timerType.value === 'work' ? workMinutes.value : breakMinutes.value,
      type: timerType.value,
    })
    ElMessage.success('番茄钟完成！')
    await loadStats()
    await loadWeekly()
    await loadFluctuation()
    resetTimer()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '保存失败')
  }
}
</script>

<style scoped>
.pomodoro-page {
  padding: 20px;
}

.card-header h3 {
  margin: 0;
}

.pomodoro-content {
  text-align: center;
}

.timer {
  padding: 40px 0;
}

.progress-wrapper {
  position: relative;
  width: 260px;
  height: 260px;
  margin: 0 auto 20px;
  display: inline-block;
}

.progress-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.progress-bg {
  fill: none;
  stroke: #ebeef5;
  stroke-width: 10;
}

.progress-bar {
  fill: none;
  stroke-width: 10;
  stroke-linecap: round;
  transition: stroke-dashoffset 1s linear;
}

.time-display {
  font-size: 72px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 30px;
}

.progress-wrapper .time-display {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0;
}

.timer-controls {
  margin-bottom: 20px;
}

.timer-controls .el-button {
  margin: 0 10px;
}

.timer-config {
  margin-top: 12px;
}
.config-row {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 8px;
}
.config-label {
  margin-right: 10px;
}

.stats {
  display: flex;
  justify-content: space-around;
}
</style>
