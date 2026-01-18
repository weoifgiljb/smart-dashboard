<template>
  <div class="plugins-page">
    <!-- Hero Section -->
    <div class="hero-section">
      <div class="hero-content">
        <div class="hero-badge">
          <span class="badge-dot"></span>
          <span>组件库</span>
        </div>
        <h1 class="hero-title">
          <span class="gradient-text">Smart Dashboard</span>
          <br />
          组件展示
        </h1>
        <p class="hero-description">
          探索我们精心设计的可复用组件，每个组件都经过精心打磨，
          <br />
          为您的应用提供一致且优雅的用户体验
        </p>
        <div class="hero-stats">
          <div class="stat-item">
            <div class="stat-value">{{ totalComponents }}</div>
            <div class="stat-label">组件总数</div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-value">{{ categories.length }}</div>
            <div class="stat-label">分类</div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-value">100%</div>
            <div class="stat-label">TypeScript</div>
          </div>
        </div>
      </div>
      <div class="hero-decoration">
        <div class="decoration-circle decoration-1"></div>
        <div class="decoration-circle decoration-2"></div>
        <div class="decoration-circle decoration-3"></div>
      </div>
    </div>

    <!-- Category Filter -->
    <div class="category-filter">
      <el-button
        v-for="cat in categories"
        :key="cat.id"
        :type="selectedCategory === cat.id ? 'primary' : ''"
        :plain="selectedCategory !== cat.id"
        size="large"
        @click="selectedCategory = cat.id"
      >
        <el-icon class="cat-icon">
          <component :is="cat.icon" />
        </el-icon>
        {{ cat.name }}
        <el-tag size="small" :type="selectedCategory === cat.id ? 'info' : ''" effect="plain" round>
          {{ cat.count }}
        </el-tag>
      </el-button>
    </div>

    <!-- Components Grid -->
    <div class="components-section">
      <transition-group name="component-card" tag="div" class="components-grid">
        <div
          v-for="component in filteredComponents"
          :key="component.id"
          class="component-card"
          @click="showComponentDetail(component)"
        >
          <div class="card-header">
            <div class="component-icon" :class="`icon-${component.category}`">
              <el-icon>
                <component :is="component.icon" />
              </el-icon>
            </div>
            <el-tag size="small" :type="component.tagType" effect="plain">
              {{ component.tag }}
            </el-tag>
          </div>
          <h3 class="component-name">{{ component.name }}</h3>
          <p class="component-desc">{{ component.description }}</p>
          <div class="component-meta">
            <span class="meta-item">
              <el-icon><Document /></el-icon>
              {{ component.props }} props
            </span>
            <span class="meta-item">
              <el-icon><Connection /></el-icon>
              {{ component.events }} events
            </span>
          </div>
          <div class="card-footer">
            <el-button text type="primary" size="small">
              查看示例
              <el-icon class="el-icon--right"><ArrowRight /></el-icon>
            </el-button>
          </div>
        </div>
      </transition-group>
    </div>

    <!-- Component Detail Dialog -->
    <el-dialog
      v-model="detailVisible"
      :title="selectedComponent?.name"
      width="90%"
      align-center
      destroy-on-close
      class="component-dialog"
    >
      <div v-if="selectedComponent" class="component-detail">
        <!-- Component Info -->
        <div class="detail-header">
          <div class="detail-icon" :class="`icon-${selectedComponent.category}`">
            <el-icon size="32">
              <component :is="selectedComponent.icon" />
            </el-icon>
          </div>
          <div class="detail-info">
            <h2>{{ selectedComponent.name }}</h2>
            <p>{{ selectedComponent.description }}</p>
            <div class="detail-tags">
              <el-tag :type="selectedComponent.tagType" effect="plain">
                {{ selectedComponent.tag }}
              </el-tag>
              <el-tag type="info" effect="plain">
                {{ selectedComponent.category }}
              </el-tag>
            </div>
          </div>
        </div>

        <!-- Live Preview -->
        <div class="detail-section">
          <h3 class="section-title">
            <el-icon><View /></el-icon>
            实时预览
          </h3>
          <div class="preview-container">
            <component :is="getComponentPreview(selectedComponent.id)" />
          </div>
        </div>

        <!-- Usage -->
        <div class="detail-section">
          <h3 class="section-title">
            <el-icon><DocumentCopy /></el-icon>
            使用方法
          </h3>
          <div class="code-block">
            <pre><code>{{ selectedComponent.usage }}</code></pre>
          </div>
        </div>

        <!-- Props Documentation -->
        <div class="detail-section">
          <h3 class="section-title">
            <el-icon><List /></el-icon>
            Props
          </h3>
          <el-table :data="selectedComponent.propsDoc" style="width: 100%">
            <el-table-column prop="name" label="属性名" width="150" />
            <el-table-column prop="type" label="类型" width="200">
              <template #default="{ row }">
                <code class="inline-code">{{ row.type }}</code>
              </template>
            </el-table-column>
            <el-table-column prop="default" label="默认值" width="120">
              <template #default="{ row }">
                <code v-if="row.default" class="inline-code">{{ row.default }}</code>
                <span v-else class="text-muted">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="说明" />
          </el-table>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, defineAsyncComponent } from 'vue'
