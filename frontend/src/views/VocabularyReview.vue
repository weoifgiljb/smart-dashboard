<template>
  <div class="vocabulary-review-page">
    <!-- 顶部状态栏 -->
    <div class="status-bar">
      <div class="left-actions">
        <el-button link class="back-btn" @click="backToWords">
          <el-icon><ArrowLeft /></el-icon> 退出
        </el-button>
        <span class="progress-indicator">{{ currentIndex + 1 }} / {{ queue.length }}</span>
      </div>
      
      <div class="right-actions">
        <div class="mode-switch">
          <span :class="{ active: reviewMode === 'recognize' }" @click="reviewMode = 'recognize'">识记</span>
          <span class="divider">|</span>
          <span :class="{ active: reviewMode === 'spell' }" @click="reviewMode = 'spell'">拼写</span>
        </div>
        <el-button circle text @click="toggleSettings">
          <el-icon><Setting /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 主体区域 -->
    <div class="review-container">
      <!-- 进度条 -->
      <div class="progress-line">
        <div class="progress-fill" :style="{ width: progressPercentage + '%' }"></div>
      </div>

      <!-- 卡片区域 -->
      <div v-if="currentWord" class="card-scene">
        <!-- 认识/不认识模式 -->
        <div v-if="reviewMode === 'recognize'" class="flip-card-wrapper" :class="{ flipped: isFlipped }" @click="!isFlipped && flipCard()">
          <div class="flip-card-inner">
            <!-- 正面 -->
            <div class="card-face card-front">
              <div class="card-body">
                <div class="word-primary">{{ currentWord.word }}</div>
                <div class="word-phonetic" v-if="currentWord.phonetic">/{{ currentWord.phonetic }}/</div>
                <el-button class="audio-play-btn" circle @click.stop="playAudio">
                  <el-icon><Headset /></el-icon>
                </el-button>
                <div class="tap-hint">点击翻转查看释义</div>
              </div>
            </div>
            
            <!-- 背面 -->
            <div class="card-face card-back">
              <div class="card-body">
                <div class="word-meaning">{{ currentWord.translation }}</div>
                <div class="word-example" v-if="currentWord.example">
                  <div class="ex-en">{{ currentWord.example }}</div>
                </div>
                <div class="action-buttons">
                  <button class="review-btn unknown" @click.stop="markAsUnknown">
                    <span class="icon">✕</span>
                    <span class="label">不认识</span>
                    <span class="sub">1分钟后</span>
                  </button>
                  <button class="review-btn vague" @click.stop="markAsKnown">
                    <span class="icon">?</span>
                    <span class="label">模糊</span>
                    <span class="sub">10分钟后</span>
                  </button>
                  <button class="review-btn known" @click.stop="markAsKnown">
                    <span class="icon">✓</span>
                    <span class="label">认识</span>
                    <span class="sub">1天后</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 拼写模式 -->
        <div v-else class="spell-card">
          <div class="spell-header">
            <div class="meaning-hint">{{ currentWord.translation }}</div>
          </div>
          
          <div class="spell-input-wrapper">
            <input 
              ref="inputRef"
              v-model="answer" 
              type="text" 
              class="spell-input" 
              placeholder="输入单词..." 
              :disabled="showResult"
              @keyup.enter="submitAnswer"
            />
            <div v-if="showResult" class="feedback-icon" :class="resultType">
              <el-icon v-if="resultType === 'correct'"><Check /></el-icon>
              <el-icon v-else><Close /></el-icon>
            </div>
          </div>

          <div v-if="showResult && resultType === 'wrong'" class="correct-answer">
            正确答案: <span>{{ currentWord.word }}</span>
          </div>

          <div class="spell-actions">
            <el-button v-if="!showResult" type="primary" round size="large" class="submit-btn" @click="submitAnswer">
              提交 (Enter)
            </el-button>
            <el-button v-else type="primary" round size="large" class="submit-btn" @click="nextWord">
              下一个 (Enter)
            </el-button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="queue.length === 0 && !summary.total" class="empty-state">
        <el-empty description="加载中或暂无单词..." />
      </div>

      <!-- 完成界面 -->
      <div v-else class="completion-card">
        <div class="success-icon">
          <el-icon><Trophy /></el-icon>
        </div>
        <h2>本次复习完成</h2>
        <div class="stats-grid">
          <div class="stat-item">
            <div class="val">{{ summary.correct }}</div>
            <div class="lbl">正确</div>
          </div>
          <div class="stat-item">
            <div class="val">{{ summary.wrong }}</div>
            <div class="lbl">错误</div>
          </div>
          <div class="stat-item">
            <div class="val">{{ accuracyPercentage }}%</div>
            <div class="lbl">准确率</div>
          </div>
        </div>
        <div class="completion-actions">
          <el-button type="primary" round size="large" @click="backToWords">返回列表</el-button>
          <el-button v-if="nextQueue.length" round size="large" @click="startNextRound">错题重练</el-button>
        </div>
      </div>
    </div>

    <!-- 设置抽屉 -->
    <el-drawer v-model="showSettings" title="复习设置" size="320px" :with-header="true">
      <div class="settings-list">
        <div class="s-item">
          <span>自动发音</span>
          <el-switch v-model="settings.autoPlayAudio" />
        </div>
        <div class="s-item">
          <span>显示例句</span>
          <el-switch v-model="settings.showExample" />
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Setting, Headset, Check, Close, Trophy } from '@element-plus/icons-vue'
import { getTodayWords, getWords, reviewWord } from '@/api/words'

