<template>
  <div class="tasks-page">
    <div class="header-section">
      <div class="page-title">
        <h2>任务清单</h2>
      </div>
      <div class="header-actions">
        <el-button type="primary" size="large" @click="openCreate">
          <el-icon class="el-icon--left"><Plus /></el-icon>新建任务
        </el-button>
      </div>
    </div>

    <!-- 过滤器与视图切换 -->
    <div class="toolbar-container">
      <div class="filters">
        <el-input
          v-model="q"
          placeholder="搜索任务..."
          prefix-icon="Search"
          clearable
          class="search-input"
          @keyup.enter="refresh"
        />
        <el-select v-model="status" placeholder="状态" clearable class="filter-select">
          <el-option label="待办" value="todo" />
          <el-option label="进行中" value="in_progress" />
          <el-option label="阻塞" value="blocked" />
          <el-option label="已完成" value="done" />
        </el-select>
        <el-select v-model="priority" placeholder="优先级" clearable class="filter-select">
          <el-option label="低" value="low" />
          <el-option label="中" value="med" />
          <el-option label="高" value="high" />
          <el-option label="紧急" value="urgent" />
        </el-select>
        <el-button @click="refresh" circle><el-icon><RefreshRight /></el-icon></el-button>
      </div>
      
      <div class="view-switcher">
        <el-radio-group v-model="tab" size="default">
          <el-radio-button label="table"><el-icon><Operation /></el-icon></el-radio-button>
          <el-radio-button label="kanban"><el-icon><Grid /></el-icon></el-radio-button>
          <el-radio-button label="gantt"><el-icon><Calendar /></el-icon></el-radio-button>
          <el-radio-button label="stats"><el-icon><PieChart /></el-icon></el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div class="content-area">
      <transition name="fade" mode="out-in">
        <!-- 列表视图 -->
        <div v-if="tab === 'table'" key="table" class="view-container">
          <el-table :data="tasks" class="custom-table" :show-header="true" row-key="id">
            <template #empty>
              <EmptyState title="暂无任务，开始规划你的一天吧" />
            </template>
            
            <el-table-column width="48">
              <template #default="{ row }">
                <div 
                  class="custom-checkbox" 
                  :class="{ checked: row.status === 'done' }"
                  @click="toggleTaskStatus(row)"
                >
                  <el-icon v-if="row.status === 'done'"><Check /></el-icon>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="任务内容" min-width="300">
              <template #default="{ row }">
                <div class="task-cell-main">
                  <div class="task-title" :class="{ done: row.status === 'done' }">{{ row.title }}</div>
                  <div class="task-meta">
                    <span v-if="row.dueDate" class="meta-item" :class="{ overdue: isOverdue(row) }">
                      <el-icon><Calendar /></el-icon> {{ row.dueDate.slice(0, 10) }}
                    </span>
                    <el-tag v-for="tag in row.tags" :key="tag" size="small" type="info" effect="plain" class="meta-tag">
                      #{{ tag }}
                    </el-tag>
                  </div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="优先级" width="100" align="center">
              <template #default="{ row }">
                <div class="priority-indicator" :class="row.priority">
                  {{ priorityText(row.priority) }}
                </div>
              </template>
            </el-table-column>
            
            <el-table-column label="操作" width="180" align="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="edit(row)">编辑</el-button>
                <el-button link type="danger" @click="remove(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 看板视图 -->
        <div v-else-if="tab === 'kanban'" key="kanban" class="view-container kanban-view">
          <div v-for="col in columns" :key="col.key" class="kanban-column">
            <div class="column-header">
              <span class="col-title">{{ col.label }}</span>
              <span class="col-count">{{ tasks.filter((x) => x.status === col.key).length }}</span>
            </div>
            <div class="column-body">
              <div
                v-for="t in tasks.filter((x) => x.status === col.key)"
                :key="t.id"
                class="kanban-card"
                @click="edit(t)"
              >
                <div class="k-card-top">
                  <el-tag size="small" :type="priorityType(t.priority)" effect="dark" class="mini-tag">
                    {{ priorityText(t.priority) }}
                  </el-tag>
                  <el-dropdown trigger="click" @command="(c: any) => handleCommand(c, t)" @click.stop>
                    <el-icon class="more-btn"><MoreFilled /></el-icon>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="edit">编辑</el-dropdown-item>
                        <el-dropdown-item command="delete" divided style="color: var(--danger)">删除</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
                <div class="k-card-title">{{ t.title }}</div>
                <div class="k-card-footer">
                  <span v-if="t.dueDate" class="due-date" :class="{ overdue: isOverdue(t) }">
                    {{ t.dueDate.slice(5, 10) }}
                  </span>
                  <div class="status-actions" @click.stop>
                    <el-button 
                      v-if="col.key !== 'done'" 
                      size="small" 
                      circle 
                      icon="ArrowRight" 
                      @click="setStatus(t, nextStatus(col.key))" 
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 甘特视图 -->
        <div v-else-if="tab === 'gantt'" key="gantt" class="view-container gantt-view">
           <el-empty
              v-if="tasks.filter((x) => x.startDate && x.dueDate).length === 0"
              description="暂无带时间范围的任务"
            />
            <el-timeline v-else>
              <el-timeline-item
                v-for="t in tasks.filter((x) => x.startDate && x.dueDate)"
                :key="t.id"
                :timestamp="t.startDate?.slice(0, 10) + ' → ' + t.dueDate?.slice(0, 10)"
                :type="statusType(t.status)"
                placement="top"
              >
                <el-card shadow="hover" class="gantt-card">
                  <h4>{{ t.title }}</h4>
                  <p>{{ t.description || '无描述' }}</p>
                </el-card>
              </el-timeline-item>
            </el-timeline>
        </div>

        <!-- 统计视图 -->
        <div v-else-if="tab === 'stats'" key="stats" class="view-container stats-view">
          <!-- 统计概览 -->
          <div class="stats-overview">
            <div class="stat-box total">
              <div class="num">{{ tasks.length }}</div>
              <div class="txt">总任务</div>
            </div>
            <div class="stat-box done">
              <div class="num">{{ tasks.filter(x => x.status === 'done').length }}</div>
              <div class="txt">已完成</div>
            </div>
            <div class="stat-box progress">
              <div class="num">{{ tasks.filter(x => x.status === 'in_progress').length }}</div>
              <div class="txt">进行中</div>
            </div>
             <div class="stat-box urgent">
              <div class="num">{{ tasks.filter(x => x.priority === 'urgent' && x.status !== 'done').length }}</div>
              <div class="txt">紧急待办</div>
            </div>
          </div>

          <div class="charts-row">
             <div class="chart-box">
               <h3>状态分布</h3>
               <BaseChart :key="statusChartKey" :option="statusPieOption" height="300px" />
             </div>
             <div class="chart-box">
               <h3>优先级分布</h3>
               <BaseChart :key="priorityChartKey" :option="priorityPieOption" height="300px" />
             </div>
          </div>
        </div>
      </transition>
    </div>

    <!-- Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑任务' : '新建任务'"
      width="500px"
      align-center
      destroy-on-close
      class="task-dialog"
    >
      <el-form :model="form" label-position="top">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="要做什么？" size="large" />
        </el-form-item>
        
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="待办" value="todo" />
                <el-option label="进行中" value="in_progress" />
                <el-option label="阻塞" value="blocked" />
                <el-option label="已完成" value="done" />
              </el-select>
            </el-form-item>
          </el-col>
           <el-col :span="12">
            <el-form-item label="优先级">
              <el-select v-model="form.priority" style="width: 100%">
                <el-option label="低" value="low" />
                <el-option label="中" value="med" />
                <el-option label="高" value="high" />
                <el-option label="紧急" value="urgent" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="详细描述..."
          />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="截止日期">
              <el-date-picker
                v-model="form.dueDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
           <el-col :span="12">
            <el-form-item label="预估时间(分钟)">
              <el-input-number
                v-model="form.estimateMinutes"
                :min="0"
                :step="15"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
         <el-form-item label="标签">
          <el-select
            v-model="form.tags"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="添加标签"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTask">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch } from 'vue'