import {
  Grid,
  PieChart,
  Connection,
  Document,
  ArrowRight,
  View,
  DocumentCopy,
  List,
  TrendCharts,
  Bell,
  Flag,
  Tickets,
  Monitor,
} from '@element-plus/icons-vue'

// Component categories
const categories = [
  { id: 'all', name: '全部组件', icon: Grid, count: 0 },
  { id: 'dashboard', name: '仪表盘', icon: TrendCharts, count: 0 },
  { id: 'common', name: '通用组件', icon: Monitor, count: 0 },
  { id: 'ui', name: 'UI组件', icon: Tickets, count: 0 },
  { id: 'charts', name: '图表', icon: PieChart, count: 0 },
]

const selectedCategory = ref('all')
const detailVisible = ref(false)
const selectedComponent = ref<ComponentInfo | null>(null)

// Component definitions
interface ComponentInfo {
  id: string
  name: string
  description: string
  category: string
  icon: any
  tag: string
  tagType: 'success' | 'warning' | 'danger' | 'info' | 'primary'
  props: number
  events: number
  usage: string
  propsDoc: Array<{
    name: string
    type: string
    default?: string
    description: string
  }>
}

const components = ref<ComponentInfo[]>([
  {
    id: 'kpi-card',
    name: 'KpiCard',
    description: 'KPI 数据卡片，用于展示关键指标，支持图标、数值、单位和标签自定义',
    category: 'dashboard',
    icon: TrendCharts,
    tag: '数据展示',
    tagType: 'success',
    props: 5,
    events: 0,
    usage: `<KpiCard
  :icon="Trophy"
  :value="365"
  unit="天"
  label="连续打卡"
  variant="streak"
/>`,
    propsDoc: [
      { name: 'icon', type: 'Component', description: '图标组件' },
      { name: 'value', type: 'number | string', description: '显示的数值' },
      { name: 'unit', type: 'string', description: '单位文本' },
      { name: 'label', type: 'string', description: '标签文本' },
      {
        name: 'variant',
        type: "'streak' | 'words' | 'pomodoro' | 'total' | ...",
        default: 'primary',
        description: '样式变体，决定颜色主题',
      },
    ],
  },
  {
    id: 'chart-card',
    name: 'ChartCard',
    description: '图表卡片容器，包含标题、标签和 ECharts 图表，支持点击事件',
    category: 'dashboard',
    icon: PieChart,
    tag: '图表容器',
    tagType: 'primary',
    props: 5,
    events: 1,
    usage: `<ChartCard
  title="近30天热力值"
  :option="chartOption"
  height="200px"
  tag-text="趋势"
  tag-type="success"
  @chart-click="handleClick"
/>`,
    propsDoc: [
      { name: 'title', type: 'string', description: '图表标题' },
      { name: 'option', type: 'EChartsOption', description: 'ECharts 配置对象' },
      { name: 'height', type: 'string', default: '300px', description: '图表高度' },
      { name: 'tagText', type: 'string', description: '标签文本' },
      {
        name: 'tagType',
        type: "'success' | 'info' | 'warning' | 'danger' | 'primary'",
        default: 'primary',
        description: '标签类型',
      },
    ],
  },
  {
    id: 'task-list-item',
    name: 'TaskListItem',
    description: '任务列表项，展示任务图标、标题、描述和状态标签，支持完成状态样式',
    category: 'dashboard',
    icon: Tickets,
    tag: '列表项',
    tagType: 'warning',
    props: 6,
    events: 0,
    usage: `<TaskListItem
  :icon="Calendar"
  icon-type="success"
  title="每日打卡"
  description="记录今天的成长足迹"
  :completed="false"
  :status-tag="{ text: '已完成', type: 'success', effect: 'dark' }"
/>`,
    propsDoc: [
      { name: 'icon', type: 'Component', description: '图标组件' },
      {
        name: 'iconType',
        type: "'primary' | 'success' | 'warning' | 'danger' | 'info'",
        description: '图标类型，决定背景色',
      },
      { name: 'title', type: 'string', description: '任务标题' },
      { name: 'description', type: 'string', description: '任务描述' },
      { name: 'completed', type: 'boolean', default: 'false', description: '是否已完成' },
      {
        name: 'statusTag',
        type: '{ text: string, type?: string, effect?: string }',
        description: '状态标签配置',
      },
    ],
  },
  {
    id: 'activity-item',
    name: 'ActivityItem',
    description: '活动列表项，展示活动图标、标题和相对时间，自动格式化时间显示',
    category: 'dashboard',
    icon: Bell,
    tag: '活动流',
    tagType: 'info',
    props: 4,
    events: 0,
    usage: `<ActivityItem
  :icon="Timer"
  title="完成了一个番茄钟"
  :time="new Date()"
  type="pomodoro"
/>`,
    propsDoc: [
      { name: 'icon', type: 'Component', description: '图标组件' },
      { name: 'title', type: 'string', description: '活动标题' },
      { name: 'time', type: 'string | Date', description: '活动时间' },
      {
        name: 'type',
        type: "'checkin' | 'pomodoro' | 'word' | 'task' | 'diary'",
        description: '活动类型，决定图标颜色',
      },
    ],
  },
  {
    id: 'toolbar',
    name: 'Toolbar',
    description: '工具栏组件，集成搜索输入框、过滤器插槽和刷新按钮',
    category: 'common',
    icon: Monitor,
    tag: '工具栏',
    tagType: 'primary',
    props: 3,
    events: 3,
    usage: `<Toolbar
  v-model="searchText"
  search-placeholder="搜索任务..."
  :loading="isLoading"
  @search="handleSearch"
  @refresh="handleRefresh"
>
  <template #filters>
    <el-select v-model="status">
      <el-option label="待办" value="todo" />
    </el-select>
  </template>
</Toolbar>`,
    propsDoc: [
      { name: 'modelValue', type: 'string', default: "''", description: '搜索文本（v-model）' },
      {
        name: 'searchPlaceholder',
        type: 'string',
        default: "'搜索...'",
        description: '搜索框占位符',
      },
      { name: 'loading', type: 'boolean', default: 'false', description: '刷新按钮加载状态' },
    ],
  },
  {
    id: 'view-switcher',
    name: 'ViewSwitcher',
    description: '视图切换器，支持图标和文字标签，可禁用单个选项',
    category: 'common',
    icon: Grid,
    tag: '视图切换',
    tagType: 'warning',
    props: 2,
    events: 1,
    usage: `<ViewSwitcher
  v-model="currentView"
  :views="[
    { value: 'table', icon: Operation, label: '列表' },
    { value: 'kanban', icon: Grid, label: '看板' },
    { value: 'gantt', icon: Calendar, label: '甘特图', disabled: true }
  ]"
/>`,
    propsDoc: [
      { name: 'modelValue', type: 'string', description: '当前选中的视图值（v-model）' },
      {
        name: 'views',
        type: 'ViewOption[]',
        description: '视图选项数组，包含 value, label, icon, disabled',
      },
    ],
  },
  {
    id: 'status-badge',
    name: 'StatusBadge',
    description: '状态徽章，带状态点动画效果，支持多种预设状态和自定义文本',
    category: 'common',
    icon: Flag,
    tag: '状态',
    tagType: 'success',
    props: 3,
    events: 0,
    usage: `<StatusBadge
  status="in_progress"
  size="default"
  custom-text="进行中"
/>`,
    propsDoc: [
      {
        name: 'status',
        type: "'todo' | 'in_progress' | 'done' | 'blocked' | ...",
        description: '状态类型',
      },
      {
        name: 'size',
        type: "'small' | 'default' | 'large'",
        default: "'default'",
        description: '尺寸大小',
      },
      { name: 'customText', type: 'string', description: '自定义状态文本' },
    ],
  },
  {
    id: 'priority-badge',
    name: 'PriorityBadge',
    description: '优先级徽章，带图标指示器，紧急级别有脉冲动画效果',
    category: 'common',
    icon: Flag,
    tag: '优先级',
    tagType: 'danger',
    props: 4,
    events: 0,
    usage: `<PriorityBadge
  priority="urgent"
  size="default"
  :show-icon="true"
  custom-text="紧急"
/>`,
    propsDoc: [
      {
        name: 'priority',
        type: "'low' | 'med' | 'high' | 'urgent'",
        description: '优先级类型',
      },
      {
        name: 'size',
        type: "'small' | 'default' | 'large'",
        default: "'default'",
        description: '尺寸大小',
      },
      { name: 'showIcon', type: 'boolean', default: 'true', description: '是否显示图标' },
      { name: 'customText', type: 'string', description: '自定义优先级文本' },
    ],
  },
])