type WordItem = {
  id: string
  word: string
  translation: string
  example?: string
  difficulty?: string
  image?: string
  phonetic?: string
}

const route = useRoute()
const router = useRouter()

// 状态
const reviewMode = ref<'recognize' | 'spell'>('recognize')
const queue = ref<WordItem[]>([])
const nextQueue = ref<WordItem[]>([])
const currentIndex = ref(0)
const isFlipped = ref(false)
const answer = ref('')
const showResult = ref(false)
const resultType = ref<'correct' | 'wrong'>('correct')
const inputRef = ref<HTMLInputElement>()
const showSettings = ref(false)

const settings = ref({
  autoPlayAudio: true,
  showExample: true
})

const summary = ref({
  total: 0,
  correct: 0,
  wrong: 0
})

const currentWord = computed(() => queue.value[currentIndex.value] || null)
const progressPercentage = computed(() => {
  if (queue.value.length === 0) return 0
  return ((currentIndex.value) / queue.value.length) * 100
})
const accuracyPercentage = computed(() => {
  const total = summary.value.correct + summary.value.wrong
  return total === 0 ? 0 : Math.round((summary.value.correct / total) * 100)
})

// Lifecycle
onMounted(() => {
  initQueue()
  window.addEventListener('keydown', handleGlobalKey)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleGlobalKey)
})

const initQueue = async () => {
  const idsParam = (route.query.ids as string) || ''
  const ids = idsParam ? decodeURIComponent(idsParam).split(',').filter(Boolean) : []
  
  let list: any[] = []
  try {
    if (ids.length) {
      // Fetch all to filter (simplified) or specific API
      const all = await getWords()
      list = (all as any[]).filter(w => ids.includes(w.id))
    } else {
      list = await getTodayWords()
    }
  } catch (e) {
    console.error(e)
  }

  queue.value = list.map(w => ({
    id: w.id,
    word: w.word,
    translation: w.translation,
    example: w.example,
    image: w.image,
    phonetic: w.phonetic || ''
  }))
  summary.value.total = queue.value.length
}

// Logic
const flipCard = () => {
  isFlipped.value = true
  if (settings.value.autoPlayAudio) playAudio()
}

const playAudio = () => {
  if (!currentWord.value) return
  const u = new SpeechSynthesisUtterance(currentWord.value.word)
  u.lang = 'en-US'
  window.speechSynthesis.speak(u)
}

