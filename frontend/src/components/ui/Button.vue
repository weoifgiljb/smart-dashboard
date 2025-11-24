<template>
  <el-button
    :type="type"
    :plain="plain"
    :round="round"
    :loading="loading"
    :link="link"
    @click="$emit('click', $event)"
  >
    <slot />
  </el-button>
</template>

<script setup lang="ts">
import { ElButton } from 'element-plus'

defineProps<{
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info' | ''
  plain?: boolean
  round?: boolean
  link?: boolean
  loading?: boolean
}>()

defineEmits<{ (e: 'click', evt: MouseEvent): void }>()
</script>

<style scoped></style>