// Calculate component counts
const componentCounts = computed(() => {
  const counts: Record<string, number> = { all: components.value.length }
  components.value.forEach((comp) => {
    counts[comp.category] = (counts[comp.category] || 0) + 1
  })
  return counts
})

// Update category counts
categories.forEach((cat) => {
  cat.count = componentCounts.value[cat.id] || 0
})

const totalComponents = computed(() => components.value.length)

const filteredComponents = computed(() => {
  if (selectedCategory.value === 'all') {
    return components.value
  }
  return components.value.filter((c) => c.category === selectedCategory.value)
})

const showComponentDetail = (component: ComponentInfo) => {
  selectedComponent.value = component
  detailVisible.value = true
}

// Component preview loader
const getComponentPreview = (id: string) => {
  return defineAsyncComponent(() =>
    import(`./previews/${id}.vue`).catch(() => import('./previews/default.vue')),
  )
}
</script>

<style scoped>
.plugins-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 0;
}

/* Hero Section */
.hero-section {
  position: relative;
  padding: 80px 40px 60px;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.hero-content {
  max-width: 800px;
  margin: 0 auto;
  text-align: center;
  position: relative;
  z-index: 2;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  color: white;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 24px;
  animation: fadeInDown 0.6s ease;
}

.badge-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #10b981;
  animation: pulse 2s ease infinite;
}

