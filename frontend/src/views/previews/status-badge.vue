<template>
  <div class="preview-demo">
    <div class="badges-row">
      <StatusBadge status="todo" />
      <StatusBadge status="in_progress" />
      <StatusBadge status="done" />
      <StatusBadge status="blocked" />
    </div>
    <div class="badges-row">
      <StatusBadge status="todo" size="small" />
      <StatusBadge status="in_progress" size="small" />
      <StatusBadge status="done" size="small" />
      <StatusBadge status="blocked" size="small" />
    </div>
    <div class="badges-row">
      <StatusBadge status="todo" size="large" />
      <StatusBadge status="in_progress" size="large" />
      <StatusBadge status="done" size="large" />
      <StatusBadge status="blocked" size="large" />
    </div>
  </div>
</template>

<script setup lang="ts">
import StatusBadge from '@/components/common/StatusBadge.vue'
</script>

<style scoped>
.preview-demo {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.badges-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
</style>
