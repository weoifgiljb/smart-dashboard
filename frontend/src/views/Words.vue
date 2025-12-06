<template>
  <div class="words-page">
    <div class="header-section">
      <div class="page-title">
        <h2>单词本</h2>
        <p class="subtitle">积累词汇，点亮思维</p>
      </div>
      <div class="header-actions">
        <el-button v-if="showDefaultBtn" type="success" plain :loading="defaultLoading" @click="handleImportDefault">
          一键导入默认词库
        </el-button>
        <el-button @click="showImportDialog = true">
          <el-icon class="el-icon--left"><Download /></el-icon>导入
        </el-button>
        <el-button type="primary" @click="showAddDialog = true">
          <el-icon class="el-icon--left"><Plus /></el-icon>添加单词
        </el-button>
      </div>
    </div>

    <el-row :gutter="24">
      <el-col :span="16">
        <el-card class="list-card">
          <el-tabs v-model="activeTab" class="custom-tabs">
            <el-tab-pane name="today">
              <template #label>
                <span class="tab-label">
                  <el-icon><Calendar /></el-icon> 今日需背
                  <el-badge :value="todayWords.length" class="tab-badge" type="primary" />
                </span>
              </template>
              
              <div class="tab-toolbar">
                <el-button type="primary" size="large" class="start-review-btn" :disabled="todayWords.length === 0" @click="startNewReview">
                  <el-icon class="el-icon--left"><VideoPlay /></el-icon>
                  开始复习 ({{ todayWords.length }})
                </el-button>
              </div>

              <div class="word-list">
                <el-empty v-if="todayWords.length === 0" description="今日任务已完成！" :image-size="100" />
                <div v-else class="list-container">
                  <div v-for="word in pagedTodayWords" :key="word.id" class="word-item">
                    <div class="word-info">
                      <div class="word-text">{{ word.word }}</div>
                      <div class="word-trans">{{ word.translation }}</div>
                    </div>
                    <div class="word-meta">
                      <el-tag size="small" effect="plain" type="info">第{{ nextStage(word) }}阶段</el-tag>
                      <span class="next-time">{{ nextIntervalText(word) }}</span>
                    </div>
                    <div class="word-actions">
                      <el-button circle size="small" @click="handleReview(word)"><el-icon><VideoPlay /></el-icon></el-button>
                      <el-button circle size="small" type="success" plain @click="handleDone(word.id)"><el-icon><Check /></el-icon></el-button>
                    </div>
                  </div>
                </div>
                <div class="pagination-wrapper">
                  <el-pagination
                    v-model:current-page="currentPageToday"
                    :page-size="pageSize"
                    layout="prev, pager, next"
                    :total="todayWords.length"
                    hide-on-single-page
                  />
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane name="all">
              <template #label>
                <span class="tab-label">
                  <el-icon><Collection /></el-icon> 全部单词
                  <el-badge :value="words.length" class="tab-badge" type="info" />
                </span>
              </template>
              
              <div class="word-list">
                <div class="list-container">
                  <div v-for="word in pagedWords" :key="word.id" class="word-item">
                    <div class="word-img-wrapper" @click="handleGenWordImage(word)">
                      <img v-if="word.image" :src="word.image" loading="lazy" />
                      <div v-else class="img-placeholder"><el-icon><Picture /></el-icon></div>
                    </div>
                    <div class="word-info">
                      <div class="word-text">{{ word.word }}</div>
                      <div class="word-trans">{{ word.translation }}</div>
                    </div>
                    <div class="word-actions">
                      <el-button link type="primary" @click="handleReview(word)">复习</el-button>
                      <el-popconfirm title="确定删除吗？" @confirm="handleDelete(word.id)">
                        <template #reference>
                          <el-button link type="danger">删除</el-button>
                        </template>
                      </el-popconfirm>
                    </div>
                  </div>
                </div>
                <div class="pagination-wrapper">
                  <el-pagination
                    v-model:current-page="currentPageAll"
                    :page-size="pageSize"
                    layout="prev, pager, next"
                    :total="words.length"
                    hide-on-single-page
                  />
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>学习趋势</span>
              <el-tag size="small" effect="plain">近7天</el-tag>
            </div>
          </template>
          <BaseChart :option="trendOption" height="240px" />
        </el-card>

        <el-card class="books-card" style="margin-top: 20px">
          <template #header>
            <div class="card-header">
              <span>我的词库</span>
            </div>
          </template>
          <div class="books-list">
            <div v-for="(book, idx) in books" :key="idx" class="book-item">
              <el-icon class="book-icon"><Notebook /></el-icon>
              <div class="book-info">
                <div class="book-name">{{ book.name || '默认词库' }}</div>
                <div class="book-count">{{ book.count || 0 }} 词</div>
              </div>
            </div>
            <el-empty v-if="books.length === 0" description="暂无词库" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Dialogs -->
    <el-dialog v-model="showAddDialog" title="添加单词" width="480px" align-center class="custom-dialog">
      <el-form :model="wordForm" label-position="top">
        <el-form-item label="单词">
          <el-input v-model="wordForm.word" placeholder="输入英文单词" size="large" />
        </el-form-item>
        <el-form-item label="翻译">
          <el-input v-model="wordForm.translation" placeholder="中文含义" />
        </el-form-item>
        <el-form-item label="例句">
          <el-input v-model="wordForm.example" type="textarea" :rows="3" placeholder="辅助记忆的例句" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAdd">添加</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showImportDialog" title="导入词库" width="520px" align-center class="custom-dialog">
      <el-form :model="importForm" label-position="top">
        <el-form-item label="词库链接 (GitHub Raw URL)">
          <el-input v-model="importForm.sourceUrl" placeholder="https://raw.githubusercontent.com/..." />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="词库名称">
              <el-input v-model="importForm.bookName" placeholder="例如：CET-4" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="每日分区大小">
              <el-input-number v-model="importForm.sectionSize" :min="10" :step="10" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="开始学习日期">
          <el-date-picker v-model="importForm.startDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <div class="dialog-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>支持格式：每行一个 "单词" 或 "单词|翻译"</span>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="showImportDialog = false">取消</el-button>
        <el-button type="primary" :loading="importLoading" @click="handleImport">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Plus, Download, VideoPlay, Calendar, Collection, Picture, Check, 
  Notebook, InfoFilled 
} from '@element-plus/icons-vue'
import {
  getWords,
  addWord,
  deleteWord,
  getTodayWords,
  importWords,
  updateWordStatus,
  getBooks,
  importDefaultWords,
} from '@/api/words'
import BaseChart from '@/components/charts/BaseChart.vue'
import { generateWordImage } from '@/api/ai'
import { getState as getSm2State } from '@/utils/sm2'