.hero-title {
  font-size: 56px;
  font-weight: 800;
  color: white;
  margin: 0 0 24px 0;
  line-height: 1.2;
  animation: fadeInUp 0.6s ease 0.1s both;
}

.gradient-text {
  background: linear-gradient(135deg, #fff 0%, #a8b9ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-description {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.9);
  line-height: 1.8;
  margin: 0 0 40px 0;
  animation: fadeInUp 0.6s ease 0.2s both;
}

.hero-stats {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 32px;
  margin-top: 48px;
  animation: fadeInUp 0.6s ease 0.3s both;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 36px;
  font-weight: 800;
  color: white;
  line-height: 1;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: rgba(255, 255, 255, 0.3);
}

.hero-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
  z-index: 1;
  pointer-events: none;
}

.decoration-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
}

.decoration-1 {
  width: 400px;
  height: 400px;
  top: -200px;
  right: -100px;
  animation: float 20s ease-in-out infinite;
}

.decoration-2 {
  width: 300px;
  height: 300px;
  bottom: -100px;
  left: -50px;
  animation: float 15s ease-in-out infinite reverse;
}

.decoration-3 {
  width: 200px;
  height: 200px;
  top: 50%;
  right: 10%;
  animation: float 25s ease-in-out infinite;
}

/* Category Filter */
.category-filter {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding: 32px 40px;
  background: white;
  flex-wrap: wrap;
}

.cat-icon {
  margin-right: 6px;
}

/* Components Grid */
.components-section {
  padding: 40px;
  background: #f9fafb;
  min-height: 60vh;
}

.components-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.component-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
}

.component-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.15);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.component-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  transition: all 0.3s ease;
}

.component-card:hover .component-icon {
  transform: scale(1.1) rotate(5deg);
}

