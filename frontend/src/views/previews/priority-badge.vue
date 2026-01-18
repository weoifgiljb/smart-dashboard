<template>
  <div class="preview-demo">
    <div class="badges-row">
      <PriorityBadge priority="low" />
      <PriorityBadge priority="med" />
      <PriorityBadge priority="high" />
      <PriorityBadge priority="urgent" />
    </div>
    <div class="badges-row">
      <PriorityBadge priority="low" size="small" />
      <PriorityBadge priority="med" size="small" />
      <PriorityBadge priority="high" size="small" />
      <PriorityBadge priority="urgent" size="small" />
    </div>
    <div class="badges-row">
      <PriorityBadge priority="low" :show-icon="false" />
      <PriorityBadge priority="med" :show-icon="false" />
      <PriorityBadge priority="high" :show-icon="false" />
      <PriorityBadge priority="urgent" :show-icon="false" />
    </div>
  </div>
</template>

<script setup lang="ts">
import PriorityBadge from '@/components/common/PriorityBadge.vue'
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