const markAsKnown = async () => {
  if (!currentWord.value) return
  try {
    await reviewWord(currentWord.value.id)
    summary.value.correct++
    nextWord()
  } catch {
    ElMessage.error('网络错误')
  }
}

const markAsUnknown = () => {
  if (!currentWord.value) return
  summary.value.wrong++
  nextQueue.value.push(currentWord.value)
  nextWord()
}

const nextWord = () => {
  isFlipped.value = false
  showResult.value = false
  answer.value = ''
  currentIndex.value++
  
  if (!currentWord.value && nextQueue.value.length === 0) {
    // Finished
  } else if (!currentWord.value && nextQueue.value.length > 0) {
    // Start next round? Or just let user decide at end screen
  } else {
    // Next word ready
    if (reviewMode.value === 'spell') {
      nextTick(() => inputRef.value?.focus())
    }
  }
}

const submitAnswer = async () => {
  if (!currentWord.value) return
  const correct = currentWord.value.word.trim().toLowerCase()
  const input = answer.value.trim().toLowerCase()
  
  if (input === correct) {
    resultType.value = 'correct'
    showResult.value = true
    if (settings.value.autoPlayAudio) playAudio()
    await reviewWord(currentWord.value.id)
    summary.value.correct++
    setTimeout(nextWord, 1000)
  } else {
    resultType.value = 'wrong'
    showResult.value = true
    summary.value.wrong++
    nextQueue.value.push(currentWord.value)
  }
}

const startNextRound = () => {
  queue.value = [...nextQueue.value]
  nextQueue.value = []
  currentIndex.value = 0
  summary.value.correct = 0
  summary.value.wrong = 0
}

const backToWords = () => router.push('/words')
const toggleSettings = () => showSettings.value = !showSettings.value

const handleGlobalKey = (e: KeyboardEvent) => {
  if (reviewMode.value === 'recognize') {
    if (e.code === 'Space') {
      if (!isFlipped.value) flipCard()
    }
  }
}
</script>

<style scoped>
.vocabulary-review-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f8fafc;
}

/* 顶部栏 */
.status-bar {
  height: 60px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  background: white;
  border-bottom: 1px solid #e2e8f0;
}