const router = useRouter()
const words = ref([])
const todayWords = ref([])
const books = ref<any[]>([])
const showAddDialog = ref(false)
const showImportDialog = ref(false)
const defaultLoading = ref(false)
const wordForm = ref({
  word: '',
  translation: '',
  example: '',
})
const importForm = ref({
  sourceUrl: '',
  bookName: '',
  sectionSize: 50,
  startDate: '',
})
const importLoading = ref(false)
const trendOption = ref<any>({})
const activeTab = ref('today')
const showDefaultBtn = ref(false)

// 分页
const pageSize = 10
const currentPageToday = ref(1)
const currentPageAll = ref(1)
const pagedTodayWords = computed(() => {
  const start = (currentPageToday.value - 1) * pageSize
  return (todayWords.value as any[]).slice(start, start + pageSize)
})
const pagedWords = computed(() => {
  const start = (currentPageAll.value - 1) * pageSize
  return (words.value as any[]).slice(start, start + pageSize)
})

onMounted(async () => {
  await loadWords()
  await Promise.all([loadTodayWords(), loadBooksSummary()])
  showDefaultBtn.value = (words.value as any[]).length === 0
})

const handleReviewedEvent = async () => {
  try {
    await loadWords()
    await loadTodayWords()
  } catch {
    // ignore
  }
}
onMounted(() => {
  window.addEventListener('word-reviewed', handleReviewedEvent as any)
})
onBeforeUnmount(() => {
  window.removeEventListener('word-reviewed', handleReviewedEvent as any)
})

const loadWords = async () => {
  try {
    const data: any = await getWords()
    words.value = data
    currentPageAll.value = 1
    buildTrendOption()
  } catch (error) {
    console.error('获取单词列表失败', error)
  }
}

