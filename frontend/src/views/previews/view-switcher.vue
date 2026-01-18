<template>
  <div class="preview-demo">
    <ViewSwitcher v-model="currentView" :views="views" />
    <div style="margin-top: 24px; text-align: center; color: #6b7280">
      当前视图: {{ currentView }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import ViewSwitcher from '@/components/common/ViewSwitcher.vue'
import { Operation, Grid, Calendar, PieChart } from '@element-plus/icons-vue'

const currentView = ref('table')

const views = [
  { value: 'table', icon: Operation, label: '列表' },
  { value: 'kanban', icon: Grid, label: '看板' },
  { value: 'gantt', icon: Calendar, label: '甘特图' },
  { value: 'stats', icon: PieChart, label: '统计' },
]
</script>

<style scoped>
.preview-demo {
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
</style>
