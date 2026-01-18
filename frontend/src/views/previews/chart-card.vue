<template>
  <div class="preview-demo">
    <ChartCard
      title="示例图表"
      :option="chartOption"
      height="250px"
      tag-text="实时数据"
      tag-type="success"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import ChartCard from '@/components/dashboard/ChartCard.vue'

const chartOption = ref({
  grid: { left: 10, right: 10, top: 10, bottom: 20, containLabel: false },
  xAxis: {
    type: 'category',
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    axisTick: { show: false },
    axisLine: { show: false },
    axisLabel: { show: true, fontSize: 10, color: '#9ca3af' },
  },
  yAxis: {
    type: 'value',
    splitLine: { show: false },
    axisLabel: { show: false },
    axisLine: { show: false },
    axisTick: { show: false },
  },
  series: [
    {
      data: [12, 15, 18, 22, 28, 35, 42],
      type: 'line',
      smooth: true,
      symbolSize: 6,
      areaStyle: { color: 'rgba(59, 130, 246, 0.1)' },
      lineStyle: { color: '#3b82f6', width: 3 },
      itemStyle: { color: '#3b82f6', borderWidth: 2, borderColor: '#fff' },
    },
  ],
  tooltip: { trigger: 'axis' },
})
</script>

<style scoped>
.preview-demo {
  padding: 20px;
}
</style>
