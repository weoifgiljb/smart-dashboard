<template>
  <el-card class="app-card" shadow="hover" :body-style="{ padding: bodyPadding }">
    <template v-if="$slots.header" #header>
      <div class="card-header">
        <slot name="header" />
      </div>
    </template>
    <slot />
  </el-card>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    bodyPadding?: string
  }>(),
  {
    bodyPadding: '20px',
  },
)
</script>

<style scoped>
.app-card {
  border: none;
  border-radius: var(--radius-md);
  background: var(--card-bg);
  transition: all 0.3s ease;
  margin-bottom: 16px;
}

.app-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: var(--app-text);
}

:deep(.el-card__header) {
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding: 16px 20px;
}
</style>
