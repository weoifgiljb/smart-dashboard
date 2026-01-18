<template>
  <div class="preview-demo">
    <div style="max-width: 500px">
      <TaskListItem
        :icon="Calendar"
        icon-type="success"
        title="每日打卡"
        description="记录今天的成长足迹"
        :completed="false"
        :status-tag="{ text: '去打卡', type: 'primary' }"
      />
      <TaskListItem
        :icon="Reading"
        icon-type="primary"
        title="学习单词"
        description="今日需复习与新学单词"
        :status-tag="{ text: '12 个待学', type: 'info', effect: 'plain' }"
      />
      <TaskListItem
        :icon="Timer"
        icon-type="warning"
        title="番茄专注"
        description="保持高效工作节奏"
        :completed="true"
        :status-tag="{ text: '已完成', type: 'success', effect: 'dark' }"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import TaskListItem from '@/components/dashboard/TaskListItem.vue'
import { Calendar, Reading, Timer } from '@element-plus/icons-vue'
</script>

<style scoped>
.preview-demo {
  padding: 20px;
}
</style>
