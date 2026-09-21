<template>
  <div class="pomodoro-page">
    <section class="timer-card" :class="{ 'is-running': isRunning }">
      <div class="card-header">
        <h3>番茄专注</h3>
        <div class="mode-switch">
          <button
            type="button"
            class="mode-btn"
            :class="{ active: timerType === 'work' }"
            @click="timerType = 'work'"
          >
            专注
          </button>
          <button
            type="button"
            class="mode-btn"
            :class="{ active: timerType === 'break' }"
            @click="timerType = 'break'"
          >
            休息
          </button>
        </div>
      </div>

      <div class="progress-container">
        <svg class="progress-svg" viewBox="0 0 288 288">
          <circle class="progress-bg" cx="144" cy="144" r="136" />
          <circle
            class="progress-bar"
            cx="144"
            cy="144"
            r="136"
            :stroke-dasharray="circumference"
            :stroke-dashoffset="dashOffset"
          />
        </svg>
        <div class="time-display">{{ formatTime(timeLeft) }}</div>
      </div>

      <div class="timer-controls">
        <button type="button" class="ctrl-btn" aria-label="重置" @click="resetTimer">
          <el-icon :size="22"><Refresh /></el-icon>
        </button>
        <button
          type="button"
          class="ctrl-btn play"
          aria-label="开始"
          :disabled="isRunning"
          @click="startTimer"
        >
          <el-icon :size="32"><VideoPlay /></el-icon>
        </button>
        <button
          type="button"
          class="ctrl-btn"
          aria-label="暂停"
          :disabled="!isRunning"
          @click="pauseTimer"
        >
          <el-icon :size="22"><VideoPause /></el-icon>
        </button>
      </div>

      <div v-if="!isRunning" class="timer-config">
        <div class="config-item">
          <span class="label">专注时长</span>
          <el-input-number v-model="workMinutes" :min="1" :max="120" controls-position="right" />
        </div>
        <div class="config-item">
          <span class="label">休息时长</span>
          <el-input-number v-model="breakMinutes" :min="1" :max="60" controls-position="right" />
        </div>
        <div v-if="timerType === 'work'" class="config-item config-item-wide">
          <span class="label">绑定任务（可选）</span>
          <el-select v-model="selectedTaskId" clearable filterable placeholder="不绑定，自由专注">
            <el-option
              v-for="task in openTasks"
              :key="task.id"
              :label="taskLabel(task)"
              :value="task.id"
            />
          </el-select>
        </div>
      </div>

      <div class="stats-block">
        <div class="stats-caption">今日成就</div>
        <div class="stats-row">
          <div class="stat-box">
            <div class="stat-num">{{ todayCount }}</div>
            <div class="stat-desc">今日完成</div>
          </div>
          <div class="stat-box">
            <div class="stat-num">{{ totalCount }}</div>
            <div class="stat-desc">累计专注</div>
          </div>
        </div>
      </div>
    </section>

    <div class="charts-row">
      <el-card shadow="never" class="chart-card">
        <template #header>
          <div class="chart-header">
            <span>近7天分布</span>
            <span class="chart-tag">分析</span>
          </div>
        </template>
        <BaseChart :option="weeklyOption" height="192px" />
      </el-card>
      <el-card shadow="never" class="chart-card">
        <template #header>
          <div class="chart-header">
            <span>时长波动</span>
          </div>
        </template>
        <BaseChart :option="fluctuationOption" height="192px" />
      </el-card>
    </div>

    <el-dialog
      v-model="reviewVisible"
      title="专注后的 2 分钟微复习"
      width="420px"
      align-center
      :close-on-click-modal="false"
    >
      <div v-if="currentReviewWord" class="micro-review">
        <p class="micro-progress">{{ reviewIndex + 1 }} / {{ reviewQueue.length }}</p>
        <button type="button" class="micro-card" @click="reviewFlipped = true">
          <strong>{{ currentReviewWord.word }}</strong>
          <span v-if="reviewFlipped">{{ currentReviewWord.translation || '暂无释义' }}</span>
          <span v-else class="micro-hint">点击看释义</span>
        </button>
        <div class="micro-actions">
          <el-button @click="skipReviewWord">跳过</el-button>
          <el-button type="primary" :disabled="!reviewFlipped" @click="markReviewKnown">
            认识了
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { VideoPlay, VideoPause, Refresh } from '@element-plus/icons-vue'
import { startPomodoro, getPomodoroStats, getPomodoroHistory } from '@/api/pomodoro'
import { listTasks } from '@/api/tasks'
import { getTodayWords, reviewWord, WordReviewResult } from '@/api/words'
import type { Task } from '@/types/task'
import BaseChart from '@/components/charts/BaseChart.vue'
import { chartPalette } from '@/utils/themeTokens'