const loadTodayWords = async () => {
  try {
    const data: any = await getTodayWords()
    todayWords.value = data
    currentPageToday.value = 1
  } catch (error) {
    console.error('获取今日需背失败', error)
  }
}

const loadBooksSummary = async () => {
  try {
    const data: any = await getBooks()
    books.value = data
  } catch (error) {
    console.error('获取词库概览失败', error)
  }
}

const toDate = (val: any): Date | null => {
  if (!val) return null
  if (typeof val === 'number') {
    const ms = val < 1_000_000_000_000 ? val * 1000 : val
    return new Date(ms)
  }
  const d = new Date(val)
  return isNaN(d.getTime()) ? null : d
}

const handleImportDefault = async () => {
  defaultLoading.value = true
  try {
    await importDefaultWords()
    ElMessage.success('默认词库导入成功')
    await Promise.all([loadWords(), loadTodayWords(), loadBooksSummary()])
    showDefaultBtn.value = false
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '导入失败')
  } finally {
    defaultLoading.value = false
  }
}

const handleAdd = async () => {
  try {
    await addWord(wordForm.value)
    ElMessage.success('添加成功')
    showAddDialog.value = false
    wordForm.value = { word: '', translation: '', example: '' }
    await Promise.all([loadWords(), loadTodayWords(), loadBooksSummary()])
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '添加失败')
  }
}

const handleDelete = async (id: string) => {
  try {
    await deleteWord(id)
    ElMessage.success('删除成功')
    await Promise.all([loadWords(), loadTodayWords(), loadBooksSummary()])
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '删除失败')
  }
}

const handleReview = (word: any) => {
  const ids = encodeURIComponent(word.id)
  router.push(`/vocabulary/review?ids=${ids}`)
}

const handleGenWordImage = async (row: any) => {
  if (row.image) return // 已有图不重复生成，除非加个强制按钮
  try {
    const updated: any = await generateWordImage(row.id)
    const updateLocal = (list: any[]) => {
      const idx = list.findIndex((w: any) => w.id === row.id)
      if (idx >= 0) list[idx] = { ...list[idx], ...updated }
    }
    updateLocal(words.value as any[])
    updateLocal(todayWords.value as any[])
    ElMessage.success('配图已生成')
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '生成配图失败')
  }
}

