<template>
  <div class="tasks-page">
    <el-card shadow="never" class="toolbar-card">
      <el-form :inline="true" class="toolbar-form">
        <el-form-item>
          <el-input v-model="q" placeholder="搜索标题/备注" clearable @keyup.enter="refresh" style="width: 220px" />
        </el-form-item>
        <el-form-item>
          <el-select v-model="status" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="全部状态" value="" />
            <el-option label="待办" value="todo" />
            <el-option label="进行中" value="in_progress" />
            <el-option label="阻塞" value="blocked" />
            <el-option label="已完成" value="done" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="priority" placeholder="全部优先级" clearable style="width: 140px">
            <el-option label="全部优先级" value="" />
            <el-option label="低" value="low" />
            <el-option label="中" value="med" />
            <el-option label="高" value="high" />
            <el-option label="紧急" value="urgent" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="refresh">查询</el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="success" @click="openCreate">新建任务</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-tabs v-model="tab" type="border-card">
        <el-tab-pane label="表格" name="table">
          <el-table :data="tasks" stripe border table-layout="auto">
            <template #empty>
              <EmptyState title="暂无任务，点击“新建任务”开始">
                <template #action>
                  <el-button type="primary" @click="openCreate">新建任务</el-button>
                </template>
              </EmptyState>
            </template>
            <el-table-column prop="title" label="标题" min-width="240" />
            <el-table-column label="优先级" width="120">
              <template #default="{ row }">
                <el-tag :type="priorityType(row.priority)" effect="light" disable-transitions>
                  {{ priorityText(row.priority) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="140">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" effect="plain" disable-transitions>
                  {{ statusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="截止" width="140">
              <template #default="{ row }">
                {{ row.dueDate ? row.dueDate.slice(0,10) : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="edit(row)">编辑</el-button>
                <el-button link type="info" @click="history(row)">历史</el-button>
                <el-button link type="warning" @click="share(row)">分享</el-button>
                <el-button link type="danger" @click="remove(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="看板" name="kanban">
          <div class="kanban">
            <div v-for="col in columns" :key="col.key" class="kanban-col">
              <div class="kanban-header">
                {{ col.label }}
                <span class="count">{{ tasks.filter(x=>x.status===col.key).length }}</span>
              </div>
              <div class="kanban-list">
                <el-card v-for="t in tasks.filter(x=>x.status===col.key)" :key="t.id" shadow="hover" class="kanban-card">
                  <template #header>
                    <div class="card-header">
                      <span class="title">{{ t.title }}</span>
                      <el-tag size="small" :type="priorityType(t.priority)">{{ priorityText(t.priority) }}</el-tag>
                    </div>
                  </template>
                  <div class="meta">
                    截止：{{ t.dueDate ? t.dueDate.slice(0,10) : '-' }}
                  </div>
                  <div class="actions">
                    <el-button size="small" @click="setStatus(t, nextStatus(col.key))">移动到 {{ statusText(nextStatus(col.key)) }}</el-button>
                  </div>
                </el-card>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="甘特" name="gantt">
          <div class="gantt">
            <el-empty v-if="tasks.filter(x=>x.startDate && x.dueDate).length===0" description="暂无带时间范围的任务" />
            <el-timeline v-else>
              <el-timeline-item v-for="t in tasks.filter(x=>x.startDate && x.dueDate)" :key="t.id"
                                :timestamp="t.startDate?.slice(0,10) + ' → ' + t.dueDate?.slice(0,10)"
                                :type="statusType(t.status)">
                {{ t.title }}
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-tab-pane>

        <el-tab-pane label="统计" name="stats">
          <!-- 顶部统计卡片 -->
          <div class="stats-overview">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-icon total">
                <el-icon><List /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ tasks.length }}</div>
                <div class="stat-label">总任务</div>
              </div>
            </el-card>
            <el-card shadow="hover" class="stat-card">
              <div class="stat-icon success">
                <el-icon><CircleCheck /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ tasks.filter(x=>x.status==='done').length }}</div>
                <div class="stat-label">已完成</div>
              </div>
            </el-card>
            <el-card shadow="hover" class="stat-card">
              <div class="stat-icon warning">
                <el-icon><Clock /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ tasks.filter(x=>x.status==='in_progress').length }}</div>
                <div class="stat-label">进行中</div>
              </div>
            </el-card>
            <el-card shadow="hover" class="stat-card">
              <div class="stat-icon danger">
                <el-icon><WarningFilled /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ tasks.filter(x=>x.dueDate && x.status!=='done' && new Date(x.dueDate) < new Date()).length }}</div>
                <div class="stat-label">逾期</div>
              </div>
            </el-card>
          </div>

          <!-- 图表区域 -->
          <div class="charts-grid">
            <!-- 状态分布 -->
            <el-card shadow="never" class="chart-card">
              <template #header>
                <div class="chart-header">
                  <span class="chart-title">状态分布</span>
                  <el-tag size="small" type="info">环形图</el-tag>
                </div>
              </template>
              <div class="chart-container">
                <BaseChart :key="statusChartKey" :option="statusPieOption" height="460px" />
              </div>
            </el-card>

            <!-- 优先级分布 -->
            <el-card shadow="never" class="chart-card">
              <template #header>
                <div class="chart-header">
                  <span class="chart-title">优先级分布</span>
                  <el-tag size="small" type="info">环形图</el-tag>
                </div>
              </template>
              <div class="chart-container">
                <BaseChart :key="priorityChartKey" :option="priorityPieOption" height="460px" />
              </div>
            </el-card>
          </div>

          <!-- 详细统计 -->
          <div class="detail-stats">
            <el-card shadow="never">
              <template #header>
                <div class="chart-header">
                  <span class="chart-title">按状态统计</span>
                </div>
              </template>
              <div class="stat-tags">
                <div v-for="s in ['todo','in_progress','blocked','done']" :key="s" class="stat-tag-item">
                  <el-tag :type="statusType(s)" effect="plain" size="large">
                    {{ statusText(s) }}
                  </el-tag>
                  <span class="stat-tag-count">{{ tasks.filter(x=>x.status===s).length }}</span>
                </div>
              </div>
            </el-card>

            <el-card shadow="never">
              <template #header>
                <div class="chart-header">
                  <span class="chart-title">按优先级统计</span>
                </div>
              </template>
              <div class="stat-tags">
                <div v-for="p in ['low','med','high','urgent']" :key="p" class="stat-tag-item">
                  <el-tag :type="priorityType(p)" effect="plain" size="large">
                    {{ priorityText(p) }}
                  </el-tag>
                  <span class="stat-tag-count">{{ tasks.filter(x=>x.priority===p).length }}</span>
                </div>
              </div>
            </el-card>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑任务' : '新建任务'" width="620px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入任务标题" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="可填写任务描述" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="优先级">
              <el-select v-model="form.priority" placeholder="请选择">
                <el-option label="低" value="low" />
                <el-option label="中" value="med" />
                <el-option label="高" value="high" />
                <el-option label="紧急" value="urgent" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" placeholder="请选择">
                <el-option label="待办" value="todo" />
                <el-option label="进行中" value="in_progress" />
                <el-option label="阻塞" value="blocked" />
                <el-option label="已完成" value="done" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="开始日期">
              <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="截止日期">
              <el-date-picker v-model="form.dueDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="预估(分钟)">
              <el-input-number v-model="form.estimateMinutes" :min="0" :step="15" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="提醒时间">
              <el-date-picker v-model="form.remindAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" />
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
            placeholder="输入后按回车新建标签"
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
import { ref, computed, nextTick, watch } from 'vue';
import { List, CircleCheck, Clock, WarningFilled } from '@element-plus/icons-vue';
import { createTask as apiCreate, updateTask as apiUpdate, deleteTask as apiDelete } from '../api/tasks';
import type { Task } from '../types/task';
import { useTasksStore } from '../store/tasks';
import EmptyState from '@/components/ui/EmptyState.vue';
import { useTasksQuery } from '@/api/hooks/useTasks';
import BaseChart from '@/components/charts/BaseChart.vue';

