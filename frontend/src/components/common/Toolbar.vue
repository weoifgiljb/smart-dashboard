<template>
  <div class="toolbar-container">
    <div class="filters">
      <el-input
        v-model="searchText"
        :placeholder="searchPlaceholder"
        prefix-icon="Search"
        clearable
        class="search-input"
        @keyup.enter="$emit('search', searchText)"
        @clear="$emit('search', '')"
      />
      <slot name="filters" />
      <el-button circle :loading="loading" @click="$emit('refresh')">
        <el-icon><RefreshRight /></el-icon>
      </el-button>
    </div>

    <div v-if="$slots.actions" class="toolbar-actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { RefreshRight } from '@element-plus/icons-vue'

const props = withDefaults(
  defineProps<{
    modelValue?: string
    searchPlaceholder?: string
    loading?: boolean
  }>(),
  {
    modelValue: '',
    searchPlaceholder: '搜索...',
    loading: false,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
  search: [value: string]
  refresh: []
}>()

const searchText = ref(props.modelValue)

watch(
  () => props.modelValue,
  (val) => {
    searchText.value = val
  },
)

watch(searchText, (val) => {
  emit('update:modelValue', val)
})
</script>

<style scoped>
.toolbar-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 16px;
  padding: 16px;
  background: var(--card-bg);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.filters {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  flex-wrap: wrap;
}

.search-input {
  width: 240px;
  min-width: 180px;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

@media (max-width: 768px) {
  .toolbar-container {
    flex-direction: column;
    align-items: stretch;
    padding: 12px;
    gap: 12px;
  }

  .filters {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .search-input {
    width: 100%;
    min-width: 100%;
  }

  .toolbar-actions {
    width: 100%;
  }

  .toolbar-actions :deep(.el-button) {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .toolbar-container {
    padding: 10px;
    margin-bottom: 16px;
    border-radius: var(--radius-sm, 8px);
  }

  .filters {
    gap: 8px;
  }

  .filters :deep(.el-select) {
    width: 100% !important;
  }

  .filters :deep(.el-button) {
    width: 100%;
  }
}
</style>
