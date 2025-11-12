<template>
  <div class="words-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>背单词</h3>
          <div>
            <el-button v-if="showDefaultBtn" style="margin-right: 8px" type="success" plain :loading="defaultLoading" @click="handleImportDefault">一键导入默认词库</el-button>
            <el-button style="margin-right: 8px" @click="showImportDialog = true">导入词库</el-button>
            <el-button type="primary" @click="showAddDialog = true">添加单词</el-button>
          </div>
        </div>
      </template>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="今日需背" name="today">
          <div style="margin-bottom: 10px; display: flex; justify-content: space-between; align-items: center;">
            <div style="display: flex; gap: 12px;">
              <el-button type="success" :disabled="selectedTodayIds.length === 0" @click="startNewReview">
                <el-icon><Star /></el-icon>
                新版复习（{{ selectedTodayIds.length }}）
              </el-button>
            </div>
          </div>
          <el-table :data="pagedTodayWords" style="width: 100%" @selection-change="onTodaySelectionChange">
            <el-table-column type="selection" width="50" />
            <el-table-column prop="word" label="单词" />
            <el-table-column prop="translation" label="翻译" />
            <el-table-column label="配图" width="120">
              <template #default="scope">
                <img v-if="scope.row.image" :src="scope.row.image" style="width: 60px; height: 60px; object-fit: cover; border-radius: 6px;" />
                <el-button v-else size="small" type="primary" plain @click="handleGenWordImage(scope.row)">生成图</el-button>
              </template>
            </el-table-column>
            <el-table-column prop="book" label="书籍" width="160" />
            <el-table-column prop="sectionIndex" label="分区" width="80" />
            <el-table-column prop="dueDate" label="计划日期" width="160">
              <template #default="scope">
                <span>{{ formatDate(scope.row.dueDate) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="nextStage" label="下个阶段" width="180">
              <template #default="scope">
                <div>
                  <div>第{{ nextStage(scope.row) }}/9</div>
                  <div style="color: var(--app-subtext); font-size: 12px;">{{ nextIntervalText(scope.row) }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="380">
              <template #default="scope">
                <el-button size="small" @click="handleReview(scope.row)">复习</el-button>
                <el-button size="small" type="success" @click="handleDone(scope.row.id)">完成</el-button>
                <el-button size="small" type="warning" @click="handleReset(scope.row.id)">忘了/重置</el-button>
                <el-button v-if="scope.row.image" size="small" type="primary" plain @click="handleGenWordImage(scope.row)">重生图</el-button>
                <el-button size="small" type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div style="margin-top: 12px; text-align: right;">
            <el-pagination
              v-model:current-page="currentPageToday"
              :page-size="pageSize"
              layout="prev, pager, next, total"
              :total="todayWords.length"
            />
          </div>
        </el-tab-pane>
        <el-tab-pane label="全部" name="all">
          <el-table :data="pagedWords" style="width: 100%">
            <el-table-column prop="word" label="单词" />
            <el-table-column prop="translation" label="翻译" />
            <el-table-column prop="example" label="例句" />
            <el-table-column label="配图" width="120">
              <template #default="scope">
                <img v-if="scope.row.image" :src="scope.row.image" style="width: 60px; height: 60px; object-fit: cover; border-radius: 6px;" />
                <el-button v-else size="small" type="primary" plain @click="handleGenWordImage(scope.row)">生成图</el-button>
              </template>
            </el-table-column>
            <el-table-column prop="reviewCount" label="复习次数" width="100" />
            <el-table-column prop="book" label="书籍" width="160" />
            <el-table-column prop="sectionIndex" label="分区" width="80" />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column prop="dueDate" label="计划日期" width="160">
              <template #default="scope">
                <span>{{ formatDate(scope.row.dueDate) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="nextStage" label="下个阶段" width="180">
              <template #default="scope">
                <div>
                  <div>第{{ nextStage(scope.row) }}/9</div>
                  <div style="color: var(--app-subtext); font-size: 12px;">{{ nextIntervalText(scope.row) }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="320">
              <template #default="scope">
                <el-button size="small" @click="handleReview(scope.row)">复习</el-button>
                <el-button size="small" type="warning" @click="handleReset(scope.row.id)">重置</el-button>
                <el-button v-if="scope.row.image" size="small" type="primary" plain @click="handleGenWordImage(scope.row)">重生图</el-button>
                <el-button size="small" type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div style="margin-top: 12px; text-align: right;">
            <el-pagination
              v-model:current-page="currentPageAll"
              :page-size="pageSize"
              layout="prev, pager, next, total"
              :total="words.length"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <h3>近7天新增/复习趋势</h3>
        </div>
      </template>
      <BaseChart :option="trendOption" height="220px" />
    </el-card>

    <el-dialog v-model="showAddDialog" title="添加单词" width="500px">
      <el-form :model="wordForm" label-width="80px">
        <el-form-item label="单词">
          <el-input v-model="wordForm.word" />
        </el-form-item>
        <el-form-item label="翻译">
          <el-input v-model="wordForm.translation" />
        </el-form-item>
        <el-form-item label="例句">
          <el-input v-model="wordForm.example" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAdd">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showImportDialog" title="导入词库（GitHub 原始链接）" width="600px">
      <el-form :model="importForm" label-width="120px">
        <el-form-item label="原始链接">
          <el-input v-model="importForm.sourceUrl" placeholder="https://raw.githubusercontent.com/xxx/wordlist.txt" />
        </el-form-item>
        <el-form-item label="书籍/词库名称">
          <el-input v-model="importForm.bookName" placeholder="如 CET4 高频词" />
        </el-form-item>
        <el-form-item label="每区单词数">
          <el-input v-model.number="importForm.sectionSize" placeholder="如 50" />
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="importForm.startDate" type="date" placeholder="选择日期" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-alert
          title="格式说明：每行一个词；也支持“单词|翻译”、“单词,翻译”、“单词[TAB]翻译”。分区会按顺序自动分配到每天学习计划。"
          type="info"
          :closable="false"
          show-icon
        />
      </el-form>
      <template #footer>
        <el-button @click="showImportDialog = false">取消</el-button>
        <el-button type="primary" :loading="importLoading" @click="handleImport">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Star } from '@element-plus/icons-vue'
import { getWords, addWord, deleteWord, reviewWord, getTodayWords, importWords, updateWordStatus, getBooks, importDefaultWords } from '@/api/words'
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
  example: ''
})
const importForm = ref({
  sourceUrl: '',
  bookName: '',
  sectionSize: 50,
  startDate: ''
})
const importLoading = ref(false)
const trendOption = ref<any>({})
const activeTab = ref('today')
const showDefaultBtn = ref(false)
const selectedTodayIds = ref<string[]>([])

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

// 跨页复习完成后，自动刷新趋势图与列表
const handleReviewedEvent = async () => {
  try {
    await loadWords()
    await loadTodayWords()
  } catch {}
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

// 兼容后端时间戳（秒/毫秒）与字符串
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
  // 跳转到新版复习页面，只复习这一个单词
  const ids = encodeURIComponent(word.id)
  router.push(`/vocabulary/review?ids=${ids}`)
}

const handleGenWordImage = async (row: any) => {
  try {
    const updated: any = await generateWordImage(row.id)
    // 同步更新两个列表中的该条
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

const handleReset = async (id: string) => {
  try {
    await updateWordStatus(id, 'todo')
    ElMessage.success('已重置到首阶段（5分钟后再来）')
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
  const key = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  const labels = days.map(d => `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`)
  const addMap = new Map<string, number>()
  const reviewMap = new Map<string, number>()
  days.forEach(d => { addMap.set(key(d), 0); reviewMap.set(key(d), 0) })
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
  const addVals = days.map(d => addMap.get(key(d)) || 0)
  const reviewVals = days.map(d => reviewMap.get(key(d)) || 0)
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
    grid: { left: 20, right: 10, top: 10, bottom: 20, containLabel: false },
    xAxis: { type: 'category', data: labels, axisTick: { show: false }, axisLine: { show: false } },
    yAxis: [
      { type: 'value', min: 0, max: addMax, splitLine: { show: false }, axisLine: { show: false }, axisTick: { show: false } },
      { type: 'value', min: 0, max: reviewMax, splitLine: { show: false }, axisLine: { show: false }, axisTick: { show: false } }
    ],
    legend: { data: ['新增', '复习'] },
    series: [
      { name: '新增', type: 'line', smooth: true, yAxisIndex: 0, data: addVals, lineStyle: { color: '#409eff' }, itemStyle: { color: '#409eff' } },
      { name: '复习', type: 'line', smooth: true, yAxisIndex: 1, data: reviewVals, lineStyle: { color: '#67c23a' }, itemStyle: { color: '#67c23a' } }
    ],
    tooltip: { trigger: 'axis' }
  }
}

const formatDate = (val: any) => {
  if (!val) return ''
  const d = new Date(val)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const nextStage = (w: any) => {
  const count = typeof w?.reviewCount === 'number' ? w.reviewCount : 0
  const n = count + 1
  return n > 9 ? 9 : n
}

const nextIntervalText = (w: any) => {
  const st = getSm2State(w.id)
  if (!st) return '—'
  const days = Math.max(1, Math.round(st.interval))
  return `约 ${days} 天后`
}

const onTodaySelectionChange = (selection: any[]) => {
  selectedTodayIds.value = (selection || []).map((w: any) => w.id)
}

const startNewReview = () => {
  if (!selectedTodayIds.value.length) return
  const ids = encodeURIComponent(selectedTodayIds.value.join(','))
  router.push(`/vocabulary/review?ids=${ids}`)
}
</script>

<style scoped>
.words-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
}
</style>





