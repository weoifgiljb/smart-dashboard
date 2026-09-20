<template>
  <div class="dashboard">
    <!-- Welcome & Actions Row -->
    <div class="welcome-section">
      <div class="welcome-text">
        <h2>自律即自由</h2>
        <p class="subtitle">每一天都是成长的契机。</p>
      </div>
      <div class="action-buttons">
        <el-button type="primary" size="large" class="action-btn" @click="router.push('/calendar')">
          <el-icon class="el-icon--left"><Calendar /></el-icon>
          立即打卡
        </el-button>
        <el-button type="success" size="large" class="action-btn" @click="router.push('/pomodoro')">
          <el-icon class="el-icon--left"><Timer /></el-icon>
          开始专注
        </el-button>
        <div class="date-filter">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="-"
            start-placeholder="开始"
            end-placeholder="结束"
            :shortcuts="shortcuts"
            size="default"
            style="width: 240px"
          />
        </div>
      </div>
    </div>

    <!-- KPI Cards -->
    <el-row :gutter="16" class="kpi-row">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="never" class="kpi-card">
          <div class="kpi-body">
            <div class="kpi-icon icon-streak">
              <el-icon><Trophy /></el-icon>
            </div>
            <div class="kpi-info">
              <div class="stat-value">{{ stats.checkInDays }} <span class="unit">天</span></div>
              <div class="stat-label">连续打卡</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="never" class="kpi-card">
          <div class="kpi-body">
            <div class="kpi-icon icon-words">
              <el-icon><Reading /></el-icon>
            </div>
            <div class="kpi-info">
              <div class="stat-value">{{ stats.wordCount }} <span class="unit">词</span></div>
              <div class="stat-label">已学单词</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="never" class="kpi-card">
          <div class="kpi-body">
            <div class="kpi-icon icon-pomodoro">
              <el-icon><Timer /></el-icon>
            </div>
            <div class="kpi-info">
              <div class="stat-value">{{ stats.pomodoroCount }} <span class="unit">个</span></div>
              <div class="stat-label">完成番茄</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="never" class="kpi-card">
          <div class="kpi-body">
            <div class="kpi-icon icon-total">
              <el-icon><DataLine /></el-icon>
            </div>
            <div class="kpi-info">
              <div class="stat-value">{{ stats.totalDays }} <span class="unit">天</span></div>
              <div class="stat-label">累计坚持</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Charts Row -->
    <el-row :gutter="20" class="chart-row">
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>近30天热力值</span>
              <el-tag size="small" effect="plain">趋势</el-tag>
            </div>
          </template>
          <BaseChart :option="heatValueOption" height="200px" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>番茄专注</span>
              <el-tag type="warning" size="small" effect="plain">近7天</el-tag>
            </div>
          </template>
          <BaseChart
            :option="pomodoroOption"
            height="200px"
            @chart-click="(p) => handleChartClick('pomodoro', p)"
          />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>单词积累</span>
              <el-tag type="success" size="small" effect="plain">近7天</el-tag>
            </div>
          </template>
          <BaseChart
            :option="wordsOption"
            height="200px"
            @chart-click="(p) => handleChartClick('word', p)"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- Bottom Row: Today Tasks & Recent Activity -->
    <el-row :gutter="20" class="list-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="list-card">
          <template #header>
            <div class="card-header">
              <span>今日任务</span>
              <el-button link type="primary" @click="router.push('/tasks')">查看全部</el-button>
            </div>
          </template>
          <div class="tasks-list">
            <div class="task-item" :class="{ completed: todayTasks.hasCheckedIn }">
              <div class="task-icon-wrapper">
                <el-icon><Calendar /></el-icon>
              </div>
              <div class="task-info">
                <span class="task-title">每日打卡</span>
                <span class="task-desc">记录今天的成长足迹</span>
              </div>
              <el-tag v-if="todayTasks.hasCheckedIn" type="success" size="small" effect="dark"
                >已完成</el-tag
              >
              <el-button v-else type="primary" link size="small" @click="router.push('/calendar')"
                >去打卡</el-button
              >
            </div>
            <div class="task-item">
              <div class="task-icon-wrapper">
                <el-icon><Reading /></el-icon>
              </div>
              <div class="task-info">
                <span class="task-title">学习单词</span>
                <span class="task-desc">今日需复习与新学单词</span>
              </div>
              <el-tag type="primary" size="small">{{ todayTasks.todayWordCount }} 个待学</el-tag>
            </div>
            <div class="task-item">
              <div class="task-icon-wrapper">
                <el-icon><Timer /></el-icon>
              </div>
              <div class="task-info">
                <span class="task-title">番茄专注</span>
                <span class="task-desc">保持高效工作节奏</span>
              </div>
              <el-tag type="warning" size="small"
                >今日 {{ todayTasks.todayPomodoroCount }} 个</el-tag
              >
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="list-card">
          <template #header>
            <div class="card-header">
              <span>最近活动</span>
            </div>
          </template>
          <div v-if="recentActivities.length > 0" class="activities-list">
            <div v-for="(activity, index) in recentActivities" :key="index" class="activity-item">
              <div class="activity-icon" :class="activity.type">
                <el-icon v-if="activity.type === 'checkin'"><Calendar /></el-icon>
                <el-icon v-else-if="activity.type === 'pomodoro'"><Timer /></el-icon>
                <el-icon v-else><Reading /></el-icon>
              </div>
              <div class="activity-content">
                <div class="activity-title">{{ activity.title }}</div>
                <div class="activity-time">{{ formatActivityTime(activity.time) }}</div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无活动" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>

    <!-- Dialogs -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" align-center>
      <el-table v-if="dialogType === 'pomodoro'" :data="dialogData" style="width: 100%" stripe>
        <el-table-column prop="startTime" label="开始时间" width="180">
          <template #default="{ row }">
            {{ row.startTime ? new Date(row.startTime).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="时长(分钟)" width="100">
          <template #default="{ row }">
            {{ Math.floor((row.duration || 0) / 60) }}
          </template>
        </el-table-column>
        <el-table-column prop="tag" label="标签">
          <template #default="{ row }">
            <el-tag v-if="row.tag" size="small">{{ row.tag }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
      <el-table v-else :data="dialogData" style="width: 100%" stripe>
        <el-table-column prop="word" label="单词" width="180">
          <template #default="{ row }">
            <span style="font-weight: bold; color: var(--color-primary)">{{ row.word }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="meaning" label="释义" />
        <el-table-column prop="createTime" label="添加时间" width="180">
          <template #default="{ row }">
            {{ row.createTime ? new Date(row.createTime).toLocaleString() : '-' }}
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Calendar, Reading, Timer, Trophy, DataLine } from '@element-plus/icons-vue'
import BaseChart from '@/components/charts/BaseChart.vue'
import { useDashboard } from '@/composables/useDashboard'

const router = useRouter()
const {
  stats,
  todayTasks,
  recentActivities,
  heatValueOption,
  pomodoroOption,
  wordsOption,
  dateRange,
  dialogVisible,
  dialogTitle,
  dialogData,
  dialogType,
  shortcuts,
  handleChartClick,
  formatActivityTime,
} = useDashboard()
</script>

<style scoped lang="less">
.dashboard {
  max-width: 1200px;
  margin: 0 auto;
}

.welcome-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--color-bg-muted);
}

.welcome-text h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 8px;
  letter-spacing: -0.5px;
}