const store = useTasksStore();
const q = ref(store.filters.q || '');
const status = ref(store.filters.status || '');
const priority = ref(store.filters.priority || '');
const tab = ref<'table'|'kanban'|'gantt'|'stats'>('table');

const { data, refetch, isFetching } = useTasksQuery({ q: q.value, status: status.value, priority: priority.value } as any)
const tasks = computed(() => (data.value as any[]) || []);
const refreshTick = ref(0);
const statusChartKey = computed(() => `status-${refreshTick.value}`);
const priorityChartKey = computed(() => `priority-${refreshTick.value}`);

const dialogVisible = ref(false);
const isEdit = ref(false);
const form = ref<Task>({
  title: '',
  description: '',
  status: 'todo',
  priority: 'med',
  tags: [],
  startDate: '',
  dueDate: '',
  estimateMinutes: 0,
  remindAt: ''
});

const columns = [
  { key: 'todo', label: '待办' },
  { key: 'in_progress', label: '进行中' },
  { key: 'blocked', label: '阻塞' },
  { key: 'done', label: '已完成' },
] as const;

async function refresh() {
  store.setFilters({ q: q.value, status: status.value, priority: priority.value });
  try {
    await refetch();
  } finally {
    // 等待 DOM 更新后强制重建 + 自适应
    await nextTick();
    refreshTick.value++;
    requestAnimationFrame(() => window.dispatchEvent(new Event('resize')));
    setTimeout(() => window.dispatchEvent(new Event('resize')), 100);
  }
}

