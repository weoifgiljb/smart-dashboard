<template>
  <div class="vocabulary-review-page">
    <!-- 顶部导航栏 -->
    <div class="top-bar">
      <div class="top-left">
        <el-button link @click="backToWords">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <div class="mode-selector">
          <el-radio-group v-model="reviewMode" size="default">
            <el-radio-button label="recognize">认识/不认识</el-radio-button>
            <el-radio-button label="spell">拼写模式</el-radio-button>
          </el-radio-group>
        </div>
      </div>
      <div class="top-right">
        <div v-if="reviewMode === 'recognize' && currentWord" class="countdown-badge" :class="countdownClass">
          {{ countdown }}
        </div>
        <el-tooltip content="快捷键：Space-翻转 / ←-不认识 / →-认识 / Enter-提交">
          <el-button link>
            <el-icon><QuestionFilled /></el-icon>
          </el-button>
        </el-tooltip>
        <el-button link @click="toggleSettings">
          <el-icon><Setting /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 进度条 -->
    <div class="progress-section">
      <div class="progress-info">
        <span class="progress-text">第 {{ roundIndex }} 轮</span>
        <span class="progress-count">{{ currentIndex + 1 }} / {{ queue.length }}</span>
        <span class="remaining-text">剩余 {{ totalRemaining }} 个</span>
      </div>
      <el-progress
        :percentage="progressPercentage"
        :stroke-width="8"
        :color="progressColors"
        :show-text="false"
      />
      <div class="stats-mini">
        <span class="stat-item correct">✓ {{ summary.correct }}</span>
        <span class="stat-item wrong">✗ {{ summary.wrong }}</span>
        <span class="stat-item timeout">⏱ {{ summary.timeout }}</span>
      </div>
    </div>

    <!-- 主卡片区域 -->
    <div class="card-container" v-if="currentWord">
      <!-- 认识/不认识模式 -->
      <div v-if="reviewMode === 'recognize'" class="word-card" :class="{ flipped: isFlipped }">
        <div class="card-inner">
          <!-- 正面 - 中文 -->
          <div class="card-front">
            <div class="card-content">
              <div class="word-main">
                <h1 class="word-translation">{{ currentWord.translation || '（无翻译）' }}</h1>
                <el-tag v-if="currentWord.difficulty" :type="difficultyType(currentWord.difficulty)">
                  {{ difficultyText(currentWord.difficulty) }}
                </el-tag>
              </div>
              <div class="card-hint">
                <el-icon><View /></el-icon>
                <span>点击卡片或按空格键查看答案</span>
              </div>
            </div>
          </div>

          <!-- 背面 - 英文 -->
          <div class="card-back">
            <div class="card-content">
              <div class="word-main">
                <h1 class="word-text">{{ currentWord.word }}</h1>
                <el-button link @click="playAudio" class="audio-btn">
                  <el-icon><Headset /></el-icon>
                </el-button>
              </div>
              <p class="word-translation-small">{{ currentWord.translation || '（无翻译）' }}</p>
              <p v-if="currentWord.example" class="word-example">
                <el-icon><Document /></el-icon>
                {{ currentWord.example }}
              </p>
              <div class="card-actions">
                <el-button type="danger" size="large" @click="markAsUnknown">
                  <el-icon><Close /></el-icon>
                  不认识 (←)
                </el-button>
                <el-button type="success" size="large" @click="markAsKnown">
                  <el-icon><Check /></el-icon>
                  认识 (→)
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 拼写模式 -->
      <div v-else-if="reviewMode === 'spell'" class="word-card spell-mode">
        <div class="card-content">
          <div class="spell-header">
            <div class="countdown-circle" :class="countdownClass">
              <svg viewBox="0 0 100 100">
                <circle cx="50" cy="50" r="45" class="circle-bg" />
                <circle
                  cx="50" cy="50" r="45"
                  class="circle-progress"
                  :style="{ strokeDashoffset: circleOffset }"
                />
              </svg>
              <span class="countdown-text">{{ countdown }}</span>
            </div>
          </div>

          <div class="word-main">
            <h2 class="word-translation">{{ currentWord.translation || '（无翻译）' }}</h2>
            <el-tag v-if="currentWord.difficulty" :type="difficultyType(currentWord.difficulty)">
              {{ difficultyText(currentWord.difficulty) }}
            </el-tag>
          </div>

          <div class="spell-input-area">
            <el-input
              v-model="answer"
              placeholder="请输入英文单词..."
              size="large"
              clearable
              @keyup.enter="submitAnswer"
              :disabled="submitting || showResult"
              ref="inputRef"
              class="spell-input"
            >
              <template #prefix>
                <el-icon><Edit /></el-icon>
              </template>
            </el-input>

            <div v-if="showResult" class="result-feedback" :class="resultType">
              <el-icon v-if="resultType === 'correct'"><CircleCheck /></el-icon>
              <el-icon v-else><CircleClose /></el-icon>
              <span v-if="resultType === 'correct'">正确！</span>
              <span v-else>正确答案：{{ currentWord.word }}</span>
            </div>
          </div>

          <div class="spell-actions">
            <el-button
              v-if="!showResult"
              type="primary"
              size="large"
              @click="submitAnswer"
              :loading="submitting"
            >
              提交答案 (Enter)
            </el-button>
            <el-button
              v-else
              type="primary"
              size="large"
              @click="nextWord"
            >
              下一个 (Enter)
            </el-button>
            <el-button size="large" @click="showHint">
              <el-icon><View /></el-icon>
              提示
            </el-button>
          </div>

          <p v-if="currentWord.example && showExampleHint" class="word-example">
            <el-icon><Document /></el-icon>
            {{ currentWord.example }}
          </p>
        </div>
      </div>

      <!-- 卡片点击翻转 -->
      <div
        v-if="reviewMode === 'recognize' && !isFlipped"
        class="card-overlay"
        @click="flipCard"
      ></div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="queue.length === 0" class="empty-state">
      <div class="empty-content">
        <div class="empty-icon">
          <el-icon><Document /></el-icon>
        </div>
        <h2>暂无单词</h2>
        <p>请先添加单词或选择要复习的单词</p>
        <el-button type="primary" size="large" @click="backToWords">
          返回单词列表
        </el-button>
      </div>
    </div>

    <!-- 完成界面 -->
    <div v-else class="completion-screen">
      <div class="completion-content">
        <div class="completion-icon">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <h1>本轮复习完成！</h1>
        <div class="completion-stats">
          <div class="stat-card">
            <div class="stat-value">{{ summary.total }}</div>
            <div class="stat-label">总计</div>
          </div>
          <div class="stat-card correct">
            <div class="stat-value">{{ summary.correct }}</div>
            <div class="stat-label">正确</div>
          </div>
          <div class="stat-card wrong">
            <div class="stat-value">{{ summary.wrong }}</div>
            <div class="stat-label">错误</div>
          </div>
          <div class="stat-card timeout">
            <div class="stat-value">{{ summary.timeout }}</div>
            <div class="stat-label">超时</div>
          </div>
        </div>
        <div class="completion-accuracy">
          <el-progress
            type="circle"
            :percentage="accuracyPercentage"
            :width="120"
            :stroke-width="10"
          >
            <template #default="{ percentage }">
              <span class="percentage-value">{{ percentage }}%</span>
              <span class="percentage-label">准确率</span>
            </template>
          </el-progress>
        </div>
        <div class="completion-actions">
          <el-button type="primary" size="large" @click="backToWords">
            返回单词列表
          </el-button>
          <el-button v-if="nextQueue.length > 0" size="large" @click="startNextRound">
            继续下一轮 ({{ nextQueue.length }} 个)
          </el-button>
        </div>
      </div>
    </div>

    <!-- 设置抽屉 -->
    <el-drawer v-model="showSettings" title="复习设置" size="400px">
      <div class="settings-content">
        <div class="setting-item">
          <label>倒计时时长（秒）</label>
          <el-slider v-model="settings.countdownTime" :min="10" :max="120" :step="10" show-stops />
        </div>
        <div class="setting-item">
          <label>自动播放发音</label>
          <el-switch v-model="settings.autoPlayAudio" />
        </div>
        <div class="setting-item">
          <label>显示例句</label>
          <el-switch v-model="settings.showExample" />
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  QuestionFilled,
  Setting,
  View,
  Headset,
  Document,
  Close,
  Check,
  Edit,
  CircleCheck,
  CircleClose
} from '@element-plus/icons-vue'
import { getTodayWords, getWords, reviewWord, importDefaultWords } from '@/api/words'

