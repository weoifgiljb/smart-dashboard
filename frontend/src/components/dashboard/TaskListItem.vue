<template>
  <div class="task-item" :class="{ completed }">
    <div class="task-icon-wrapper" :class="`task-icon-${iconType}`">
      <el-icon>
        <component :is="icon" />
      </el-icon>
    </div>
    <div class="task-info">
      <span class="task-title">{{ title }}</span>
      <span class="task-desc">{{ description }}</span>
    </div>
    <slot name="action">
      <el-tag
        v-if="statusTag"
        :type="statusTag.type"
        size="small"
        :effect="statusTag.effect || 'plain'"
      >
        {{ statusTag.text }}
      </el-tag>
    </slot>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

defineProps<{
  icon: Component
  iconType?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
  title: string
  description: string
  completed?: boolean
  statusTag?: {
    text: string
    type?: 'success' | 'info' | 'warning' | 'danger' | 'primary'
    effect?: 'dark' | 'light' | 'plain'
  }
}>()
</script>

<style scoped>
.task-item {
  display: flex;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid var(--border);
  transition: all 0.2s ease;
}

.task-item:hover {
  background: var(--app-bg);
  padding-left: 12px;
  padding-right: 12px;
  margin: 0 -12px;
  border-radius: 8px;
}

.task-item:last-child {
  border-bottom: none;
}

.task-icon-wrapper {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: var(--app-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  color: var(--text-secondary);
  flex-shrink: 0;
  transition: all 0.3s ease;
}

.task-item:hover .task-icon-wrapper {
  transform: scale(1.05);
}

.task-icon-primary {
  background: #eff6ff;
  color: #3b82f6;
}

.task-icon-success {
  background: #f0fdf4;
  color: #10b981;
}

.task-icon-warning {
  background: #fff7ed;
  color: #f59e0b;
}

.task-icon-danger {
  background: #fef2f2;
  color: #ef4444;
}

.task-icon-info {
  background: #f0f9ff;
  color: #0ea5e9;
}

.task-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.task-title {
  display: block;
  font-size: 15px;
  font-weight: 500;
  color: var(--app-text);
}

.task-desc {
  font-size: 12px;
  color: var(--text-light);
}

.task-item.completed {
  opacity: 0.6;
}

.task-item.completed .task-title {
  text-decoration: line-through;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .task-item {
    padding: 14px 0;
  }

  .task-item:hover {
    padding-left: 8px;
    padding-right: 8px;
    margin: 0 -8px;
  }

  .task-icon-wrapper {
    width: 36px;
    height: 36px;
    margin-right: 12px;
  }

  .task-title {
    font-size: 14px;
  }

  .task-desc {
    font-size: 11px;
  }
}

@media (max-width: 480px) {
  .task-item {
    padding: 12px 0;
    flex-wrap: wrap;
  }

  .task-icon-wrapper {
    width: 32px;
    height: 32px;
    margin-right: 10px;
    border-radius: 8px;
  }

  .task-info {
    flex: 1;
    min-width: calc(100% - 50px);
  }

  .task-title {
    font-size: 13px;
  }

  .task-desc {
    font-size: 11px;
  }
}
</style>
