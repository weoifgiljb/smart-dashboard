<template>
  <el-card class="stat-chart-card">
    <template #header>
      <div class="card-header">
        <span class="chart-title">{{ title }}</span>
        <el-tag v-if="tagText" :type="tagType" size="small" effect="plain">
          {{ tagText }}
        </el-tag>
      </div>
    </template>
    <BaseChart :option="option" :height="height" @chart-click="handleClick" />
  </el-card>
</template>

<script setup lang="ts">
import BaseChart from '../charts/BaseChart.vue'
import type { EChartsOption } from 'echarts'

defineProps<{
  title: string
  option: EChartsOption
  height?: string
  tagText?: string
  tagType?: 'success' | 'info' | 'warning' | 'danger' | 'primary'
}>()

const emit = defineEmits<{
  chartClick: [params: any]
}>()

const handleClick = (params: any) => {
  emit('chartClick', params)
}
</script>

<style scoped>
.stat-chart-card {
  border: none;
  border-radius: var(--radius-md);
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--app-text);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .chart-title {
    font-size: 14px;
  }
}

@media (max-width: 480px) {
  .stat-chart-card {
    border-radius: var(--radius-sm, 8px);
  }

  .chart-title {
    font-size: 13px;
  }
}
</style>
