<template>
  <div class="view-switcher">
    <el-radio-group :model-value="modelValue" size="default" @update:model-value="handleChange">
      <el-radio-button
        v-for="view in views"
        :key="view.value"
        :label="view.value"
        :disabled="view.disabled"
      >
        <el-icon v-if="view.icon">
          <component :is="view.icon" />
        </el-icon>
        <span v-if="view.label" class="view-label">{{ view.label }}</span>
      </el-radio-button>
    </el-radio-group>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

export interface ViewOption {
  value: string
  label?: string
  icon?: Component
  disabled?: boolean
}

defineProps<{
  modelValue: string
  views: ViewOption[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const handleChange = (value: string) => {
  emit('update:modelValue', value)
}
</script>

<style scoped>
.view-switcher {
  display: flex;
  align-items: center;
}

.view-label {
  margin-left: 4px;
}

:deep(.el-radio-button__inner) {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .view-switcher {
    width: 100%;
  }

  .view-switcher :deep(.el-radio-group) {
    width: 100%;
    display: flex;
  }

  .view-switcher :deep(.el-radio-button) {
    flex: 1;
  }

  .view-switcher :deep(.el-radio-button__inner) {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .view-label {
    display: none;
  }

  .view-switcher :deep(.el-radio-button__inner) {
    padding: 8px 12px;
  }
}
</style>