import { 
  Plus, RefreshRight, Operation, Grid, Calendar, PieChart, 
  Check, MoreFilled
} from '@element-plus/icons-vue'
import {
  createTask as apiCreate,
  updateTask as apiUpdate,
  deleteTask as apiDelete,
} from '../api/tasks'
import type { Task } from '../types/task'
import { useTasksStore } from '../store/tasks'
import EmptyState from '@/components/ui/EmptyState.vue'
import { useTasksQuery } from '@/api/hooks/useTasks'
import BaseChart from '@/components/charts/BaseChart.vue'

const store = useTasksStore()
const q = ref(store.filters.q || '')
const status = ref(store.filters.status || '')
const priority = ref(store.filters.priority || '')
const tab = ref<'table' | 'kanban' | 'gantt' | 'stats'>('table')

const { data, refetch } = useTasksQuery({
  q: q.value,
  status: status.value,
  priority: priority.value,
} as any)
const tasks = computed(() => (data.value as any[]) || [])
const refreshTick = ref(0)
const statusChartKey = computed(() => `status-${refreshTick.value}`)
const priorityChartKey = computed(() => `priority-${refreshTick.value}`)

const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref<Task>({
  title: '',
  description: '',
  status: 'todo',
  priority: 'med',
  tags: [],
  startDate: '',
  dueDate: '',
  estimateMinutes: 0,
  remindAt: '',
})