const handleDone = async (id: string) => {
  try {
    await updateWordStatus(id, 'done')
    ElMessage.success('标记完成')
    await Promise.all([loadWords(), loadTodayWords(), loadBooksSummary()])
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

const handleImport = async () => {
  if (!importForm.value.sourceUrl || !importForm.value.sectionSize) {
    ElMessage.warning('请填写链接与分区大小')
    return
  }
  importLoading.value = true
  try {
    await importWords(importForm.value)
    ElMessage.success('导入成功')
    showImportDialog.value = false
    await Promise.all([loadWords(), loadTodayWords(), loadBooksSummary()])
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '导入失败')
  } finally {
    importLoading.value = false
  }
}

const buildTrendOption = () => {
  const data: any[] = (words.value as any[]) || []
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
  const addMap = new Map<string, number>()
  const reviewMap = new Map<string, number>()
  days.forEach((d) => {
    addMap.set(key(d), 0)
    reviewMap.set(key(d), 0)
  })
  data.forEach((w: any) => {
    {
      const d = toDate(w?.createTime)
      if (d) {
        const k = key(d)
        if (addMap.has(k)) addMap.set(k, (addMap.get(k) || 0) + 1)
      }
    }
    {
      const d2 = toDate(w?.lastReviewTime)
      if (d2) {
        const k2 = key(d2)
        if (reviewMap.has(k2)) reviewMap.set(k2, (reviewMap.get(k2) || 0) + 1)
      }
    }
  })
  const addVals = days.map((d) => addMap.get(key(d)) || 0)
  const reviewVals = days.map((d) => reviewMap.get(key(d)) || 0)
  const niceMax = (v: number) => {
    if (v <= 5) return 5
    if (v <= 10) return 10
    if (v <= 20) return 20
    if (v <= 50) return 50
    if (v <= 100) return 100
    return Math.ceil(v * 1.2)
  }
  const addMax = niceMax(Math.max(...addVals, 0))
  const reviewMax = niceMax(Math.max(...reviewVals, 0))
  trendOption.value = {
    grid: { left: 10, right: 10, top: 10, bottom: 20, containLabel: false },
    xAxis: { type: 'category', data: labels, axisTick: { show: false }, axisLine: { show: false }, axisLabel: { fontSize: 10, color: '#999' } },
    yAxis: [
      {
        type: 'value',
        min: 0,
        max: addMax,
        splitLine: { show: false },
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { show: false }
      },
      {
        type: 'value',
        min: 0,
        max: reviewMax,
        splitLine: { show: false },
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { show: false }
      },
    ],
    series: [
      {
        name: '新增',
        type: 'line',
        smooth: true,
        yAxisIndex: 0,
        data: addVals,
        showSymbol: false,
        lineStyle: { color: '#409eff', width: 3 },
        itemStyle: { color: '#409eff' },
      },
      {
        name: '复习',
        type: 'bar',
        yAxisIndex: 1,
        data: reviewVals,
        itemStyle: { color: '#e5e7eb', borderRadius: 2 },
        barWidth: '60%'
      },
    ],
    tooltip: { trigger: 'axis' },
  }
}

const nextStage = (w: any) => {
  const count = typeof w?.reviewCount === 'number' ? w.reviewCount : 0
  const n = count + 1
  return n > 9 ? 9 : n
}

const nextIntervalText = (w: any) => {
  const st = getSm2State(w.id)
  if (!st) return '待学习'
  const days = Math.max(1, Math.round(st.interval))
  return `约 ${days} 天后`
}

const startNewReview = () => {
  const ids = todayWords.value.map((w:any) => w.id).join(',')
  const encoded = encodeURIComponent(ids)
  router.push(`/vocabulary/review?ids=${encoded}`)
}
</script>

<style scoped>
.words-page {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

/* Header */
.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.page-title h2 { margin: 0; font-size: 24px; color: var(--app-text); }
.subtitle { margin: 4px 0 0; color: var(--text-secondary); font-size: 14px; }

/* Tabs */
.list-card {
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  min-height: 600px;
}

.custom-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: var(--border);
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
}

.tab-badge :deep(.el-badge__content) {
  transform: translateY(-2px);
}

.tab-toolbar {
  padding: 16px 0;
  display: flex;
  justify-content: center;
}

.start-review-btn {
  width: 100%;
  font-weight: 600;
  box-shadow: var(--shadow-colored);
}

/* Word List */
.word-list {
  min-height: 400px;
}

.list-container {
  display: flex;
  flex-direction: column;
}

.word-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  transition: background 0.2s;
}

.word-item:hover {
  background: var(--app-bg);
  padding-left: 8px;
  padding-right: 8px;
  border-radius: 8px;
  margin: 0 -8px;
}

.word-img-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  overflow: hidden;
  margin-right: 16px;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
}

.word-img-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.img-placeholder {
  color: #cbd5e1;
  font-size: 20px;
}

.word-info {
  flex: 1;
}

.word-text {
  font-weight: 600;
  font-size: 15px;
  color: var(--app-text);
  margin-bottom: 2px;
}

.word-trans {
  color: var(--text-secondary);
  font-size: 13px;
}

.word-meta {
  text-align: right;
  margin-right: 16px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.next-time {
  font-size: 12px;
  color: var(--text-light);
}

.word-actions {
  display: flex;
  gap: 8px;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

/* Side Column */
.chart-card {
  border-radius: var(--radius-md);
  border: none;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.books-card {
  border-radius: var(--radius-md);
  border: none;
}

.books-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.book-item {
  display: flex;
  align-items: center;
  padding: 10px;
  background: #f8fafc;
  border-radius: 8px;
}

.book-icon {
  font-size: 24px;
  color: var(--primary);
  margin-right: 12px;
  background: var(--primary-light);
  padding: 8px;
  border-radius: 8px;
}

.book-info {
  flex: 1;
}

.book-name {
  font-weight: 500;
  font-size: 14px;
  color: var(--app-text);
}

.book-count {
  font-size: 12px;
  color: var(--text-light);
}

.dialog-tip {
  margin-top: 16px;
  background: #eff6ff;
  color: #3b82f6;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>