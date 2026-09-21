<template>
  <div class="diary-page">
    <el-card class="toolbar-card">
      <div class="toolbar">
        <div class="page-title">
          <h2>我的日记</h2>
          <span class="subtitle">{{ periodHint }}</span>
        </div>
        <div class="period">
          <el-button @click="showAll">全部</el-button>
          <el-button @click="goMonth(-1)">上一月</el-button>
          <el-date-picker
            v-model="viewMonth"
            type="month"
            placeholder="全部月份"
            value-format="YYYY-MM"
            clearable
            class="month-picker"
            @change="onMonthChange"
          />
          <el-button @click="goMonth(1)">下一月</el-button>
          <el-button @click="showCurrentMonth">本月</el-button>
          <el-date-picker
            v-model="jumpDate"
            type="date"
            placeholder="查看某一天"
            value-format="YYYY-MM-DD"
            clearable
            class="day-picker"
            @change="onJumpDate"
          />
        </div>
        <div class="actions">
          <el-button :disabled="!diaries.length" :loading="exportingPdf" @click="handleExportPdf">
            导出 PDF
          </el-button>
          <el-button :disabled="!diaries.length" :loading="exportingWord" @click="handleExportWord">
            导出 Word
          </el-button>
          <el-button type="primary" @click="openDialog()">
            <el-icon><EditPen /></el-icon>
            写日记
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card v-loading="loading" class="diary-list-card">
      <div v-if="visibleDiaries.length === 0" class="empty-state">
        <el-empty :description="emptyDescription">
          <el-button v-if="focusDate" type="primary" @click="openBackfill">补记这一天</el-button>
          <el-button v-else-if="diaries.length" @click="showAll">查看全部日记</el-button>
        </el-empty>
      </div>
      <DiaryTimeline v-else :diaries="pagedDiaries" @edit="openDialog" @delete="handleDelete" />
      <AppPagination
        v-model="currentPage"
        :page-size="DIARY_PAGE_SIZE"
        :total="visibleDiaries.length"
        @change="scrollDiaryListIntoView"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="日期">
          <el-date-picker
            v-model="form.diaryDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            :disabled="dateLocked"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="心情">
          <el-radio-group v-model="form.mood">
            <el-radio-button :label="DiaryMood.Happy">
              <el-tooltip content="开心" placement="top" :show-after="200">
                <span class="mood-emoji">😄</span>
              </el-tooltip>
            </el-radio-button>
            <el-radio-button :label="DiaryMood.Neutral">
              <el-tooltip content="平淡" placement="top" :show-after="200">
                <span class="mood-emoji">😐</span>
              </el-tooltip>
            </el-radio-button>
            <el-radio-button :label="DiaryMood.Sad">
              <el-tooltip content="难过" placement="top" :show-after="200">
                <span class="mood-emoji">😭</span>
              </el-tooltip>
            </el-radio-button>
            <el-radio-button :label="DiaryMood.Energetic">
              <el-tooltip content="充满活力" placement="top" :show-after="200">
                <span class="mood-emoji">💪</span>
              </el-tooltip>
            </el-radio-button>
            <el-radio-button :label="DiaryMood.Tired">
              <el-tooltip content="疲惫" placement="top" :show-after="200">
                <span class="mood-emoji">😫</span>
              </el-tooltip>
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="内容">
          <div class="diary-composer">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="10"
              class="diary-editor"
              placeholder="写下今天的想法，支持 Markdown"
            />
            <div class="preview-label">预览</div>
            <!-- eslint-disable-next-line vue/no-v-html -->
            <div class="diary-preview" v-html="previewHtml"></div>
            <div class="composer-tools">
              <el-button size="small" :loading="matchingMeme" @click="handleMatchMeme">
                <el-icon><Picture /></el-icon>
                {{ form.imageUrl ? '重新配图' : '智能配图 (RAG)' }}
              </el-button>
              <div v-if="form.imageUrl" class="composer-image">
                <el-image :src="form.imageUrl" class="composer-image-preview" />
                <el-button
                  type="danger"
                  circle
                  size="small"
                  class="composer-image-clear"
                  @click="form.imageUrl = ''"
                >
                  <el-icon><Close /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="标签">
          <el-select
            v-model="form.tags"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="添加标签"
            style="width: 100%"
          >
            <el-option label="工作" value="工作" />
            <el-option label="学习" value="学习" />
            <el-option label="生活" value="生活" />
            <el-option label="运动" value="运动" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { isAxiosError } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close, EditPen, Picture } from '@element-plus/icons-vue'
import {
  asDownloadBlob,
  deleteDiary,
  exportPdf,
  exportWord,
  getDiaries,
  matchMeme,
  saveDiary,
  triggerDownload,
  type Diary,
} from '@/api/diary'
import DiaryTimeline from '@/components/DiaryTimeline.vue'
import AppPagination from '@/components/ui/AppPagination.vue'
import {
  DIARY_PAGE_SIZE,
  DiaryMood,
  clampPage,
  inMonth,
  monthKey,
  monthKeyFromDate,
  paginateItems,
  parseDiaryMood,
  renderDiaryHtml,
  shiftMonth,
  todayDateKey,
} from '@/utils/diaryDisplay'