// 当任务数据数量变化时，保证图表跟随重建一次
watch(() => (tasks.value as any[]).length, () => {
  refreshTick.value++;
  requestAnimationFrame(() => window.dispatchEvent(new Event('resize')));
}, { flush: 'post' });

// 当切换到“统计”标签页时，强制重建并触发自适应，避免隐藏状态初始化导致尺寸过小
watch(tab, (val) => {
  if (val === 'stats') {
    nextTick().then(() => {
      refreshTick.value++;
      requestAnimationFrame(() => window.dispatchEvent(new Event('resize')));
      setTimeout(() => window.dispatchEvent(new Event('resize')), 80);
    });
  }
});

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
    remindAt: ''
  };
}
function openCreate() {
  isEdit.value = false;
  resetForm();
  dialogVisible.value = true;
}

function edit(t: Task) {
  isEdit.value = true;
  form.value = {
    ...t,
    startDate: t.startDate ? (t.startDate.includes('T') ? t.startDate.slice(0, 10) : t.startDate) : '',
    dueDate: t.dueDate ? (t.dueDate.includes('T') ? t.dueDate.slice(0, 10) : t.dueDate) : '',
    remindAt: t.remindAt ? t.remindAt.replace('T', ' ').slice(0, 19) : ''
  };
  dialogVisible.value = true;
}

function history(_t: Task) {
  window.alert('请在后续“历史抽屉”中查看（此处先占位）。');
}

function share(t: Task) {
  const uid = window.prompt('分享给用户ID');
  if (!uid) return;
  fetch(`/api/tasks/${t.id}/share?userId=${uid}&role=VIEW`, { method: 'POST' }).then(refresh);
}

function remove(t: Task) {
  if (!window.confirm('确认删除？')) return;
  apiDelete(t.id!).then(refresh);
}

function saveTask() {
  if (!form.value.title || !form.value.title.trim()) {
    return;
  }
  const payload: any = { ...form.value };
  if (payload.startDate === '') delete payload.startDate;
  if (payload.dueDate === '') delete payload.dueDate;
  if (payload.remindAt === '') delete payload.remindAt;
  if (typeof payload.startDate === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(payload.startDate)) {
    payload.startDate = `${payload.startDate}T00:00:00`;
  }
  if (typeof payload.dueDate === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(payload.dueDate)) {
    payload.dueDate = `${payload.dueDate}T00:00:00`;
  }
  if (typeof payload.remindAt === 'string' && /^\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}$/.test(payload.remindAt)) {
    payload.remindAt = payload.remindAt.replace(' ', 'T');
  }
  // 清理空字符串字段，避免后端 LocalDateTime 解析报错
  Object.keys(payload).forEach(k => {
    if (payload[k] === '') delete payload[k];
  });
  if (isEdit.value && form.value.id) {
    apiUpdate(form.value.id, payload).then(() => {
      dialogVisible.value = false;
      refresh();
    });
  } else {
    apiCreate(payload).then(() => {
      dialogVisible.value = false;
      refresh();
    });
  }
}

function nextStatus(s: string) {
  if (s === 'todo') return 'in_progress';
  if (s === 'in_progress') return 'blocked';
  if (s === 'blocked') return 'done';
  return 'done';
}

function setStatus(t: Task, s: string) {
  apiUpdate(t.id!, { status: s }).then(refresh);
}

function statusText(s?: string) {
  if (s === 'todo') return '待办';
  if (s === 'in_progress') return '进行中';
  if (s === 'blocked') return '阻塞';
  if (s === 'done') return '已完成';
  return '未知';
}
function statusType(s?: string) {
  if (s === 'todo') return '';
  if (s === 'in_progress') return 'warning';
  if (s === 'blocked') return 'danger';
  if (s === 'done') return 'success';
  return '';
}
function priorityText(p?: string) {
  if (p === 'low') return '低';
  if (p === 'med') return '中';
  if (p === 'high') return '高';
  if (p === 'urgent') return '紧急';
  return '未设';
}
function priorityType(p?: string) {
  if (p === 'low') return '';
  if (p === 'med') return 'info';
  if (p === 'high') return 'warning';
  if (p === 'urgent') return 'danger';
  return '';
}

