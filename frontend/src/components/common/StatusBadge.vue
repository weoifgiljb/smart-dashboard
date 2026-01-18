<template>
  <div class="status-badge" :class="[`status-${status}`, size]">
    <span class="status-dot"></span>
    <span class="status-text">{{ text }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    status:
      | 'todo'
      | 'in_progress'
      | 'done'
      | 'blocked'
      | 'pending'
      | 'active'
      | 'completed'
      | 'cancelled'
    size?: 'small' | 'default' | 'large'
    customText?: string
  }>(),
  {
    size: 'default',
  },
)

const statusTextMap: Record<string, string> = {
  todo: '待办',
  in_progress: '进行中',
  done: '已完成',
  blocked: '阻塞',
  pending: '待处理',
  active: '活跃',
  completed: '完成',
  cancelled: '已取消',
}

const text = computed(() => {
  return props.customText || statusTextMap[props.status] || props.status
})
</script>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  transition: all 0.2s ease;
}

.status-badge.small {
  padding: 2px 8px;
  font-size: 12px;
  gap: 4px;
}

.status-badge.large {
  padding: 6px 16px;
  font-size: 14px;
  gap: 8px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.status-badge.small .status-dot {
  width: 5px;
  height: 5px;
}

.status-badge.large .status-dot {
  width: 7px;
  height: 7px;
}

/* Status colors */
.status-todo {
  background: #f3f4f6;
  color: #6b7280;
}

.status-todo .status-dot {
  background: #9ca3af;
}

.status-pending {
  background: #fef3c7;
  color: #d97706;
}

.status-pending .status-dot {
  background: #f59e0b;
}

.status-in_progress,
.status-active {
  background: #dbeafe;
  color: #1d4ed8;
}

.status-in_progress .status-dot,
.status-active .status-dot {
  background: #3b82f6;
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

.status-done,
.status-completed {
  background: #d1fae5;
  color: #065f46;
}

.status-done .status-dot,
.status-completed .status-dot {
  background: #10b981;
}

.status-blocked {
  background: #fee2e2;
  color: #b91c1c;
}

.status-blocked .status-dot {
  background: #ef4444;
}

.status-cancelled {
  background: #f3f4f6;
  color: #9ca3af;
  text-decoration: line-through;
}

.status-cancelled .status-dot {
  background: #d1d5db;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

/* 响应式设计 */
@media (max-width: 480px) {
  .status-badge {
    font-size: 12px;
    padding: 3px 10px;
    gap: 5px;
  }

  .status-badge.small {
    font-size: 11px;
    padding: 2px 7px;
    gap: 3px;
  }

  .status-badge.large {
    font-size: 13px;
    padding: 5px 14px;
    gap: 7px;
  }

  .status-text {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 120px;
  }
}
</style>