const loading = ref(false)
const saving = ref(false)
const matchingMeme = ref(false)
const exportingPdf = ref(false)
const exportingWord = ref(false)
const diaries = ref<Diary[]>([])
const dialogVisible = ref(false)
const dateLocked = ref(false)
const viewMonth = ref('')
const jumpDate = ref('')
const focusDate = ref('')
const currentPage = ref(1)

const form = reactive({
  id: undefined as string | undefined,
  diaryDate: '',
  content: '',
  mood: DiaryMood.Neutral,
  tags: [] as string[],
  imageUrl: '',
})

const dialogTitle = computed(() => {
  if (form.id && form.diaryDate === todayDateKey()) return '编辑今天的日记'
  if (form.id) return '编辑日记'
  if (dateLocked.value) return '补记日记'
  return '写日记'
})

const previewHtml = computed(() => {
  const content = form.content.trim()
  if (!content) return '<p class="preview-empty">开始写点什么，预览会出现在这里</p>'
  return renderDiaryHtml(form.content)
})

const visibleDiaries = computed(() => {
  if (focusDate.value) {
    return diaries.value.filter((item) => item.diaryDate === focusDate.value)
  }
  if (viewMonth.value) {
    return diaries.value.filter((item) => inMonth(item.diaryDate, viewMonth.value))
  }
  return diaries.value
})

const pagedDiaries = computed(() =>
  paginateItems(visibleDiaries.value, currentPage.value, DIARY_PAGE_SIZE),
)

watch([viewMonth, focusDate], () => {
  currentPage.value = 1
})

watch(
  () => visibleDiaries.value.length,
  (len) => {
    currentPage.value = clampPage(currentPage.value, len, DIARY_PAGE_SIZE)
  },
)