const statusPieOption = computed(() => {
  const ds = ['todo','in_progress','blocked','done'].map(s => ({
    name: statusText(s),
    value: (tasks.value as any[]).filter(x => x.status === s).length
  }));

  const colors = ['#909399', '#409eff', '#f56c6c', '#67c23a'];

  return {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: { bottom: 0, left: 'center' },
    color: colors,
    series: [
      {
        type: 'pie',
        center: ['50%', '46%'],
        radius: ['60%', '80%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}\n{c} ({d}%)',
          fontSize: 13,
          fontWeight: 600,
          color: '#303133'
        },
        labelLine: {
          show: true,
          length: 12,
          length2: 10,
          smooth: true
        },
        emphasis: {
          scale: true,
          scaleSize: 10,
          itemStyle: {
            shadowBlur: 12,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.3)'
          }
        },
        data: ds
      }
    ]
  };
});

const priorityPieOption = computed(() => {
  const priorities = [
    { key: 'low', name: '低' },
    { key: 'med', name: '中' },
    { key: 'high', name: '高' },
    { key: 'urgent', name: '紧急' },
  ];
  const ds = priorities.map(p => ({
    name: p.name,
    value: (tasks.value as any[]).filter(x => x.priority === p.key).length
  }));
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, left: 'center' },
    series: [
      {
        type: 'pie',
        center: ['50%', '46%'],
        radius: ['60%', '80%'],
        label: { show: true, formatter: '{b}\n{c} ({d}%)', fontSize: 13 },
        labelLine: { length: 12, length2: 10 },
        data: ds
      }
    ]
  };
});

refresh();
</script>

<style scoped>
.tasks-page { padding: 12px; }
.toolbar-card { margin-bottom: 12px; }
.toolbar-form { display: flex; flex-wrap: wrap; gap: 8px; }

/* 看板样式 */
.kanban { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.kanban-col { background: #fafafa; border: 1px solid #eee; border-radius: 6px; min-height: 120px; }
.kanban-header { padding: 10px; font-weight: 600; border-bottom: 1px solid #eee; display: flex; justify-content: space-between; align-items: center; }
.kanban-header .count { color: #909399; font-size: 12px; }
.kanban-list { padding: 10px; display: flex; flex-direction: column; gap: 10px; }
.kanban-card .card-header { display: flex; align-items: center; justify-content: space-between; }
.kanban-card .title { font-weight: 600; }
.kanban-card .meta { color: #909399; font-size: 12px; margin-bottom: 6px; }
.kanban-card .actions { text-align: right; }

/* 甘特图样式 */
.gantt { padding: 8px 0; }

/* 统计页面样式 */
.stats-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border-radius: 12px;
  transition: all 0.3s;
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  flex-shrink: 0;
}

.stat-icon.total {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.stat-icon.success {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  color: #fff;
}

.stat-icon.warning {
  background: linear-gradient(135deg, #e6a23c 0%, #f0c78a 100%);
  color: #fff;
}

.stat-icon.danger {
  background: linear-gradient(135deg, #f56c6c 0%, #f89898 100%);
  color: #fff;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

/* 图表网格 */
.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.chart-card {
  border-radius: 12px;
  overflow: visible;
  min-height: 580px;
}

.chart-container {
  width: 100%;
  min-height: 480px;
  height: 480px;
  padding: 12px 12px 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

/* 详细统计 */
.detail-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.stat-tags {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  padding: 8px 0;
}

.stat-tag-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  transition: all 0.3s;
}

.stat-tag-item:hover {
  background: #e4e7ed;
  transform: translateX(4px);
}

.stat-tag-count {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}

/* 响应式 */
@media (max-width: 1400px) {
  .stats-overview {
    grid-template-columns: repeat(2, 1fr);
  }

  .charts-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1200px) {
  .kanban {
    grid-template-columns: repeat(2, 1fr);
  }

  .detail-stats {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-overview {
    grid-template-columns: 1fr;
  }

  .stat-card {
    padding: 16px;
  }

  .stat-icon {
    width: 48px;
    height: 48px;
    font-size: 24px;
  }

  .stat-value {
    font-size: 24px;
  }

  .stat-tags {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .kanban {
    grid-template-columns: 1fr;
  }
}
</style>