type WordItem = {
  id: string
  word: string
  translation: string
  example?: string
  difficulty?: string
  image?: string
}

const route = useRoute()
const router = useRouter()

// 复习模式
const reviewMode = ref<'recognize' | 'spell'>('recognize')

// 数据
const queue = ref<WordItem[]>([])
const nextQueue = ref<WordItem[]>([])
const currentIndex = ref(0)
const roundIndex = ref(1)
const isFlipped = ref(false)
const answer = ref('')
const submitting = ref(false)
const showResult = ref(false)
const resultType = ref<'correct' | 'wrong'>('correct')
const showExampleHint = ref(false)
const inputRef = ref()
const hasAutoImported = ref(false)

// 倒计时
const countdown = ref(60)
const timer = ref<number | null>(null)

// 统计
const summary = ref({
  total: 0,
  correct: 0,
  wrong: 0,
  timeout: 0
})

// 设置
const showSettings = ref(false)
const settings = ref({
  countdownTime: 60,
  autoPlayAudio: false,
  showExample: true
})

// 计算属性
const currentWord = computed(() => queue.value[currentIndex.value] || null)
const totalRemaining = computed(() => queue.value.length - currentIndex.value + nextQueue.value.length)
const progressPercentage = computed(() => {
  if (queue.value.length === 0) return 0
  const completed = currentIndex.value
  return Math.round((completed / queue.value.length) * 100)
})

