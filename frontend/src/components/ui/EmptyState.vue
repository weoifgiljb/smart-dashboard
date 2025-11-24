<template>
  <div class="empty-state">
    <el-icon class="icon"><i class="el-icon-remove-outline" /></el-icon>
    <div class="title">{{ title }}</div>
    <div v-if="$slots.action" class="action">
      <slot name="action" />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    title?: string
  }>(),
  {
    title: '暂无数据',
  },
)
</script>

<style scoped>
.empty-state {
  padding: 24px;
  text-align: center;
  color: var(--app-subtext);
}
.icon {
  font-size: 28px;
  opacity: 0.6;
}
.title {
  margin-top: 8px;
}
.action {
  margin-top: 12px;
}
</style>
