<template>
  <el-card shadow="never" :body-style="{ padding: bodyPadding }">
    <template v-if="$slots.header" #header>
      <div class="card-header">
        <slot name="header" />
      </div>
    </template>
    <slot />
  </el-card>
  </template>

<script setup lang="ts">
const props = withDefaults(defineProps<{
  bodyPadding?: string
}>(), {
  bodyPadding: '16px'
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>