.subtitle {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 14px;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 12px;
}

.action-btn {
  font-weight: 600;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.kpi-row {
  margin-bottom: 8px;
}

.chart-row,
.list-row {
  margin-top: 20px;
}

.kpi-card,
.chart-card,
.list-card {
  border: none;
  background: var(--color-bg-elevated);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.kpi-card :deep(.el-card__body) {
  padding: 16px 20px;
}

.kpi-body {
  display: flex;
  align-items: center;
  gap: 16px;
}

.kpi-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.icon-streak {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.icon-words {
  background: var(--color-info-soft);
  color: var(--color-info);
}

.icon-pomodoro {
  background: var(--color-warning-soft);
  color: var(--color-warning);
}

.icon-total {
  background: var(--color-success-soft);
  color: var(--color-success);
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  color: var(--color-text);
  line-height: 1.2;
}

.unit {
  font-size: 12px;
  font-weight: 400;
  color: var(--color-text-muted);
  margin-left: 2px;
}

.stat-label {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: var(--color-text);
}

.list-card {
  height: 100%;
}

.tasks-list,
.activities-list {
  padding: 4px 0;
}

.task-item {
  display: flex;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid var(--color-bg-muted);
}

.task-item:last-child {
  border-bottom: none;
}

.task-icon-wrapper {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-sm);
  background: var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  color: var(--color-text-secondary);
}

.task-info {
  flex: 1;
}

.task-title {
  display: block;
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: 4px;
}

.task-desc {
  font-size: 12px;
  color: var(--color-text-muted);
}

.task-item.completed {
  opacity: 0.6;
}

.activity-item {
  display: flex;
  align-items: flex-start;
  padding: 12px 0;
  border-bottom: 1px dashed var(--color-border);
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  flex-shrink: 0;
  font-size: 14px;
}

.activity-icon.checkin {
  background-color: var(--color-success-soft);
  color: var(--color-success);
}

.activity-icon.pomodoro {
  background-color: var(--color-warning-soft);
  color: var(--color-warning);
}

.activity-icon.word {
  background-color: var(--color-info-soft);
  color: var(--color-info);
}

.activity-content {
  flex: 1;
}

.activity-title {
  font-size: 14px;
  color: var(--color-text);
  margin-bottom: 4px;
}

.activity-time {
  font-size: 12px;
  color: var(--color-text-muted);
}

@media (max-width: 768px) {
  .welcome-section {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .action-buttons {
    width: 100%;
    flex-wrap: wrap;
  }

  .kpi-row,
  .chart-row,
  .list-row {
    :deep(.el-col) {
      margin-bottom: 16px;
    }
  }
}
</style>