.left-actions { display: flex; align-items: center; gap: 16px; }
.progress-indicator { font-size: 14px; font-weight: 600; color: #64748b; }

.mode-switch {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f1f5f9;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 13px;
  margin-right: 12px;
}
.mode-switch span { cursor: pointer; color: #94a3b8; transition: color 0.2s; }
.mode-switch span.active { color: #0f172a; font-weight: 600; }
.mode-switch .divider { color: #cbd5e1; cursor: default; }

/* 主体 */
.review-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  position: relative;
}

.progress-line {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: #e2e8f0;
}
.progress-fill {
  height: 100%;
  background: var(--primary);
  transition: width 0.3s ease;
}

/* 卡片场景 */
.card-scene {
  width: 100%;
  max-width: 480px;
  perspective: 1000px;
  height: 560px;
}

/* 翻转卡片 */
.flip-card-wrapper {
  width: 100%;
  height: 100%;
  cursor: pointer;
  position: relative;
}

.flip-card-inner {
  position: relative;
  width: 100%;
  height: 100%;
  text-align: center;
  transition: transform 0.6s cubic-bezier(0.4, 0, 0.2, 1);
  transform-style: preserve-3d;
}

.flip-card-wrapper.flipped .flip-card-inner {
  transform: rotateY(180deg);
}

.card-face {
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
  border-radius: 24px;
  box-shadow: 
    0 10px 15px -3px rgba(0, 0, 0, 0.1),
    0 4px 6px -2px rgba(0, 0, 0, 0.05),
    inset 0 0 0 1px rgba(255,255,255,0.1);
  background: white;
  overflow: hidden;
}

.card-front {
  background: linear-gradient(145deg, #ffffff, #f8fafc);
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-back {
  background: white;
  transform: rotateY(180deg);
  display: flex;
  flex-direction: column;
}

.card-body {
  padding: 40px;
  width: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
}

/* 正面元素 */
.word-primary {
  font-size: 48px;
  font-weight: 800;
  color: #1e293b;
  margin-bottom: 8px;
}
.word-phonetic {
  font-size: 18px;
  color: #64748b;
  font-family: monospace;
  margin-bottom: 24px;
}
.audio-play-btn {
  width: 48px;
  height: 48px;
  font-size: 20px;
}
.tap-hint {
  margin-top: auto;
  font-size: 13px;
  color: #94a3b8;
  animation: pulse 2s infinite;
}

/* 背面元素 */
.card-back .card-body { justify-content: center; }
.word-meaning {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 24px;
  text-align: center;
}
.word-example {
  background: #f1f5f9;
  padding: 16px;
  border-radius: 12px;
  width: 100%;
  margin-bottom: auto;
}
.ex-en {
  font-size: 16px;
  color: #334155;
  line-height: 1.5;
  text-align: center;
}

.action-buttons {
  display: flex;
  gap: 12px;
  width: 100%;
  margin-top: 24px;
}

.review-btn {
  flex: 1;
  border: none;
  border-radius: 16px;
  padding: 16px 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  transition: transform 0.1s;
}
.review-btn:active { transform: scale(0.95); }

.review-btn .icon { font-size: 20px; margin-bottom: 4px; font-weight: bold; }
.review-btn .label { font-size: 14px; font-weight: 600; margin-bottom: 2px; }
.review-btn .sub { font-size: 10px; opacity: 0.8; }

.review-btn.unknown { background: #fee2e2; color: #ef4444; }
.review-btn.vague { background: #fef3c7; color: #d97706; }
.review-btn.known { background: #dcfce7; color: #16a34a; }

/* 拼写卡片 */
.spell-card {
  background: white;
  border-radius: 24px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1);
  width: 100%;
  height: 100%;
  padding: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.spell-header { flex: 1; display: flex; align-items: center; }
.meaning-hint { font-size: 24px; font-weight: 600; color: #334155; text-align: center; }

.spell-input-wrapper {
  width: 100%;
  position: relative;
  margin-bottom: 40px;
}
.spell-input {
  width: 100%;
  font-size: 32px;
  text-align: center;
  border: none;
  border-bottom: 2px solid #e2e8f0;
  padding: 10px;
  outline: none;
  font-weight: 700;
  color: #0f172a;
  background: transparent;
}
.spell-input:focus { border-color: var(--primary); }

.feedback-icon {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  font-size: 24px;
}
.feedback-icon.correct { color: var(--success); }
.feedback-icon.wrong { color: var(--danger); }

.correct-answer {
  color: var(--danger);
  font-size: 16px;
  margin-bottom: 20px;
}
.correct-answer span { font-weight: bold; font-size: 20px; }

.submit-btn { width: 100%; }

/* 完成卡片 */
.completion-card {
  background: white;
  padding: 40px;
  border-radius: 24px;
  text-align: center;
  box-shadow: 0 10px 25px -5px rgba(0,0,0,0.1);
  width: 100%;
  max-width: 400px;
}
.success-icon {
  font-size: 64px;
  color: #fbbf24;
  margin-bottom: 16px;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin: 24px 0;
}
.stat-item {
  background: #f8fafc;
  padding: 12px;
  border-radius: 12px;
}
.stat-item .val { font-size: 20px; font-weight: 800; color: #0f172a; }
.stat-item .lbl { font-size: 12px; color: #64748b; }

.completion-actions { display: flex; gap: 12px; justify-content: center; }

/* 设置 */
.settings-list { padding: 20px; }
.s-item { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; font-size: 14px; }

@keyframes pulse { 50% { opacity: 0.5; } }
</style>