.icon-dashboard {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.icon-common {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.icon-ui {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: white;
}

.icon-charts {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
  color: white;
}

.component-name {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.component-desc {
  font-size: 14px;
  color: #6b7280;
  line-height: 1.6;
  margin: 0 0 16px 0;
  flex: 1;
}

.component-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  padding-top: 16px;
  border-top: 1px dashed #e5e7eb;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #9ca3af;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
}

/* Component Detail Dialog */
.component-detail {
  padding: 8px;
}

.detail-header {
  display: flex;
  gap: 24px;
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 2px solid #f3f4f6;
}

.detail-icon {
  width: 80px;
  height: 80px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.detail-info {
  flex: 1;
}

.detail-info h2 {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.detail-info p {
  font-size: 16px;
  color: #6b7280;
  line-height: 1.6;
  margin: 0 0 16px 0;
}

.detail-tags {
  display: flex;
  gap: 8px;
}

.detail-section {
  margin-bottom: 32px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 16px;
}

.preview-container {
  padding: 32px;
  background: #f9fafb;
  border-radius: 12px;
  border: 2px dashed #e5e7eb;
}

.code-block {
  background: #1f2937;
  border-radius: 12px;
  padding: 20px;
  overflow-x: auto;
}

.code-block pre {
  margin: 0;
  color: #e5e7eb;
  font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
}

.inline-code {
  background: #f3f4f6;
  color: #e11d48;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
  font-size: 13px;
}

.text-muted {
  color: #9ca3af;
}

/* Animations */
@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(5deg);
  }
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.component-card-enter-active,
.component-card-leave-active {
  transition: all 0.3s ease;
}

.component-card-enter-from {
  opacity: 0;
  transform: scale(0.9);
}

.component-card-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

/* Responsive */
@media (max-width: 1024px) {
  .components-grid {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 20px;
  }

  .component-card {
    padding: 20px;
  }
}

@media (max-width: 768px) {
  .hero-section {
    padding: 60px 24px 40px;
  }

  .hero-badge {
    font-size: 12px;
    padding: 5px 14px;
  }

  .hero-title {
    font-size: 36px;
  }

  .hero-description {
    font-size: 16px;
  }

  .hero-stats {
    flex-direction: column;
    gap: 20px;
    margin-top: 32px;
  }

  .stat-value {
    font-size: 32px;
  }

  .stat-divider {
    display: none;
  }

  .category-filter {
    padding: 24px 20px;
    gap: 8px;
  }

  .category-filter .el-button {
    font-size: 13px;
  }

  .cat-icon {
    margin-right: 4px;
  }

  .components-section {
    padding: 24px 20px;
  }

  .components-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .component-card {
    padding: 18px;
  }

  .component-name {
    font-size: 18px;
  }

  .component-desc {
    font-size: 13px;
  }

  /* Dialog adjustments */
  :deep(.component-dialog) {
    width: 95% !important;
  }

  .detail-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .detail-icon {
    width: 64px;
    height: 64px;
  }

  .detail-info h2 {
    font-size: 24px;
  }

  .preview-container {
    padding: 20px;
  }

  .code-block {
    padding: 15px;
    font-size: 12px;
  }
}

@media (max-width: 480px) {
  .hero-section {
    padding: 40px 16px 30px;
  }

  .hero-badge {
    font-size: 11px;
    padding: 4px 12px;
  }

  .hero-title {
    font-size: 28px;
  }

  .hero-description {
    font-size: 14px;
    line-height: 1.6;
  }

  .hero-stats {
    margin-top: 24px;
    gap: 16px;
  }

  .stat-value {
    font-size: 28px;
  }

  .stat-label {
    font-size: 13px;
  }

  .category-filter {
    padding: 16px;
    gap: 6px;
  }

  .category-filter .el-button {
    font-size: 12px;
    padding: 8px 12px;
  }

  .category-filter .el-button .el-tag {
    display: none;
  }

  .components-section {
    padding: 20px 16px;
  }

  .component-card {
    padding: 16px;
    border-radius: 12px;
  }

  .component-icon {
    width: 40px;
    height: 40px;
    font-size: 20px;
    border-radius: 10px;
  }

  .component-name {
    font-size: 16px;
  }

  .component-desc {
    font-size: 12px;
    margin-bottom: 12px;
  }

  .component-meta {
    font-size: 12px;
    gap: 12px;
    padding-top: 12px;
  }

  .meta-item {
    font-size: 12px;
  }

  .card-footer {
    margin-top: 8px;
  }

  /* Dialog adjustments */
  :deep(.component-dialog) {
    width: 98% !important;
  }

  .detail-icon {
    width: 56px;
    height: 56px;
  }

  .detail-info h2 {
    font-size: 20px;
  }

  .detail-info p {
    font-size: 14px;
  }

  .section-title {
    font-size: 16px;
  }

  .preview-container {
    padding: 16px;
  }

  .code-block {
    padding: 12px;
    font-size: 11px;
  }

  .code-block pre {
    font-size: 11px;
    line-height: 1.5;
  }

  /* Hide decoration circles on mobile for performance */
  .decoration-circle {
    display: none;
  }
}
</style>
