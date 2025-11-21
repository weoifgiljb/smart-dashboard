<template>
  <div class="diary-page">
    <el-card class="toolbar-card">
      <div class="toolbar">
        <div class="page-title">
          <h2>我的日记</h2>
          <span class="subtitle">记录生活，反思成长</span>
        </div>
        <el-button type="primary" @click="openDialog()">
          <el-icon><EditPen /></el-icon>
          写日记
        </el-button>
      </div>
    </el-card>

    <el-card class="diary-list-card" v-loading="loading">
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
                <el-tag v-if="diary.mood" size="small" :type="getMoodType(diary.mood)" effect="plain">
                  {{ getMoodLabel(diary.mood) }}
                </el-tag>
                <span class="diary-time" v-if="diary.updatedAt">
                  更新于 {{ formatTime(diary.updatedAt) }}
                </span>
              </div>
              <div class="diary-actions">
                <el-button type="primary" link @click="openDialog(diary)">编辑</el-button>
                <el-button type="danger" link @click="handleDelete(diary)">删除</el-button>
              </div>
            </div>
            <div class="diary-content" v-html="formatContent(diary.content)"></div>
            <div class="diary-tags" v-if="diary.tags && diary.tags.length">
              <el-tag v-for="tag in diary.tags" :key="tag" size="small" class="tag-item"># {{ tag }}</el-tag>
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
                <span style="font-size: 1.4em; vertical-align: middle;">{{ option.emoji }}</span>
              </el-tooltip>
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            placeholder="今天发生了什么？有什么感悟？"
          />
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
          <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { EditPen } from '@element-plus/icons-vue'
import { getDiaries, saveDiary, deleteDiary, type Diary } from '@/api/diary'

const loading = ref(false)
const saving = ref(false)
const diaries = ref<Diary[]>([])
const dialogVisible = ref(false)
const editingDiary = ref<Partial<Diary>>({})

const form = reactive<Diary>({
  diaryDate: '',
  content: '',
  mood: 'neutral',
  tags: []
})

const moodOptions = [
  { value: 'happy', label: '开心', emoji: '😄', type: 'success', color: '#10b981' },
  { value: 'neutral', label: '平淡', emoji: '😐', type: 'info', color: '#909399' },
  { value: 'sad', label: '难过', emoji: '😭', type: 'info', color: '#606266' },
  { value: 'energetic', label: '充满活力', emoji: '💪', type: 'warning', color: '#f59e0b' },
  { value: 'tired', label: '疲惫', emoji: '😫', type: 'danger', color: '#ef4444' }
]

const moodMap = moodOptions.reduce((acc, cur) => {
  acc[cur.value] = cur
  return acc
}, {} as Record<string, typeof moodOptions[0]>)

const getMoodLabel = (mood?: string) => (mood && moodMap[mood]) ? moodMap[mood].label : '未知'
const getMoodType = (mood?: string) => (mood && moodMap[mood]) ? moodMap[mood].type : 'info'
const getMoodColor = (mood?: string) => (mood && moodMap[mood]) ? moodMap[mood].color : '#909399'

const formatTime = (timeStr: string) => {
  if (!timeStr) return ''
  return new Date(timeStr).toLocaleString()
}

const formatContent = (content: string) => {
  if (!content) return ''
  return content.replace(/\n/g, '<br>')
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
      tags: diary.tags || []
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
      tags: []
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
    type: 'warning'
  }).then(async () => {
    if (diary.id) {
      await deleteDiary(diary.id)
      ElMessage.success('删除成功')
      loadData()
    }
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
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
  white-space: pre-wrap;
}

.diary-tags {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}

.tag-item {
  border-radius: 12px;
}
</style>