const accuracyPercentage = computed(() => {
  const total = summary.value.correct + summary.value.wrong + summary.value.timeout
  if (total === 0) return 0
  return Math.round((summary.value.correct / total) * 100)
})

const progressColors = [
  { color: '#f56c6c', percentage: 30 },
  { color: '#e6a23c', percentage: 60 },
  { color: '#67c23a', percentage: 100 }
]

const countdownClass = computed(() => {
  if (countdown.value <= 10) return 'danger'
  if (countdown.value <= 30) return 'warn'
  return 'safe'
})

const circleOffset = computed(() => {
  const circumference = 2 * Math.PI * 45
  const progress = countdown.value / settings.value.countdownTime
  return circumference * (1 - progress)
})

// 方法
const difficultyType = (difficulty: string) => {
  const map: any = { easy: 'success', medium: 'warning', hard: 'danger' }
  return map[difficulty] || 'info'
}

const difficultyText = (difficulty: string) => {
  const map: any = { easy: '简单', medium: '中等', hard: '困难' }
  return map[difficulty] || difficulty
}

const flipCard = () => {
  isFlipped.value = !isFlipped.value
  if (isFlipped.value && settings.value.autoPlayAudio) {
    playAudio()
  }
}

const playAudio = () => {
  if (!currentWord.value) return
  const utterance = new SpeechSynthesisUtterance(currentWord.value.word)
  utterance.lang = 'en-US'
  utterance.rate = 0.8
  window.speechSynthesis.speak(utterance)
}

const startTimer = () => {
  stopTimer()
  countdown.value = settings.value.countdownTime
  timer.value = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      handleTimeout()
    }
  }, 1000)
}

const stopTimer = () => {
  if (timer.value) {
    clearInterval(timer.value)
    timer.value = null
  }
}

const handleTimeout = () => {
  stopTimer()
  summary.value.timeout += 1
  if (currentWord.value) {
    nextQueue.value.push(currentWord.value)
  }
  ElMessage.warning('超时！')
  goNext()
}

