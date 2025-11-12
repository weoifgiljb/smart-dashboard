<template>
  <div class="checkin-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>每日打卡</h3>
        </div>
      </template>
      <div class="checkin-content">
        <el-button 
          type="primary" 
          size="large" 
          :disabled="hasCheckedIn"
          @click="handleCheckIn"
        >
          {{ hasCheckedIn ? '今日已打卡' : '立即打卡' }}
        </el-button>
        <div class="stats" style="margin-top: 20px">
          <el-statistic title="连续打卡天数" :value="consecutiveDays" />
          <el-statistic title="总打卡天数" :value="totalDays" />
          <el-statistic v-if="todayHeatValue > 0" title="今日热力值" :value="todayHeatValue" />
        </div>
        <div v-if="hasCheckedIn" class="heat-info" style="margin-top: 20px">
          <el-alert 
            :title="`✨ 今日热力值: ${todayHeatValue} | 构成: 打卡+${pomodoroCount}*番茄(×2)+${wordCount}*单词(×1)+${taskCount}*任务(×3)`"
            type="success"
            :closable="false"
          />
        </div>
      </div>
    </el-card>
    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <h3>年度打卡热力图</h3>
          <span style="font-size: 12px; color: #909399;">
            颜色深浅代表该天的热力值：基础分1 + 番茄×2 + 单词×1 + 任务×3
          </span>
        </div>
      </template>
      <BaseChart :option="calendarOption" height="240px" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { checkIn, getCheckInStats, getCheckInHistoryWithHeatValue } from '@/api/checkin'
import { getCalendarData } from '@/api/calendar'
import BaseChart from '@/components/charts/BaseChart.vue'

const hasCheckedIn = ref(false)
const consecutiveDays = ref(0)
const totalDays = ref(0)
const todayHeatValue = ref(0)
const pomodoroCount = ref(0)
const wordCount = ref(0)
const taskCount = ref(0)
const calendarOption = ref<any>({})

onMounted(async () => {
  await loadStats()
  await loadCalendar()
})

const loadStats = async () => {
  try {
    const stats: any = await getCheckInStats()
    consecutiveDays.value = stats.consecutiveDays
    totalDays.value = stats.totalDays
    hasCheckedIn.value = stats.hasCheckedInToday
    
    // 如果今天已打卡，获取今天的热力值详情
    if (hasCheckedIn.value) {
      await loadTodayHeatValue()
    }
  } catch (error) {
    console.error('获取打卡统计失败', error)
  }
}

const loadTodayHeatValue = async () => {
  try {
    const today = new Date()
    const todayKey = today.toISOString().split('T')[0]
    const params = {
      start: todayKey,
      end: todayKey
    }
    const calendarData: any = await getCalendarData(params)
    const dayData = calendarData[todayKey] || {}
    
    pomodoroCount.value = dayData.pomodoro || 0
    wordCount.value = dayData.word || 0
    taskCount.value = dayData.task || 0
    
    // 计算热力值：1 + 番茄*2 + 单词*1 + 任务*3
    todayHeatValue.value = 1 + pomodoroCount.value * 2 + wordCount.value * 1 + taskCount.value * 3
  } catch (error) {
    console.error('获取今日热力值失败', error)
  }
}

const loadCalendar = async () => {
  try {
    const response: any = await getCheckInHistoryWithHeatValue()
    const historyData = response.data || response
    
    // 找出最大热力值用于色阶映射
    let maxHeat = 10 // 最小色阶范围
    const data = (Array.isArray(historyData) ? historyData : []).map((c: any) => {
      const heatValue = c.heatValue || 0
      maxHeat = Math.max(maxHeat, heatValue)
      return [c.checkInDate, heatValue]
    })
    
    // 色阶配置：0值为浅色，maxHeat为深色
    const year = new Date().getFullYear()
    calendarOption.value = {
      tooltip: {
        formatter: (p: any) => {
          const date = p.data?.[0]
          const heat = p.data?.[1]
          return heat ? `${date}：热力值 ${heat}` : `${date}：未打卡`
        }
      },
      visualMap: {
        show: true,
        min: 0,
        max: maxHeat,
        inRange: {
          // 使用渐进色系：白 -> 浅蓝 -> 中蓝 -> 深蓝 -> 靛蓝
          color: ['#f0f5ff', '#d4e9ff', '#85c4ff', '#409eff', '#0052cc']
        },
        textStyle: {
          color: '#666'
        }
      },
      calendar: {
        range: `${year}`,
        cellSize: ['auto', 18],
        splitLine: { show: false },
        itemStyle: { borderWidth: 0.5, borderColor: '#f0f0f0' },
        yearLabel: { show: false },
        monthLabel: { nameMap: 'cn' },
        dayLabel: { nameMap: 'cn' }
      },
      series: [
        {
          type: 'heatmap',
          coordinateSystem: 'calendar',
          data
        }
      ]
    }
  } catch (error) {
    console.error('获取打卡历史失败', error)
  }
}

const handleCheckIn = async () => {
  try {
    await checkIn()
    ElMessage.success('打卡成功！')
    await loadStats()
    await loadCalendar()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '打卡失败')
  }
}
</script>

<style scoped>
.checkin-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  flex: 1;
}

.checkin-content {
  text-align: center;
}

.stats {
  display: flex;
  justify-content: space-around;
  margin-top: 30px;
  flex-wrap: wrap;
  gap: 20px;
}

.heat-info {
  animation: fadeIn 0.5s ease-in;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>