const columns = [
  { key: 'todo', label: '待办' },
  { key: 'in_progress', label: '进行中' },
  { key: 'blocked', label: '阻塞' },
  { key: 'done', label: '已完成' },
] as const

async function refresh() {
  store.setFilters({ q: q.value, status: status.value, priority: priority.value })
  try {
    await refetch()
  } finally {
    await nextTick()
    refreshTick.value++
    requestAnimationFrame(() => window.dispatchEvent(new Event('resize')))
  }
}

watch(
  () => (tasks.value as any[]).length,
  () => {
    refreshTick.value++
    requestAnimationFrame(() => window.dispatchEvent(new Event('resize')))
  },
  { flush: 'post' },
)

watch(tab, (val) => {
  if (val === 'stats') {
    nextTick().then(() => {
      refreshTick.value++
      requestAnimationFrame(() => window.dispatchEvent(new Event('resize')))
    })
  }
})

function toggleTaskStatus(row: Task) {
  const newStatus = row.status === 'done' ? 'todo' : 'done'
  apiUpdate(row.id!, { status: newStatus }).then(refresh)
}

function setStatus(t: Task, s: string) {
  apiUpdate(t.id!, { status: s }).then(refresh)
}

function handleCommand(command: string, t: Task) {
  if (command === 'edit') edit(t)
  if (command === 'delete') remove(t)
}

function isOverdue(t: Task) {
  if (!t.dueDate || t.status === 'done') return false
  return new Date(t.dueDate) < new Date()
}

function resetForm() {
  form.value = {
    title: '',
    description: '',
    status: 'todo',
    priority: 'med',
    tags: [],
    startDate: '',
    dueDate: '',
    estimateMinutes: 0,
    remindAt: '',
  }
}
function openCreate() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function edit(t: Task) {
  isEdit.value = true
  form.value = {
    ...t,
    startDate: t.startDate?.includes('T') ? t.startDate.slice(0, 10) : (t.startDate || ''),
    dueDate: t.dueDate?.includes('T') ? t.dueDate.slice(0, 10) : (t.dueDate || ''),
    remindAt: t.remindAt ? t.remindAt.replace('T', ' ').slice(0, 19) : '',
  }
  dialogVisible.value = true
}

function remove(t: Task) {
  if (!window.confirm('确认删除？')) return
  apiDelete(t.id!).then(refresh)
}

function saveTask() {
  if (!form.value.title || !form.value.title.trim()) return
  const payload: any = { ...form.value }
  // 清理字段
  if (!payload.startDate) delete payload.startDate
  if (!payload.dueDate) delete payload.dueDate
  if (!payload.remindAt) delete payload.remindAt
  
  if (payload.startDate && /^\d{4}-\d{2}-\d{2}$/.test(payload.startDate)) {
    payload.startDate = `${payload.startDate}T00:00:00`
  }
  if (payload.dueDate && /^\d{4}-\d{2}-\d{2}$/.test(payload.dueDate)) {
    payload.dueDate = `${payload.dueDate}T00:00:00`
  }

  const action = isEdit.value && form.value.id 
    ? apiUpdate(form.value.id, payload) 
    : apiCreate(payload)

  action.then(() => {
    dialogVisible.value = false
    refresh()
  })
}

function nextStatus(s: string) {
  if (s === 'todo') return 'in_progress'
  if (s === 'in_progress') return 'blocked'
  if (s === 'blocked') return 'done'
  return 'done'
}

function priorityText(p?: string) {
  if (p === 'low') return '低'
  if (p === 'med') return '中'
  if (p === 'high') return '高'
  if (p === 'urgent') return '紧急'
  return '-'
}
function priorityType(p?: string) {
  if (p === 'low') return 'info'
  if (p === 'med') return 'primary'
  if (p === 'high') return 'warning'
  if (p === 'urgent') return 'danger'
  return ''
}
function statusType(s?: string) {
  if (s === 'done') return 'success'
  if (s === 'in_progress') return 'warning'
  if (s === 'blocked') return 'danger'
  return 'primary'
}

