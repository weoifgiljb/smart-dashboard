<template>
  <div class="activity-item">
    <div class="activity-icon" :class="type">
      <el-icon>
        <component :is="icon" />
      </el-icon>
    </div>
    <div class="activity-content">
      <div class="activity-title">{{ title }}</div>
      <div class="activity-time">{{ formattedTime }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Component } from 'vue'

const props = defineProps<{
  icon: Component
  title: string
  time: string | Date
  type: 'checkin' | 'pomodoro' | 'word' | 'task' | 'diary'
}>()

const formattedTime = computed(() => {
  const date = new Date(props.time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
})
</script>

<style scoped>
.activity-item {
  display: flex;
  align-items: flex-start;
  padding: 12px 0;
  border-bottom: 1px dashed var(--el-border-color-lighter);
  transition: all 0.2s ease;
}

.activity-item:hover {
  background: var(--app-bg);
  padding-left: 12px;
  padding-right: 12px;
  margin: 0 -12px;
  border-radius: 8px;
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  flex-shrink: 0;
  font-size: 14px;
  transition: transform 0.3s ease;
}

.activity-item:hover .activity-icon {
  transform: scale(1.1);
}

.activity-icon.checkin {
  background-color: var(--success-light);
  color: var(--success);
}

.activity-icon.pomodoro {
  background-color: var(--warning-light);
  color: var(--warning);
}

.activity-icon.word {
  background-color: var(--info-light);
  color: var(--info);
}

.activity-icon.task {
  background-color: var(--primary-light);
  color: var(--primary);
}

.activity-icon.diary {
  background-color: #fef2f2;
  color: #ef4444;
}

.activity-content {
  flex: 1;
  min-width: 0;
}

.activity-title {
  font-size: 14px;
  color: var(--app-text);
  margin-bottom: 4px;
  font-weight: 500;
}

.activity-time {
  font-size: 12px;
  color: var(--text-light);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .activity-item {
    padding: 10px 0;
  }

  .activity-item:hover {
    padding-left: 8px;
    padding-right: 8px;
    margin: 0 -8px;
  }

  .activity-icon {
    width: 28px;
    height: 28px;
    font-size: 13px;
    margin-right: 10px;
  }

  .activity-title {
    font-size: 13px;
  }

  .activity-time {
    font-size: 11px;
  }
}

@media (max-width: 480px) {
  .activity-item {
    padding: 8px 0;
  }

  .activity-icon {
    width: 26px;
    height: 26px;
    font-size: 12px;
    margin-right: 8px;
  }

  .activity-title {
    font-size: 12px;
  }

  .activity-time {
    font-size: 10px;
  }
}
</style>