const markAsKnown = async () => {
  if (!currentWord.value || submitting.value) return
  submitting.value = true
  try {
    await reviewWord(currentWord.value.id)
    // 通知其它页面（如 Words.vue）刷新统计与图表
    try {
      window.dispatchEvent(new CustomEvent('word-reviewed', { detail: { id: currentWord.value.id, at: Date.now() } }))
    } catch {}
    summary.value.correct += 1
    ElMessage.success('已标记为认识')
    stopTimer()
    goNext()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const markAsUnknown = () => {
  if (!currentWord.value) return
  summary.value.wrong += 1
  nextQueue.value.push(currentWord.value)
  ElMessage.info('已加入下一轮复习')
  stopTimer()
  goNext()
}

const submitAnswer = async () => {
  if (!currentWord.value || submitting.value || showResult.value) return

  const userInput = answer.value.trim().toLowerCase()
  const correct = currentWord.value.word.trim().toLowerCase()

  submitting.value = true

  try {
    if (userInput === correct) {
      await reviewWord(currentWord.value.id)
      try {
        window.dispatchEvent(new CustomEvent('word-reviewed', { detail: { id: currentWord.value.id, at: Date.now() } }))
      } catch {}
      summary.value.correct += 1
      resultType.value = 'correct'
      showResult.value = true
      ElMessage.success('正确！')
      stopTimer()

      // 自动进入下一个
      setTimeout(() => {
        nextWord()
      }, 1500)
    } else {
      summary.value.wrong += 1
      resultType.value = 'wrong'
      showResult.value = true
      nextQueue.value.push(currentWord.value)
      stopTimer()
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

const showHint = () => {
  if (!currentWord.value) return
  showExampleHint.value = true
  ElMessage.info(`首字母提示：${currentWord.value.word.charAt(0).toUpperCase()}`)
}

const nextWord = () => {
  showResult.value = false
  showExampleHint.value = false
  goNext()
}

const goNext = () => {
  answer.value = ''
  isFlipped.value = false
  currentIndex.value += 1

  if (currentIndex.value >= queue.value.length) {
    // 一轮结束
    if (nextQueue.value.length > 0) {
      // 进入下一轮
      ElMessage.info(`第 ${roundIndex.value} 轮完成，开始第 ${roundIndex.value + 1} 轮`)
      queue.value = nextQueue.value.slice()
      nextQueue.value = []
      currentIndex.value = 0
      roundIndex.value += 1

      startTimer()
      if (reviewMode.value === 'spell') {
        nextTick(() => {
          inputRef.value?.focus()
        })
      }
    } else {
      // 全部完成
      stopTimer()
    }
  } else {
    startTimer()
    if (reviewMode.value === 'spell') {
      nextTick(() => {
        inputRef.value?.focus()
      })
    }
  }
}

const startNextRound = () => {
  queue.value = nextQueue.value.slice()
  nextQueue.value = []
  currentIndex.value = 0
  roundIndex.value += 1
  summary.value = {
    total: queue.value.length,
    correct: 0,
    wrong: 0,
    timeout: 0
  }

  startTimer()
}

const backToWords = () => {
  router.push('/words')
}

const toggleSettings = () => {
  showSettings.value = !showSettings.value
}

const initQueue = async () => {
  const idsParam = (route.query.ids as string) || ''
  const ids = idsParam ? decodeURIComponent(idsParam).split(',').filter(Boolean) : []

  let base: any[] = []
  try {
    // 如果有指定ids，直接获取所有单词
    if (ids.length > 0) {
      base = await getWords()
    } else {
      // 没有指定ids，优先获取今日需背
      base = await getTodayWords()
      if (!base || base.length === 0) {
        base = await getWords()
      }
    }
  } catch (error) {
    console.error('获取单词失败:', error)
    try {
      base = await getWords()
    } catch (e) {
      console.error('获取所有单词也失败:', e)
      base = []
    }
  }

  const mapped: WordItem[] = (base as any[]).map(w => ({
    id: w.id,
    word: w.word,
    translation: w.translation,
    example: w.example,
    difficulty: w.difficulty,
    image: w.image
  }))

  if (ids.length) {
    queue.value = mapped.filter(w => ids.includes(w.id))
    console.log('过滤后的单词:', queue.value.length, '个，原始:', mapped.length, '个')
  } else {
    queue.value = mapped
  }

  nextQueue.value = []
  currentIndex.value = 0
  roundIndex.value = 1
  summary.value = {
    total: queue.value.length,
    correct: 0,
    wrong: 0,
    timeout: 0
  }

  console.log('=== 初始化完成 ===')
  console.log('队列长度:', queue.value.length)
  console.log('当前索引:', currentIndex.value)
  console.log('队列数据:', queue.value)
  console.log('当前单词:', currentWord.value)
  console.log('第一个单词:', queue.value[0])

  if (queue.value.length === 0) {
    try {
      if (!hasAutoImported.value) {
        await ElMessageBox.confirm(
          '当前没有可复习的单词，是否一键导入默认词库并开始？',
          '没有单词',
          {
            confirmButtonText: '导入并开始',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        await importDefaultWords()
        hasAutoImported.value = true
        ElMessage.success('默认词库已导入，正在加载...')
        await initQueue()
        return
      }
    } catch {
      ElMessage.info('已取消导入默认词库')
    }
    ElMessage.warning('没有可复习的单词，请先添加单词或选择要复习的单词')
  } else {
    ElMessage.success(`加载了 ${queue.value.length} 个单词`)
    startTimer()
    if (reviewMode.value === 'spell') {
      nextTick(() => {
        inputRef.value?.focus()
      })
    }
  }
}

// 键盘快捷键
const handleKeyPress = (e: KeyboardEvent) => {
  if (!currentWord.value) return

  if (reviewMode.value === 'recognize') {
    if (e.code === 'Space' && !isFlipped.value) {
      e.preventDefault()
      flipCard()
    } else if (e.code === 'ArrowLeft' && isFlipped.value) {
      e.preventDefault()
      markAsUnknown()
    } else if (e.code === 'ArrowRight' && isFlipped.value) {
      e.preventDefault()
      markAsKnown()
    }
  } else if (reviewMode.value === 'spell') {
    if (e.code === 'Enter' && showResult.value) {
      e.preventDefault()
      nextWord()
    }
  }
}

// 监听模式切换
watch(reviewMode, (newMode) => {
  stopTimer()
  isFlipped.value = false
  answer.value = ''
  showResult.value = false

  if (currentWord.value) {
    startTimer()
    if (newMode === 'spell') {
      nextTick(() => {
        inputRef.value?.focus()
      })
    }
  }
})

onMounted(() => {
  initQueue()
  window.addEventListener('keydown', handleKeyPress)
})

onBeforeUnmount(() => {
  stopTimer()
  window.removeEventListener('keydown', handleKeyPress)
})

watch(() => route.fullPath, () => {
  stopTimer()
  initQueue()
})




</script>

<style scoped>
.vocabulary-review-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  box-sizing: border-box;
}

/* 顶部导航栏 */
.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.top-left,
.top-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.mode-selector {
  margin-left: 20px;
}

.mode-selector :deep(.el-radio-button__inner) {
  padding: 8px 20px;
  font-weight: 500;
}

/* 进度区域 */
.progress-section {
  margin-bottom: 30px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 14px;
  color: #606266;
}

.progress-text {
  font-weight: 600;
  color: #409eff;
}

.progress-count {
  font-weight: 600;
  font-size: 16px;
  color: #303133;
}

.remaining-text {
  color: #909399;
}

.stats-mini {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 12px;
}

.stat-item {
  font-size: 14px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 6px;
  background: #f5f7fa;
}

.stat-item.correct {
  color: #67c23a;
  background: #f0f9eb;
}

.stat-item.wrong {
  color: #f56c6c;
  background: #fef0f0;
}

.stat-item.timeout {
  color: #e6a23c;
  background: #fdf6ec;
}

/* 卡片容器 */
.card-container {
  max-width: 800px;
  margin: 0 auto;
  perspective: 1000px;
}

/* 认识/不认识模式 - 翻转卡片 */
.word-card {
  position: relative;
  width: 100%;
  height: 500px;
}

.card-inner {
  position: relative;
  width: 100%;
  height: 100%;
  transition: transform 0.6s;
  transform-style: preserve-3d;
}

.word-card.flipped .card-inner {
  transform: rotateY(180deg);
}

.card-front,
.card-back {
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  overflow: hidden;
}

.card-front {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.card-back {
  background: #fff;
  transform: rotateY(180deg);
}

.card-content {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
  padding: 40px;
  box-sizing: border-box;
}

.word-main {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.word-translation {
  font-size: 48px;
  font-weight: 700;
  color: #fff;
  margin: 0;
  text-align: center;
}

.card-back .word-text {
  font-size: 56px;
  font-weight: 700;
  color: #303133;
  margin: 0;
  text-align: center;
}

.audio-btn {
  font-size: 24px;
  color: #409eff;
}

.word-translation-small {
  font-size: 24px;
  color: #909399;
  margin: 0 0 20px 0;
}

.word-example {
  font-size: 16px;
  color: #606266;
  line-height: 1.6;
  text-align: center;
  max-width: 600px;
  margin: 20px 0;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.card-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  margin-top: auto;
}

.card-actions {
  display: flex;
  gap: 16px;
  margin-top: 32px;
}

.card-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  cursor: pointer;
  z-index: 10;
}

/* 拼写模式 */
.spell-mode {
  height: auto;
  min-height: 500px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  transform: none !important;
}

.spell-header {
  display: flex;
  justify-content: center;
  margin-bottom: 32px;
}

.countdown-circle {
  position: relative;
  width: 100px;
  height: 100px;
}

.countdown-circle svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.circle-bg {
  fill: none;
  stroke: #e4e7ed;
  stroke-width: 8;
}

.circle-progress {
  fill: none;
  stroke-width: 8;
  stroke-linecap: round;
  transition: stroke-dashoffset 1s linear;
  stroke-dasharray: 283;
}

.countdown-circle.safe .circle-progress {
  stroke: #67c23a;
}

.countdown-circle.warn .circle-progress {
  stroke: #e6a23c;
}

.countdown-circle.danger .circle-progress {
  stroke: #f56c6c;
  animation: pulse-stroke 1s infinite;
}

@keyframes pulse-stroke {
  0%, 100% { stroke-width: 8; }
  50% { stroke-width: 10; }
}

.countdown-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 32px;
  font-weight: 700;
}

.countdown-circle.safe .countdown-text {
  color: #67c23a;
}

.countdown-circle.warn .countdown-text {
  color: #e6a23c;
}

.countdown-circle.danger .countdown-text {
  color: #f56c6c;
}

.spell-mode .word-translation {
  font-size: 36px;
  color: #303133;
}

.spell-input-area {
  width: 100%;
  max-width: 500px;
  margin: 24px 0;
}

.spell-input {
  font-size: 18px;
}

.spell-input :deep(.el-input__inner) {
  font-size: 20px;
  text-align: center;
  padding: 16px;
}

.result-feedback {
  margin-top: 16px;
  padding: 12px 20px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.result-feedback.correct {
  background: #f0f9eb;
  color: #67c23a;
  border: 2px solid #67c23a;
}

.result-feedback.wrong {
  background: #fef0f0;
  color: #f56c6c;
  border: 2px solid #f56c6c;
}

.spell-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

/* 空状态 */
.empty-state {
  max-width: 600px;
  margin: 60px auto;
  padding: 80px 40px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  text-align: center;
}

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

.empty-icon {
  font-size: 80px;
  color: #909399;
}

.empty-content h2 {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.empty-content p {
  font-size: 16px;
  color: #909399;
  margin: 0;
}

/* 完成界面 */
.completion-screen {
  max-width: 800px;
  margin: 0 auto;
  padding: 60px 40px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  text-align: center;
}

.completion-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32px;
}

.completion-icon {
  font-size: 80px;
  color: #67c23a;
}

.completion-content h1 {
  font-size: 32px;
  font-weight: 700;
  color: #303133;
  margin: 0;
}

.completion-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  width: 100%;
  max-width: 600px;
}

.stat-card {
  padding: 24px;
  background: #f5f7fa;
  border-radius: 12px;
  border: 2px solid #e4e7ed;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-card.correct {
  background: #f0f9eb;
  border-color: #67c23a;
}

.stat-card.wrong {
  background: #fef0f0;
  border-color: #f56c6c;
}

.stat-card.timeout {
  background: #fdf6ec;
  border-color: #e6a23c;
}

.stat-value {
  font-size: 36px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 8px;
}

.stat-card.correct .stat-value {
  color: #67c23a;
}

.stat-card.wrong .stat-value {
  color: #f56c6c;
}

.stat-card.timeout .stat-value {
  color: #e6a23c;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.completion-accuracy {
  margin: 20px 0;
}

.percentage-value {
  display: block;
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.percentage-label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.completion-actions {
  display: flex;
  gap: 16px;
}

/* 设置抽屉 */
.settings-content {
  padding: 20px 0;
}

.setting-item {
  margin-bottom: 32px;
}

.setting-item label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

/* 响应式 */
@media (max-width: 768px) {
  .vocabulary-review-page {
    padding: 10px;
  }

  .top-bar {
    flex-direction: column;
    gap: 12px;
  }

  .mode-selector {
    margin-left: 0;
  }

  .word-card {
    height: 400px;
  }

  .card-front .word-translation {
    font-size: 36px;
  }

  .card-back .word-text {
    font-size: 42px;
  }

  .completion-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .stat-value {
    font-size: 28px;
  }
}

/* 认识模式顶部倒计时徽标 */
.countdown-badge {
  min-width: 64px;
  text-align: center;
  font-size: 18px;
  font-weight: 700;
  padding: 6px 12px;
  border-radius: 999px;
  line-height: 1;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  border: 1px solid transparent;
}
.countdown-badge.safe {
  color: #67c23a;
  background: #f0f9eb;
  border-color: #e1f3d8;
}
.countdown-badge.warn {
  color: #e6a23c;
  background: #fdf6ec;
  border-color: #faecd8;
}
.countdown-badge.danger {
  color: #f56c6c;
  background: #fef0f0;
  border-color: #fde2e2;
  animation: pulse 1s infinite;
}
</style>