const statusPieOption = computed(() => {
  const ds = ['todo', 'in_progress', 'blocked', 'done'].map((s) => ({
    name: s === 'todo' ? '待办' : s === 'in_progress' ? '进行中' : s === 'blocked' ? '阻塞' : '完成',
    value: (tasks.value as any[]).filter((x) => x.status === s).length,
  }))
  const colors = ['#94a3b8', '#3b82f6', '#ef4444', '#10b981']
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    color: colors,
    series: [{
      type: 'pie',
      radius: ['50%', '70%'],
      avoidLabelOverlap: false,
      label: { show: false },
      data: ds,
    }],
  }
})

const priorityPieOption = computed(() => {
  const priorities = [
    { key: 'low', name: '低' },
    { key: 'med', name: '中' },
    { key: 'high', name: '高' },
    { key: 'urgent', name: '紧急' },
  ]
  const ds = priorities.map((p) => ({
    name: p.name,
    value: (tasks.value as any[]).filter((x) => x.priority === p.key).length,
  }))
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['50%', '70%'],
      label: { show: false },
      data: ds,
    }],
  }
})

refresh()
</script>

<style scoped>
.tasks-page {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

/* Header */
.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.page-title h2 {
  font-size: 24px;
  font-weight: 700;
  color: var(--app-text);
  margin: 0;
}

/* Toolbar */
.toolbar-container {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.filters {
  display: flex;
  gap: 12px;
  flex: 1;
}

.search-input {
  width: 240px;
}
.filter-select {
  width: 120px;
}

/* Custom Table */
.custom-table {
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  --el-table-header-bg-color: #f8fafc;
}

.custom-checkbox {
  width: 20px;
  height: 20px;
  border: 2px solid var(--text-light);
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.custom-checkbox:hover {
  border-color: var(--primary);
}

.custom-checkbox.checked {
  background-color: var(--success);
  border-color: var(--success);
  color: white;
}

.task-cell-main {
  display: flex;
  flex-direction: column;
}

.task-title {
  font-weight: 500;
  color: var(--app-text);
  font-size: 14px;
}

.task-title.done {
  text-decoration: line-through;
  color: var(--text-light);
}

.task-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  flex-wrap: wrap;
}

.meta-item {
  font-size: 12px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 4px;
}

.meta-item.overdue {
  color: var(--danger);
  font-weight: 600;
}

.meta-tag {
  font-size: 11px;
}

.priority-indicator {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 12px;
  display: inline-block;
}

.priority-indicator.low { color: var(--text-secondary); background: var(--app-bg); }
.priority-indicator.med { color: var(--secondary); background: var(--secondary-light); }
.priority-indicator.high { color: var(--warning); background: var(--warning-light); }
.priority-indicator.urgent { color: var(--danger); background: var(--danger-light); }

/* Kanban View */
.kanban-view {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  overflow-x: auto;
  padding-bottom: 20px;
}

.kanban-column {
  background: #f1f5f9;
  border-radius: var(--radius-md);
  padding: 12px;
  min-width: 240px;
}

.column-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  font-size: 14px;
}

.col-count {
  background: rgba(0,0,0,0.05);
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

.kanban-card {
  background: white;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  cursor: pointer;
  transition: transform 0.2s;
}

.kanban-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 6px rgba(0,0,0,0.05);
}

.k-card-top {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.k-card-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 12px;
  line-height: 1.4;
  color: var(--app-text);
}

.k-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--text-light);
}

.due-date.overdue {
  color: var(--danger);
}

/* Stats View */
.stats-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-box {
  background: white;
  padding: 20px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  text-align: center;
}

.stat-box .num { font-size: 28px; font-weight: 800; }
.stat-box .txt { font-size: 12px; color: var(--text-secondary); margin-top: 4px; }
.stat-box.total .num { color: var(--app-text); }
.stat-box.done .num { color: var(--success); }
.stat-box.progress .num { color: var(--secondary); }
.stat-box.urgent .num { color: var(--danger); }

.charts-row {
  display: flex;
  gap: 24px;
}
.chart-box {
  flex: 1;
  background: white;
  border-radius: var(--radius-md);
  padding: 20px;
  border: 1px solid var(--border);
}
.chart-box h3 { margin: 0 0 20px 0; font-size: 16px; }

/* Responsive */
@media (max-width: 1024px) {
  .kanban-view {
    grid-template-columns: repeat(2, 1fr);
  }
  .charts-row { flex-direction: column; }
}

@media (max-width: 640px) {
  .kanban-view { grid-template-columns: 1fr; }
  .stats-overview { grid-template-columns: repeat(2, 1fr); }
  .filters { flex-direction: column; }
  .search-input { width: 100%; }
}
</style>