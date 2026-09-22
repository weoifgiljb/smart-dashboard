<template>
  <el-card class="diary-item-card" shadow="hover" :data-diary-date="diary.diaryDate">
    <div class="diary-header">
      <div class="diary-meta">
        <el-tag v-if="mood" size="small" :type="tagType" effect="plain">
          {{ label }}
        </el-tag>
        <span v-if="updatedLabel" class="diary-time">{{ updatedLabel }}</span>
      </div>
      <div class="diary-actions">
        <el-button type="primary" link @click="emit('edit', diary)">编辑</el-button>
        <el-button type="danger" link @click="emit('delete', diary)">删除</el-button>
      </div>
    </div>
    <!-- eslint-disable-next-line vue/no-v-html -->
    <div class="diary-content" v-html="html"></div>
    <div v-if="diary.imageUrl" class="diary-image">
      <el-image :src="diary.imageUrl" fit="contain" :preview-src-list="[diary.imageUrl]" />
    </div>
    <DiaryTags v-if="(diary.tags || []).length" :tags="diary.tags || []" />
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Diary } from '@/api/diary'
import DiaryTags from '@/components/DiaryTags.vue'
import {
  formatUpdatedAt,
  moodLabel,
  moodTagType,
  parseDiaryMood,
  renderDiaryHtml,
} from '@/utils/diaryDisplay'

const props = defineProps<{
  diary: Diary
}>()

const emit = defineEmits<{
  edit: [diary: Diary]
  delete: [diary: Diary]
}>()

const mood = computed(() => parseDiaryMood(String(props.diary.mood || '')))
const label = computed(() => moodLabel(mood.value))
const tagType = computed(() => moodTagType(mood.value))
const updatedLabel = computed(() => formatUpdatedAt(props.diary.updatedAt, props.diary.diaryDate))

const html = computed(() => renderDiaryHtml(props.diary.content || ''))
</script>

<style scoped lang="less">
.diary-item-card {
  min-width: 0;
  overflow: hidden;
  border-radius: var(--radius-md);
}

.diary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--color-border);
}

.diary-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  min-width: 0;
}

.diary-time {
  font-size: 12px;
  color: var(--color-text-muted);
}

.diary-content {
  min-width: 0;
  max-width: 100%;
  overflow-wrap: anywhere;
  word-break: break-word;
  font-size: 15px;
  line-height: 1.6;
  color: var(--color-text);
}

.diary-content :deep(p) {
  margin: 0.5em 0;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.diary-content :deep(h1),
.diary-content :deep(h2),
.diary-content :deep(h3) {
  margin: 0.8em 0 0.4em;
  font-weight: 600;
  overflow-wrap: anywhere;
}

.diary-content :deep(ul),
.diary-content :deep(ol) {
  padding-left: 1.5em;
  margin: 0.5em 0;
}

.diary-content :deep(blockquote) {
  margin: 0.5em 0;
  padding-left: 1em;
  border-left: 4px solid var(--color-border);
  color: var(--color-text-secondary);
}

.diary-content :deep(code) {
  background-color: var(--color-bg-muted);
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-family: monospace;
  overflow-wrap: anywhere;
}

.diary-content :deep(pre) {
  background-color: var(--color-bg-muted);
  padding: 1em;
  border-radius: 4px;
  overflow-x: auto;
  max-width: 100%;
}

.diary-content :deep(img) {
  max-width: 100%;
  height: auto;
}

.diary-image {
  margin-top: 10px;
  max-width: 100%;
}

.diary-image :deep(.el-image) {
  max-width: 100%;
}

.diary-image :deep(img) {
  max-width: 100%;
  max-height: 200px;
  height: auto;
  border-radius: 8px;
}

.diary-actions {
  flex-shrink: 0;
}
</style>
