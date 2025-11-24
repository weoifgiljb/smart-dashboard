<template>
  <div ref="elRef" :style="{ height, width: '100%' }"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, computed } from 'vue'
import * as echarts from 'echarts'

interface Props {
  option: any
  height?: string
  autoresize?: boolean
}

const emit = defineEmits(['chart-click'])

const props = defineProps<Props>()
const elRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null
let ro: ResizeObserver | null = null
let io: IntersectionObserver | null = null

const height = computed(() => props.height || '220px')

const initChart = () => {
  if (!elRef.value) return
  if (chart) {
    chart.dispose()
    chart = null
  }
  chart = echarts.init(elRef.value)
  if (props.option) {
    chart.setOption(props.option)
  }

  chart.on('click', (params) => {
    emit('chart-click', params)
  })

  // 初始化后做一次异步 resize，避免在 display:none 场景下尺寸为 0
  requestAnimationFrame(() => handleResize())
  setTimeout(() => handleResize(), 120)
}

const handleResize = () => {
  if (!chart || !elRef.value) return
  const rect = elRef.value.getBoundingClientRect()
  // 兜底：若容器尚未渲染出有效宽高，延迟再试一次
  if (rect.width < 10 || rect.height < 10) {
    setTimeout(() => {
      const r2 = elRef.value?.getBoundingClientRect()
      if (r2 && r2.width >= 10 && r2.height >= 10) {
        chart?.resize({ width: r2.width, height: r2.height, animation: false as any })
      }
    }, 80)
    return
  }
  chart.resize({ width: rect.width, height: rect.height, animation: false as any })
}

onMounted(() => {
  initChart()
  if (props.autoresize !== false) {
    window.addEventListener('resize', handleResize)
    // Observe container size changes (e.g., shown inside tabs/dialogs)
    if ('ResizeObserver' in window && elRef.value) {
      ro = new ResizeObserver(() => {
        handleResize()
      })
      ro.observe(elRef.value)
    }
    // 当元素从不可见变为可见时，触发一次 resize
    if ('IntersectionObserver' in window && elRef.value) {
      io = new IntersectionObserver(
        (entries) => {
          entries.forEach((e) => {
            if (e.isIntersecting) {
              handleResize()
            }
          })
        },
        { threshold: 0.1 },
      )
      io.observe(elRef.value)
    }
  }
})

onBeforeUnmount(() => {
  if (props.autoresize !== false) {
    window.removeEventListener('resize', handleResize)
  }
  ro?.disconnect()
  ro = null
  io?.disconnect()
  io = null
  chart?.dispose()
  chart = null
})

watch(
  () => props.option,
  (val) => {
    if (!chart) {
      initChart()
      return
    }
    if (val) {
      chart.setOption(val, true)
      // 选项变化后立即根据容器实际尺寸强制自适应一次
      requestAnimationFrame(() => handleResize())
      setTimeout(() => handleResize(), 60)
    }
  },
  { deep: true },
)
</script>

<style scoped></style>
