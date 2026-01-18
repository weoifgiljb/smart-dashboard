<template>
  <div class="priority-badge" :class="[`priority-${priority}`, size]">
    <el-icon v-if="showIcon" class="priority-icon">
      <Top v-if="priority === 'urgent'" />
      <ArrowUp v-else-if="priority === 'high'" />
      <Minus v-else-if="priority === 'med'" />
      <ArrowDown v-else />
    </el-icon>
    <span class="priority-text">{{ text }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Top, ArrowUp, Minus, ArrowDown } from '@element-plus/icons-vue'

const props = withDefaults(
  defineProps<{
    priority: 'low' | 'med' | 'high' | 'urgent'
    size?: 'small' | 'default' | 'large'
    showIcon?: boolean
    customText?: string
  }>(),
  {
    size: 'default',
    showIcon: true,
  },
)

const priorityTextMap: Record<string, string> = {
  low: '低',
  med: '中',
  high: '高',
  urgent: '紧急',
}

const text = computed(() => {
  return props.customText || priorityTextMap[props.priority] || props.priority
})
</script>

<style scoped>
.priority-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  transition: all 0.2s ease;
}

.priority-badge.small {
  padding: 2px 6px;
  font-size: 11px;
  gap: 2px;
}

.priority-badge.large {
  padding: 6px 14px;
  font-size: 13px;
  gap: 6px;
}

.priority-icon {
  font-size: 14px;
}

.priority-badge.small .priority-icon {
  font-size: 12px;
}

.priority-badge.large .priority-icon {
  font-size: 16px;
}

/* Priority colors */
.priority-low {
  background: #f0f9ff;
  color: #0369a1;
  border: 1px solid #bae6fd;
}

.priority-med {
  background: #fef3c7;
  color: #d97706;
  border: 1px solid #fde68a;
}

.priority-high {
  background: #ffedd5;
  color: #c2410c;
  border: 1px solid #fed7aa;
}

.priority-urgent {
  background: #fee2e2;
  color: #b91c1c;
  border: 1px solid #fecaca;
  animation: urgentPulse 2s ease-in-out infinite;
}

@keyframes urgentPulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.4);
  }
  50% {
    box-shadow: 0 0 0 4px rgba(239, 68, 68, 0);
  }
}

/* 响应式设计 */
@media (max-width: 480px) {
  .priority-badge {
    font-size: 11px;
    padding: 3px 8px;
    gap: 3px;
  }

  .priority-badge.small {
    font-size: 10px;
    padding: 2px 5px;
    gap: 2px;
  }

  .priority-badge.large {
    font-size: 12px;
    padding: 5px 12px;
    gap: 5px;
  }

  .priority-icon {
    font-size: 12px;
  }

  .priority-badge.small .priority-icon {
    font-size: 11px;
  }

  .priority-badge.large .priority-icon {
    font-size: 14px;
  }

  .priority-text {
    white-space: nowrap;
  }
}
</style>