type ReviewWord = { id: string; word: string; translation?: string }

const route = useRoute()
const workMinutes = ref(25)
const breakMinutes = ref(5)
const timeLeft = ref(workMinutes.value * 60) // 单位：秒
const isRunning = ref(false)
const timerType = ref<'work' | 'break'>('work')
let worker: Worker | null = null
const todayCount = ref(0)
const totalCount = ref(0)
const weeklyOption = ref<any>({})
const fluctuationOption = ref<any>({})
const selectedTaskId = ref('')
const openTasks = ref<Task[]>([])
const reviewVisible = ref(false)
const reviewQueue = ref<ReviewWord[]>([])
const reviewIndex = ref(0)
const reviewFlipped = ref(false)

const currentReviewWord = computed(() => reviewQueue.value[reviewIndex.value] || null)

// 进度计算（基于当前模式总秒数与剩余秒数）
const totalSeconds = computed(() =>
  timerType.value === 'work' ? workMinutes.value * 60 : breakMinutes.value * 60,
)
const progress = computed(() => {
  if (totalSeconds.value <= 0) return 0
  return 1 - timeLeft.value / totalSeconds.value
})
const radius = 136
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
  applyTaskQuery(route.query.taskId)
  await loadOpenTasks()
})

onUnmounted(() => {
  if (worker) {
    worker.terminate()
    worker = null
  }
})

watch(
  () => route.query.taskId,
  async (taskId) => {
    applyTaskQuery(taskId)
    await loadOpenTasks()
  },
)

const applyTaskQuery = (taskId: unknown) => {
  const id = Array.isArray(taskId) ? taskId[0] : taskId
  if (typeof id === 'string' && id) {
    selectedTaskId.value = id
  }
}

const taskLabel = (task: Task) => {
  const minutes = task.actualMinutes ? ` · 已专注 ${task.actualMinutes} 分钟` : ''
  return `${task.title}${minutes}`
}

