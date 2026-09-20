<template>
  <div class="diary-page">
    <el-card class="toolbar-card">
      <div class="toolbar">
        <div class="page-title">
          <h2>我的日记</h2>
          <span class="subtitle">记录生活，反思成长</span>
        </div>
        <div class="actions">
          <el-button @click="handleExportPdf">导出 PDF</el-button>
          <el-button @click="handleExportWord">导出 Word</el-button>
          <el-button type="primary" @click="openDialog()">
            <el-icon><EditPen /></el-icon>
            写日记
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card v-loading="loading" class="diary-list-card">
      <div v-if="diaries.length === 0" class="empty-state">
        <el-empty description="还没有写过日记，开始记录第一篇吧！" />
      </div>

      <el-timeline v-else>
        <el-timeline-item
          v-for="diary in diaries"
          :key="diary.id"
          :timestamp="diary.diaryDate"
          placement="top"
          :color="getMoodColor(diary.mood)"
        >
          <el-card class="diary-item-card" shadow="hover">
            <div class="diary-header">
              <div class="diary-meta">
                <el-tag
                  v-if="diary.mood"
                  size="small"
                  :type="getMoodType(diary.mood)"
                  effect="plain"
                >
                  {{ getMoodLabel(diary.mood) }}
                </el-tag>
                <span v-if="diary.updatedAt" class="diary-time">
                  更新于 {{ formatTime(diary.updatedAt) }}
                </span>
              </div>
              <div class="diary-actions">
                <el-button type="primary" link @click="openDialog(diary)">编辑</el-button>
                <el-button type="danger" link @click="handleDelete(diary)">删除</el-button>
              </div>
            </div>
            <!-- eslint-disable-next-line vue/no-v-html -->
            <div class="diary-content" v-html="formatContent(diary.content)"></div>
            <div v-if="diary.imageUrl" class="diary-image" style="margin-top: 10px">
              <el-image
                :src="diary.imageUrl"
                fit="contain"
                style="max-height: 200px; border-radius: 8px"
                :preview-src-list="[diary.imageUrl]"
              />
            </div>
            <div v-if="diary.tags && diary.tags.length" class="diary-tags">
              <el-tag v-for="tag in diary.tags" :key="tag" size="small" class="tag-item"
                ># {{ tag }}</el-tag
              >
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <!-- 编辑/新建对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingDiary.id ? '编辑日记' : '写日记'"
      width="600px"
      destroy-on-close
    >
      <el-form :model="form" label-width="80px">
        <el-form-item label="日期">
          <el-date-picker
            v-model="form.diaryDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="心情">
          <el-radio-group v-model="form.mood">
            <el-radio-button
              v-for="option in moodOptions"
              :key="option.value"
              :label="option.value"
            >
              <el-tooltip :content="option.label" placement="top" :show-after="200">
                <span style="font-size: 1.4em; vertical-align: middle">{{ option.emoji }}</span>
              </el-tooltip>
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="内容">
          <div
            style="
              border: 1px solid var(--el-border-color);
              border-radius: 4px;
              overflow: hidden;
              width: 100%;
            "
          >
            <VueMonacoEditor
              v-model:value="form.content"
              theme="vs"
              :options="{
                minimap: { enabled: false },
                automaticLayout: true,
                wordWrap: 'on',
                fontSize: 14,
                lineNumbers: 'off',
                renderLineHighlight: 'none',
                scrollBeyondLastLine: false,
              }"
              language="markdown"
              height="400px"
            />
          </div>
          <div style="margin-top: 10px">
            <el-button size="small" :loading="matchingMeme" @click="handleMatchMeme">
              <el-icon><Picture /></el-icon> {{ form.imageUrl ? '重新配图' : '智能配图 (RAG)' }}
            </el-button>
            <div
              v-if="form.imageUrl"
              style="margin-top: 10px; position: relative; display: inline-block"
            >
              <el-image :src="form.imageUrl" style="max-height: 150px; border-radius: 4px" />
              <el-button
                type="danger"
                circle
                size="small"
                style="position: absolute; top: -5px; right: -5px"
                @click="form.imageUrl = ''"
              >
                <el-icon><Close /></el-icon>
              </el-button>
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
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { EditPen, Picture, Close } from '@element-plus/icons-vue'
import {
  getDiaries,
  saveDiary,
  deleteDiary,
  exportPdf,
  exportWord,
  matchMeme,
  type Diary,
} from '@/api/diary'
import { VueMonacoEditor } from '@guolao/vue-monaco-editor'
import { marked } from 'marked'
import { sanitizeHtml } from '@/utils/sanitizeHtml'

const loading = ref(false)
const saving = ref(false)
const matchingMeme = ref(false)
const diaries = ref<Diary[]>([])
const dialogVisible = ref(false)
const editingDiary = ref<Partial<Diary>>({})

const form = reactive<Diary>({
  diaryDate: '',
  content: '',
  mood: 'neutral',
  tags: [],
  imageUrl: '',
})

const moodOptions = [
  { value: 'happy', label: '开心', emoji: '😄', type: 'success', color: 'var(--color-success)' },
  { value: 'neutral', label: '平淡', emoji: '😐', type: 'info', color: 'var(--color-text-muted)' },
  { value: 'sad', label: '难过', emoji: '😭', type: 'info', color: 'var(--color-text-secondary)' },
  {
    value: 'energetic',
    label: '充满活力',
    emoji: '💪',
    type: 'warning',
    color: 'var(--color-warning)',
  },
  { value: 'tired', label: '疲惫', emoji: '😫', type: 'danger', color: 'var(--color-danger)' },
]