function scrollDiaryListIntoView() {
  const el = document.querySelector('.diary-list-card')
  if (el && typeof el.scrollIntoView === 'function') {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

const periodHint = computed(() => {
  const total = diaries.value.length
  const shown = visibleDiaries.value.length
  if (!total) return '一天一篇，可回看任意日期'
  if (focusDate.value) return `${focusDate.value} · ${shown} 篇`
  if (viewMonth.value) return `${viewMonth.value} · ${shown} / ${total} 篇`
  return `全部 ${total} 篇 · 一天一篇`
})

const emptyDescription = computed(() => {
  if (diaries.value.length === 0) return '还没有写过日记，开始记录第一篇吧！'
  if (focusDate.value) return `${focusDate.value} 还没有日记，可以选择这一天补记`
  if (viewMonth.value) return `${viewMonth.value} 还没有日记，可切换月份或查看全部`
  return '还没有写过日记，开始记录第一篇吧！'
})

function showAll() {
  viewMonth.value = ''
  focusDate.value = ''
  jumpDate.value = ''
}

function showCurrentMonth() {
  viewMonth.value = monthKey()
  focusDate.value = ''
  jumpDate.value = ''
}

function showPeriod(dateKey: string, dayOnly: boolean) {
  viewMonth.value = monthKeyFromDate(dateKey)
  focusDate.value = dayOnly ? dateKey : ''
  jumpDate.value = dayOnly ? dateKey : ''
}

function goMonth(delta: number) {
  viewMonth.value = shiftMonth(viewMonth.value || monthKey(), delta)
  focusDate.value = ''
  jumpDate.value = ''
}

function onMonthChange() {
  if (!viewMonth.value) viewMonth.value = ''
  focusDate.value = ''
  jumpDate.value = ''
}

function onJumpDate() {
  if (!jumpDate.value) {
    focusDate.value = ''
    return
  }
  showPeriod(jumpDate.value, true)
}

function openBackfill() {
  if (!focusDate.value) return
  fillForm(
    {
      diaryDate: focusDate.value,
      content: '',
      mood: DiaryMood.Neutral,
      tags: [],
      imageUrl: '',
    },
    true,
  )
  dialogVisible.value = true
}

function fillForm(diary: Partial<Diary>, locked: boolean) {
  dateLocked.value = locked
  form.id = diary.id
  form.diaryDate = diary.diaryDate || todayDateKey()
  form.content = diary.content || ''
  form.mood = parseDiaryMood(String(diary.mood || ''))
  form.tags = diary.tags ? [...diary.tags] : []
  form.imageUrl = diary.imageUrl || ''
}

function asDiaryList(res: unknown) {
  if (Array.isArray(res)) return res as Diary[]
  return []
}

const loadData = async () => {
  try {
    loading.value = true
    diaries.value = asDiaryList(await getDiaries())
  } catch (e) {
    if (!isAxiosError(e)) ElMessage.error('加载日记失败')
  } finally {
    loading.value = false
  }
}

function openDialog(diary?: Diary) {
  if (diary) {
    fillForm(diary, true)
    dialogVisible.value = true
    return
  }
  const today = todayDateKey()
  const existing = diaries.value.find((item) => item.diaryDate === today)
  if (existing) {
    fillForm(existing, true)
  } else {
    fillForm(
      {
        diaryDate: today,
        content: '',
        mood: DiaryMood.Neutral,
        tags: [],
        imageUrl: '',
      },
      false,
    )
  }
  dialogVisible.value = true
}

async function confirmOverwriteIfNeeded() {
  const existing = diaries.value.find((item) => item.diaryDate === form.diaryDate)
  if (!existing || existing.id === form.id) return true
  try {
    await ElMessageBox.confirm(`将覆盖 ${form.diaryDate} 的内容`, '覆盖确认', {
      confirmButtonText: '覆盖',
      cancelButtonText: '取消',
      type: 'warning',
    })
    return true
  } catch {
    return false
  }
}

const handleSave = async () => {
  if (!form.content.trim()) {
    ElMessage.warning('请填写日记内容')
    return
  }
  if (!form.diaryDate) {
    ElMessage.warning('请选择日期')
    return
  }
  if (!(await confirmOverwriteIfNeeded())) return
  try {
    saving.value = true
    await saveDiary({
      id: form.id,
      diaryDate: form.diaryDate,
      content: form.content,
      mood: form.mood,
      tags: form.tags,
      imageUrl: form.imageUrl,
    })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    showPeriod(form.diaryDate, true)
    await loadData()
  } catch (e) {
    if (!isAxiosError(e)) ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleDelete = (diary: Diary) => {
  ElMessageBox.confirm('确定要删除这篇日记吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      if (!diary.id) return
      try {
        await deleteDiary(diary.id)
        ElMessage.success('删除成功')
        await loadData()
      } catch {
        // 拦截器已提示失败，避免假成功
      }
    })
    .catch(() => undefined)
}

async function downloadExport(
  fetcher: () => Promise<unknown>,
  mime: string,
  filename: string,
  loadingFlag: { value: boolean },
) {
  if (!diaries.value.length) {
    ElMessage.warning('暂无日记可导出')
    return
  }
  try {
    loadingFlag.value = true
    const blob = await asDownloadBlob(await fetcher(), mime)
    triggerDownload(blob, filename)
  } catch (e) {
    if (!isAxiosError(e)) {
      ElMessage.error(e instanceof Error ? e.message : '导出失败')
    }
  } finally {
    loadingFlag.value = false
  }
}

const handleExportPdf = () =>
  downloadExport(exportPdf, 'application/pdf', `diaries-${todayDateKey()}.pdf`, exportingPdf)

const handleExportWord = () =>
  downloadExport(
    exportWord,
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    `diaries-${todayDateKey()}.docx`,
    exportingWord,
  )

const handleMatchMeme = async () => {
  if (!form.content.trim()) {
    ElMessage.warning('请先填写日记内容')
    return
  }
  try {
    matchingMeme.value = true
    const res = await matchMeme(form.content)
    if (typeof res === 'string' && res) {
      form.imageUrl = res
      ElMessage.success('智能配图成功')
    } else {
      ElMessage.warning('未找到合适的配图')
    }
  } catch (e) {
    if (!isAxiosError(e)) ElMessage.error('智能配图失败')
  } finally {
    matchingMeme.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="less">
.diary-page {
  max-width: 1000px;
  margin: 0 auto;
}

.toolbar-card {
  margin-bottom: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.page-title h2 {
  margin: 0;
  font-size: 20px;
  color: var(--color-text);
}

.subtitle {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.period {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.month-picker,
.day-picker {
  width: 150px;
}

.diary-list-card {
  min-height: 500px;
  min-width: 0;
}

.diary-timeline {
  min-width: 0;
}

.diary-list-card :deep(.el-timeline-item__wrapper),
.diary-list-card :deep(.el-timeline-item__content) {
  min-width: 0;
  max-width: 100%;
}

.mood-emoji {
  font-size: 1.4em;
  vertical-align: middle;
}

.diary-composer {
  width: 100%;
  min-width: 0;
}

.diary-editor :deep(.el-textarea__inner) {
  background: var(--color-bg-muted);
  color: var(--color-text);
  border-color: var(--color-border);
  box-shadow: none;
}

.preview-label {
  margin: 12px 0 6px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.diary-preview {
  min-height: 120px;
  max-height: 240px;
  overflow: auto;
  padding: 12px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-bg-muted);
  color: var(--color-text);
  overflow-wrap: anywhere;
  word-break: break-word;
}

.diary-preview :deep(.preview-empty) {
  margin: 0;
  color: var(--color-text-muted);
}

.composer-tools {
  margin-top: 10px;
}

.composer-image {
  margin-top: 10px;
  position: relative;
  display: inline-block;
  max-width: 100%;
}

.composer-image-preview {
  max-height: 150px;
  max-width: 100%;
  border-radius: 4px;
}

.composer-image-clear {
  position: absolute;
  top: -5px;
  right: -5px;
}

.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