const loadOpenTasks = async () => {
  try {
    const list = (await listTasks({})) as Task[]
    openTasks.value = (list || []).filter((task): task is Task & { id: string } => {
      const status = String(task.status || '').toLowerCase()
      return (status === 'in_progress' || status === 'todo') && Boolean(task.id)
    })
    if (selectedTaskId.value && !openTasks.value.some((task) => task.id === selectedTaskId.value)) {
      openTasks.value = [{ id: selectedTaskId.value, title: '当前任务' }, ...openTasks.value]
    }
  } catch (error) {
    console.error('获取可绑定任务失败', error)
  }
}

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
    const palette = chartPalette()
    weeklyOption.value = {
      grid: { left: 12, right: 12, top: 16, bottom: 28, containLabel: true },
      xAxis: {
        type: 'category',
        data: labels,
        axisTick: { show: false },
        axisLine: { show: false },
        axisLabel: { show: true, fontSize: 10, color: palette.text },
      },
      yAxis: {
        type: 'value',
        splitLine: { show: false },
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { show: false },
      },
      legend: {
        data: ['工作', '休息'],
        bottom: 0,
        textStyle: { color: palette.text, fontSize: 11 },
        itemWidth: 10,
        itemHeight: 8,
      },
      series: [
        {
          name: '工作',
          type: 'bar',
          stack: 'total',
          data: workVals,
          itemStyle: { color: palette.primary, borderRadius: [6, 6, 0, 0] },
          barWidth: '42%',
        },
        {
          name: '休息',
          type: 'bar',
          stack: 'total',
          data: breakVals,
          itemStyle: { color: palette.primary, opacity: 0.35 },
          barWidth: '42%',
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
    const palette = chartPalette()
    fluctuationOption.value = {
      grid: { left: 12, right: 12, top: 16, bottom: 28, containLabel: true },
      xAxis: {
        type: 'time',
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { show: false },
      },
      yAxis: {
        type: 'value',
        splitLine: { show: true, lineStyle: { type: 'dashed', color: palette.border } },
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { show: false },
      },
      legend: {
        data: ['工作', '休息'],
        bottom: 0,
        textStyle: { color: palette.text, fontSize: 11 },
        itemWidth: 10,
        itemHeight: 8,
      },
      tooltip: { trigger: 'axis' },
      series: [
        {
          name: '工作',
          type: 'line',
          smooth: true,
          showSymbol: false,
          data: workPoints,
          itemStyle: { color: palette.primary },
          lineStyle: { color: palette.primary, width: 2 },
          areaStyle: { color: palette.primary, opacity: 0.08 },
        },
        {
          name: '休息',
          type: 'line',
          smooth: true,
          showSymbol: false,
          data: breakPoints,
          itemStyle: { color: palette.secondary },
          lineStyle: { color: palette.secondary, width: 2 },
          areaStyle: { color: palette.secondary, opacity: 0.08 },
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
  const finishedType = timerType.value
  try {
    await startPomodoro({
      duration: finishedType === 'work' ? workMinutes.value : breakMinutes.value,
      type: finishedType,
      ...(finishedType === 'work' && selectedTaskId.value ? { taskId: selectedTaskId.value } : {}),
    })
    ElMessage.success(
      finishedType === 'work' && selectedTaskId.value ? '专注完成，任务耗时已更新' : '番茄钟完成！',
    )
    await loadStats()
    await loadWeekly()
    await loadFluctuation()
    await loadOpenTasks()
    resetTimer()
    if (finishedType === 'work') {
      await openMicroReview()
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '保存失败')
  }
}

const openMicroReview = async () => {
  try {
    const list = (await getTodayWords()) as ReviewWord[]
    reviewQueue.value = (list || []).filter((word) => word?.id && word?.word).slice(0, 3)
    if (!reviewQueue.value.length) {
      return
    }
    reviewIndex.value = 0
    reviewFlipped.value = false
    reviewVisible.value = true
  } catch {
    // 到期词拉取失败时不打断主流程
  }
}

const skipReviewWord = () => {
  advanceReview()
}

const markReviewKnown = async () => {
  const word = currentReviewWord.value
  if (!word) return
  try {
    await reviewWord(word.id, WordReviewResult.Known)
    advanceReview()
  } catch {
    ElMessage.error('复习保存失败')
  }
}

const advanceReview = () => {
  reviewFlipped.value = false
  if (reviewIndex.value + 1 >= reviewQueue.value.length) {
    reviewVisible.value = false
    ElMessage.success('微复习完成，今天又接上了一小段。')
    return
  }
  reviewIndex.value += 1
}
</script>

<style scoped lang="less">
.pomodoro-page {
  width: 100%;
  max-width: 800px;
  min-width: 0;
  margin: 0 auto;
  container-type: inline-size;
}

.timer-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  min-width: 0;
  padding: 32px;
  background: var(--color-bg-elevated);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.card-header {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 40px;
}

.card-header h3 {
  margin: 0;
  flex: 0 0 auto;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text);
  white-space: nowrap;
}

.mode-switch {
  display: flex;
  flex: 0 0 auto;
  padding: 4px;
  background: var(--color-bg-muted);
  border-radius: 12px;
}

.mode-btn {
  border: 0;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 14px;
  font-weight: 500;
  padding: 6px 24px;
  border-radius: 8px;
  cursor: pointer;
  white-space: nowrap;
}

.mode-btn.active {
  background: var(--color-bg-elevated);
  color: var(--color-primary);
  font-weight: 600;
  box-shadow: var(--shadow-sm);
}

.progress-container {
  position: relative;
  width: min(288px, 100%);
  aspect-ratio: 1;
  height: auto;
  margin-bottom: 32px;
}

.progress-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.progress-bg {
  fill: none;
  stroke: var(--color-bg-muted);
  stroke-width: 8;
}

.progress-bar {
  fill: none;
  stroke: var(--color-primary);
  stroke-width: 8;
  stroke-linecap: round;
  transition: stroke-dashoffset 1s linear;
}

.time-display {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: clamp(32px, 18cqi, 60px);
  font-weight: 700;
  letter-spacing: -2px;
  color: var(--color-text);
  font-variant-numeric: tabular-nums;
}

.timer-controls {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 32px;
  margin-bottom: 40px;
}

.ctrl-btn {
  width: 56px;
  height: 56px;
  border: 0;
  border-radius: 50%;
  background: var(--color-bg-muted);
  color: var(--color-text-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.ctrl-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.ctrl-btn.play {
  width: 80px;
  height: 80px;
  background: var(--color-primary);
  color: #fff;
  box-shadow: var(--shadow-sm);
}

.ctrl-btn.play:disabled {
  opacity: 0.55;
}

.timer-config {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px;
  width: 100%;
  max-width: 380px;
  min-width: 0;
  padding: 16px 24px;
  background: color-mix(in srgb, var(--color-bg-muted) 70%, transparent);
  border-radius: var(--radius-md);
}

.config-item-wide {
  grid-column: 1 / -1;
}

.config-item-wide :deep(.el-select) {
  width: 100%;
}

.config-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.config-item .label {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.config-item :deep(.el-input-number) {
  width: 100%;
}

.config-item :deep(.el-input-number .el-input__wrapper) {
  background: var(--color-bg-elevated);
  border-radius: var(--radius-sm);
  box-shadow: 0 0 0 1px var(--color-border) inset;
}

.stats-block {
  width: 100%;
  margin-top: 40px;
  padding-top: 24px;
  border-top: 1px solid var(--color-bg-muted);
}

.stats-caption {
  text-align: center;
  margin-bottom: 24px;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--color-text-muted);
}

.stats-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
}

.stat-box {
  text-align: center;
}

.stat-box:first-child {
  border-right: 1px solid var(--color-bg-muted);
}

.stat-num {
  font-size: 30px;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1.2;
}

.stat-desc {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-top: 24px;
  min-width: 0;
}

.chart-card {
  border: none;
  background: var(--color-bg-elevated);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 700;
  color: var(--color-text);
}

.chart-tag {
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 6px;
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.micro-review {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 16px;
}

.micro-progress {
  margin: 0;
  text-align: center;
  font-size: 13px;
  color: var(--color-text-muted);
}

.micro-card {
  min-height: 140px;
  border: 0;
  border-radius: var(--radius-md);
  background: var(--color-bg-muted);
  color: var(--color-text);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  cursor: pointer;
}

.micro-card strong {
  font-size: 28px;
}

.micro-hint {
  font-size: 13px;
  color: var(--color-text-muted);
}

.micro-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 720px) {
  .timer-card {
    padding: 24px 16px;
  }

  .progress-container {
    width: min(240px, 100%);
  }

  .time-display {
    font-size: 48px;
  }

  .charts-row {
    grid-template-columns: 1fr;
  }
}

@container (max-width: 560px) {
  .timer-card {
    padding: 20px 12px;
  }

  .card-header {
    flex-wrap: wrap;
    margin-bottom: 24px;
  }

  .mode-btn {
    padding: 6px 14px;
  }

  .progress-container {
    width: min(220px, 100%);
    margin-bottom: 20px;
  }

  .time-display {
    font-size: clamp(32px, 12cqi, 48px);
    letter-spacing: 0;
  }

  .timer-controls {
    gap: 16px;
    margin-bottom: 24px;
  }

  .ctrl-btn {
    width: 44px;
    height: 44px;
  }

  .ctrl-btn.play {
    width: 64px;
    height: 64px;
  }

  .timer-config {
    grid-template-columns: 1fr;
    gap: 16px;
    padding: 12px;
  }

  .charts-row {
    grid-template-columns: 1fr;
  }
}
</style>
