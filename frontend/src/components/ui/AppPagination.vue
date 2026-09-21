<template>
  <div v-if="visible" class="app-pagination">
    <el-pagination
      v-model:current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      :layout="layout"
      :background="background"
      :hide-on-single-page="hideOnSinglePage"
      @current-change="onChange"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    total: number
    pageSize?: number
    hideOnSinglePage?: boolean
    layout?: string
    background?: boolean
  }>(),
  {
    pageSize: 10,
    hideOnSinglePage: true,
    layout: 'total, prev, pager, next',
    background: true,
  },
)

const currentPage = defineModel<number>({ default: 1 })

const emit = defineEmits<{
  (e: 'change', page: number): void
}>()

const visible = computed(() => {
  if (props.total <= 0) return false
  if (props.hideOnSinglePage && props.total <= props.pageSize) return false
  return true
})

function onChange(page: number) {
  emit('change', page)
}
</script>

<style scoped lang="less">
.app-pagination {
  margin-top: var(--space-5);
  display: flex;
  justify-content: center;
}
</style>
