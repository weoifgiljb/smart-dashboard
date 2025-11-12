<template>
  <el-button
    :type="typeMap[variant] || type"
    :plain="plain"
    :round="round"
    :link="link"
    :disabled="disabled"
    :loading="loading"
    @click="$emit('click', $event)"
  >
    <slot />
  </el-button>
  </template>

<script setup lang="ts">
import { computed } from 'vue'

type Variant = 'primary' | 'secondary' | 'danger' | 'success' | 'warning' | 'info' | 'text'

const props = withDefaults(defineProps<{
  variant?: Variant
  type?: string
  plain?: boolean
  round?: boolean
  link?: boolean
  disabled?: boolean
  loading?: boolean
}>(), {
  variant: 'primary',
  type: '',
  plain: false,
  round: false,
  link: false,
  disabled: false,
  loading: false
})

defineEmits<{ (e: 'click', ev: MouseEvent): void }>()

const typeMap = computed<Record<string, any>>(() => ({
  primary: 'primary',
  secondary: 'info',
  danger: 'danger',
  success: 'success',
  warning: 'warning',
  info: 'info',
  text: ''
}))
</script>

<style scoped>
</style>