const moodMap = moodOptions.reduce(
  (acc, cur) => {
    acc[cur.value] = cur
    return acc
  },
  {} as Record<string, (typeof moodOptions)[0]>,
)

const getMoodLabel = (mood?: string) => (mood && moodMap[mood] ? moodMap[mood].label : '未知')
const getMoodType = (mood?: string) => (mood && moodMap[mood] ? moodMap[mood].type : 'info')
const getMoodColor = (mood?: string) =>
  mood && moodMap[mood] ? moodMap[mood].color : 'var(--color-text-muted)'

const formatTime = (timeStr: string) => {
  if (!timeStr) return ''
  return new Date(timeStr).toLocaleString()
}

const formatContent = (content: string) => {
  if (!content) return ''
  try {
    return sanitizeHtml(String(marked.parse(content)))
  } catch (e) {
    return sanitizeHtml(content)
  }
}

const loadData = async () => {
  try {
    loading.value = true
    const res: any = await getDiaries()
    diaries.value = res || []
  } catch (e) {
    ElMessage.error('加载日记失败')
  } finally {
    loading.value = false
  }
}

const openDialog = (diary?: Diary) => {
  if (diary) {
    editingDiary.value = diary
    Object.assign(form, {
      id: diary.id,
      diaryDate: diary.diaryDate,
      content: diary.content,
      mood: diary.mood || 'neutral',
      tags: diary.tags || [],
      imageUrl: diary.imageUrl || '',
    })
  } else {
    editingDiary.value = {}
    const today = new Date()
    const y = today.getFullYear()
    const m = String(today.getMonth() + 1).padStart(2, '0')
    const d = String(today.getDate()).padStart(2, '0')

    Object.assign(form, {
      id: undefined,
      diaryDate: `${y}-${m}-${d}`,
      content: '',
      mood: 'neutral',
      tags: [],
      imageUrl: '',
    })
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!form.content.trim()) {
    ElMessage.warning('请填写日记内容')
    return
  }
  try {
    saving.value = true
    await saveDiary(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleDelete = (diary: Diary) => {
  ElMessageBox.confirm('确定要删除这篇日记吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    if (diary.id) {
      await deleteDiary(diary.id)
      ElMessage.success('删除成功')
      loadData()
    }
  })
}

const handleExportPdf = async () => {
  try {
    const res: any = await exportPdf()
    const blob = new Blob([res], { type: 'application/pdf' })
    const link = document.createElement('a')
    link.href = window.URL.createObjectURL(blob)
    link.download = 'diaries.pdf'
    link.click()
  } catch (e) {
    ElMessage.error('导出 PDF 失败')
  }
}

const handleExportWord = async () => {
  try {
    const res: any = await exportWord()
    const blob = new Blob([res], {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    })
    const link = document.createElement('a')
    link.href = window.URL.createObjectURL(blob)
    link.download = 'diaries.docx'
    link.click()
  } catch (e) {
    ElMessage.error('导出 Word 失败')
  }
}

const handleMatchMeme = async () => {
  if (!form.content.trim()) {
    ElMessage.warning('请先填写日记内容')
    return
  }
  try {
    matchingMeme.value = true
    const res: any = await matchMeme(form.content)
    if (res) {
      form.imageUrl = res
      ElMessage.success('智能配图成功')
    } else {
      ElMessage.warning('未找到合适的配图')
    }
  } catch (e) {
    ElMessage.error('智能配图失败')
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
}

.page-title h2 {
  margin: 0;
  font-size: 20px;
  color: var(--app-text);
}

.subtitle {
  font-size: 12px;
  color: var(--text-secondary);
}

.diary-list-card {
  min-height: 500px;
}

.diary-item-card {
  border-radius: var(--radius-md);
}

.diary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.diary-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.diary-time {
  font-size: 12px;
  color: var(--text-light);
}

.diary-content {
  font-size: 15px;
  line-height: 1.6;
  color: var(--app-text);
  /* white-space: pre-wrap; Removed to let markdown handle spacing */
}

.diary-content :deep(p) {
  margin: 0.5em 0;
}

.diary-content :deep(h1),
.diary-content :deep(h2),
.diary-content :deep(h3) {
  margin: 0.8em 0 0.4em;
  font-weight: 600;
}

.diary-content :deep(ul),
.diary-content :deep(ol) {
  padding-left: 1.5em;
  margin: 0.5em 0;
}

.diary-content :deep(blockquote) {
  margin: 0.5em 0;
  padding-left: 1em;
  border-left: 4px solid var(--el-border-color);
  color: var(--text-secondary);
}

.diary-content :deep(code) {
  background-color: var(--el-fill-color-light);
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-family: monospace;
}

.diary-content :deep(pre) {
  background-color: var(--el-fill-color-light);
  padding: 1em;
  border-radius: 4px;
  overflow-x: auto;
}

.diary-image {
  margin-top: 10px;
}

.diary-tags {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}

.tag-item {
  border-radius: 12px;
}

.actions {
  display: flex;
  gap: 10px;
}
</style>
