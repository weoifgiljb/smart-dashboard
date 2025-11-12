<template>
  <div class="virtual-list" :style="{ height, overflowY: 'auto' }" @scroll="onScroll" ref="containerRef">
    <div :style="{ height: totalHeight + 'px', position: 'relative' }">
      <div
        v-for="(item, i) in visibleItems"
        :key="startIndex + i"
        :style="{
          position: 'absolute',
          top: (startIndex + i) * itemHeight + 'px',
          left: 0,
          right: 0,
          height: itemHeight + 'px'
        }"
      >
        <slot :item="item" :index="startIndex + i" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'

const props = withDefaults(defineProps<{
  items: any[],
  itemHeight: number,
  height?: string,
  overscan?: number
}>(), {
  height: '70vh',
  overscan: 5
})

const containerRef = ref<HTMLElement | null>(null)
const scrollTop = ref(0)
const totalHeight = computed(() => (props.items?.length || 0) * props.itemHeight)
const visibleCount = computed(() => {
  const h = containerRef.value?.clientHeight || 0
  return Math.ceil(h / props.itemHeight) + props.overscan
})
const startIndex = computed(() => Math.max(0, Math.floor(scrollTop.value / props.itemHeight) - props.overscan))
const endIndex = computed(() => Math.min((props.items?.length || 0), startIndex.value + visibleCount.value))
const visibleItems = computed(() => (props.items || []).slice(startIndex.value, endIndex.value))

const onScroll = (e: Event) => {
  const el = e.target as HTMLElement
  scrollTop.value = el.scrollTop
}

onMounted(() => {
  scrollTop.value = containerRef.value?.scrollTop || 0
})
</script>

<style scoped>
</style>


