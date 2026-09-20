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
    <el-row :gutter="20" class="kpi-row">
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card">
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
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card">
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
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card">
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
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card">
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
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="8">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>近30天热力值</span>
              <el-tag size="small" effect="plain">趋势</el-tag>
            </div>
          </template>
          <BaseChart :option="heatValueOption" height="200px" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="chart-card">
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
      <el-col :span="8">
        <el-card class="chart-card">
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
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card class="list-card">
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
      <el-col :span="12">
        <el-card class="list-card">
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
            <span style="font-weight: bold; color: var(--primary)">{{ row.word }}</span>
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

<style scoped>
.dashboard {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

/* Welcome Section */
.welcome-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border);
}

.welcome-text h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--app-text);
  margin: 0 0 8px 0;
  letter-spacing: -0.5px;
}

.subtitle {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 12px;
}

.action-btn {
  font-weight: 600;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  transition: transform 0.2s;
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

/* KPI Cards */
.kpi-row {
  margin-bottom: 24px;
}

.kpi-card {
  border: none;
  background: var(--card-bg);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.kpi-body {
  display: flex;
  align-items: center;
  padding: 16px 8px;
}

.kpi-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  font-size: 24px;
  flex-shrink: 0;
}

.icon-streak {
  background: #fff1f2;
  color: #e11d48;
}

.icon-words {
  background: #eff6ff;
  color: #2563eb;
}

.icon-pomodoro {
  background: #fff7ed;
  color: #ea580c;
}

.icon-total {
  background: #f0fdf4;
  color: #16a34a;
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  color: var(--app-text);
  line-height: 1.2;
}

.unit {
  font-size: 12px;
  font-weight: normal;
  color: var(--text-light);
  margin-left: 2px;
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
}

/* Charts */
.chart-card {
  border: none;
  border-radius: var(--radius-md);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* Lists */
.list-card {
  border: none;
  border-radius: var(--radius-md);
  height: 100%;
}

.tasks-list,
.activities-list {
  padding: 8px 0;
}

.task-item {
  display: flex;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid var(--border);
}

.task-item:last-child {
  border-bottom: none;
}

.task-icon-wrapper {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: var(--app-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  color: var(--text-secondary);
}

.task-info {
  flex: 1;
}

.task-title {
  display: block;
  font-size: 15px;
  font-weight: 500;
  color: var(--app-text);
  margin-bottom: 4px;
}

.task-desc {
  font-size: 12px;
  color: var(--text-light);
}

.task-item.completed {
  opacity: 0.6;
}

.activity-item {
  display: flex;
  align-items: flex-start;
  padding: 12px 0;
  border-bottom: 1px dashed var(--el-border-color-lighter);
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
  background-color: var(--success-light);
  color: var(--success);
}

.activity-icon.pomodoro {
  background-color: var(--warning-light);
  color: var(--warning);
}

.activity-icon.word {
  background-color: var(--info-light);
  color: var(--info);
}

.activity-content {
  flex: 1;
}

.activity-title {
  font-size: 14px;
  color: var(--app-text);
  margin-bottom: 4px;
}

.activity-time {
  font-size: 12px;
  color: var(--text-light);
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

  .el-col {
    width: 100% !important;
    margin-bottom: 16px;
  }
}
</style>
