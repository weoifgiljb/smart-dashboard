<template>
  <div class="preview-demo">
    <div style="max-width: 400px">
      <ActivityItem
        :icon="Calendar"
        title="打卡成功"
        :time="new Date(Date.now() - 5 * 60000)"
        type="checkin"
      />
      <ActivityItem
        :icon="Timer"
        title="完成了一个番茄钟"
        :time="new Date(Date.now() - 120 * 60000)"
        type="pomodoro"
      />
      <ActivityItem
        :icon="Reading"
        title="学习了10个新单词"
        :time="new Date(Date.now() - 3 * 3600000)"
        type="word"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import ActivityItem from '@/components/dashboard/ActivityItem.vue'
import { Calendar, Timer, Reading } from '@element-plus/icons-vue'
</script>

<style scoped>
.preview-demo {
  padding: 20px;
}
</style>
