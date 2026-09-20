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

<style scoped lang="less">
.app-card {
  border: none;
  border-radius: var(--radius-md);
  background: var(--color-bg-elevated);
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: var(--color-text);
}

:deep(.el-card__header) {
  border-bottom: 1px solid var(--color-border);
  padding: 16px 20px;
}
</style>
