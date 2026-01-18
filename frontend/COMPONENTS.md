# Smart Dashboard 组件库

这是一个为 Smart Dashboard 项目开发的组件库，包含了经过精心设计和优化的可复用组件。

## 📦 安装和导入

### 批量导入

```typescript
import { KpiCard, ChartCard, StatusBadge } from '@/components'
```

### 单独导入

```typescript
import KpiCard from '@/components/dashboard/KpiCard.vue'
```

## 🎨 组件分类

### Dashboard 组件

用于仪表盘和数据展示的组件。

#### KpiCard - KPI 数据卡片

展示关键绩效指标的卡片组件，支持图标、数值、单位和标签自定义。

**Props:**

- `icon` (Component) - 图标组件
- `value` (number | string) - 显示的数值
- `unit` (string) - 单位文本
- `label` (string) - 标签文本
- `variant` ('streak' | 'words' | 'pomodoro' | 'total' | 'primary' | 'success' | 'warning' | 'info') - 样式变体

**示例:**

```vue
<KpiCard :icon="Trophy" :value="365" unit="天" label="连续打卡" variant="streak" />
```

**特性:**

- ✨ 悬停动画效果
- 🎨 8 种预设配色方案
- 📱 响应式设计

---

#### ChartCard - 图表卡片

包含标题、标签和图表的容器组件，基于 ECharts 封装。

**Props:**

- `title` (string) - 图表标题
- `option` (EChartsOption) - ECharts 配置对象
- `height` (string) - 图表高度，默认 '300px'
- `tagText` (string?) - 可选的标签文本
- `tagType` ('success' | 'info' | 'warning' | 'danger' | 'primary') - 标签类型

**Events:**

- `chartClick` - 图表点击事件，参数为 ECharts 事件参数

**示例:**

```vue
<ChartCard
  title="近30天热力值"
  :option="chartOption"
  height="200px"
  tag-text="趋势"
  tag-type="success"
  @chart-click="handleClick"
/>
```

---

#### TaskListItem - 任务列表项

展示任务信息的列表项组件，支持完成状态和自定义操作。

**Props:**

- `icon` (Component) - 图标组件
- `iconType` ('primary' | 'success' | 'warning' | 'danger' | 'info') - 图标类型
- `title` (string) - 任务标题
- `description` (string) - 任务描述
- `completed` (boolean) - 是否已完成，默认 false
- `statusTag` (object?) - 状态标签配置 `{ text, type?, effect? }`

**插槽:**

- `action` - 自定义操作区域

**示例:**

```vue
<TaskListItem
  :icon="Calendar"
  icon-type="success"
  title="每日打卡"
  description="记录今天的成长足迹"
  :completed="false"
  :status-tag="{ text: '去打卡', type: 'primary' }"
/>
```

**特性:**

- ✨ 悬停放大效果
- 🎯 完成状态视觉反馈
- 🎨 5 种图标颜色主题

---

#### ActivityItem - 活动列表项

展示用户活动的时间线组件，自动格式化相对时间。

**Props:**

- `icon` (Component) - 图标组件
- `title` (string) - 活动标题
- `time` (string | Date) - 活动时间
- `type` ('checkin' | 'pomodoro' | 'word' | 'task' | 'diary') - 活动类型

**示例:**

```vue
<ActivityItem :icon="Timer" title="完成了一个番茄钟" :time="new Date()" type="pomodoro" />
```

**特性:**

- ⏰ 自动时间格式化 (刚刚 / X分钟前 / X小时前 / X天前)
- 🎨 根据类型自动配色
- ✨ 悬停动画效果

---

### Common 组件

通用的可复用组件。

#### Toolbar - 工具栏

集成搜索、过滤器和操作按钮的工具栏组件。

**Props:**

- `modelValue` (string) - 搜索文本，支持 v-model
- `searchPlaceholder` (string) - 搜索框占位符，默认 '搜索...'
- `loading` (boolean) - 刷新按钮加载状态

**Events:**

- `update:modelValue` - 搜索文本更新
- `search` - 搜索触发（回车键或搜索按钮）
- `refresh` - 刷新按钮点击

**插槽:**

- `filters` - 过滤器区域
- `actions` - 操作按钮区域

**示例:**

```vue
<Toolbar
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
  <template #actions>
    <el-button type="primary">新建</el-button>
  </template>
</Toolbar>
```

---

#### ViewSwitcher - 视图切换器

支持图标和文字的视图切换组件。

**Props:**

- `modelValue` (string) - 当前选中的视图值，支持 v-model
- `views` (ViewOption[]) - 视图选项数组

**ViewOption 接口:**

```typescript
interface ViewOption {
  value: string
  label?: string
  icon?: Component
  disabled?: boolean
}
```

**示例:**

```vue
<ViewSwitcher
  v-model="currentView"
  :views="[
    { value: 'table', icon: Operation, label: '列表' },
    { value: 'kanban', icon: Grid, label: '看板' },
    { value: 'gantt', icon: Calendar, label: '甘特图' },
  ]"
/>
```

---

#### StatusBadge - 状态徽章

显示状态的徽章组件，带动态点动画效果。

**Props:**

- `status` ('todo' | 'in_progress' | 'done' | 'blocked' | 'pending' | 'active' | 'completed' | 'cancelled') - 状态类型
- `size` ('small' | 'default' | 'large') - 尺寸，默认 'default'
- `customText` (string?) - 自定义状态文本

**示例:**

```vue
<StatusBadge status="in_progress" size="default" />
<StatusBadge status="done" size="small" custom-text="已完成" />
```

**特性:**

- 🎯 8 种预设状态
- ✨ 进行中状态带脉冲动画
- 🎨 3 种尺寸选择

---

#### PriorityBadge - 优先级徽章

显示优先级的徽章组件，带图标指示器。

**Props:**

- `priority` ('low' | 'med' | 'high' | 'urgent') - 优先级类型
- `size` ('small' | 'default' | 'large') - 尺寸，默认 'default'
- `showIcon` (boolean) - 是否显示图标，默认 true
- `customText` (string?) - 自定义优先级文本

**示例:**

```vue
<PriorityBadge priority="urgent" size="default" />
<PriorityBadge priority="high" :show-icon="false" />
```

**特性:**

- 🚨 紧急优先级带脉冲动画
- ⬆️ 图标指示方向
- 🎨 4 种优先级配色

---

### UI 组件

基础 UI 组件库。

#### AppButton

Element Plus 按钮的封装组件。

#### AppCard

Element Plus 卡片的封装组件。

#### EmptyState

空状态占位组件。

---

### Charts 组件

#### BaseChart

ECharts 的 Vue 封装，支持响应式大小调整和交叉观察器。

**Props:**

- `option` (EChartsOption) - ECharts 配置
- `height` (string) - 图表高度

**Events:**

- `chart-click` - 图表点击事件

---

## 🎯 使用最佳实践

### 1. 组件组合

组件设计遵循组合优于继承的原则，鼓励灵活组合：

```vue
<el-card>
  <TaskListItem
    v-for="task in tasks"
    :key="task.id"
    :icon="task.icon"
    :title="task.title"
    :description="task.description"
  >
    <template #action>
      <StatusBadge :status="task.status" />
      <PriorityBadge :priority="task.priority" size="small" />
    </template>
  </TaskListItem>
</el-card>
```

### 2. 响应式设计

所有组件都支持响应式设计，在移动端会自动调整布局。

### 3. 主题定制

组件使用 CSS 变量，可以通过修改变量轻松定制主题：

```css
:root {
  --primary: #3b82f6;
  --success: #10b981;
  --warning: #f59e0b;
  --danger: #ef4444;
}
```

### 4. TypeScript 支持

所有组件都提供完整的 TypeScript 类型定义。

---

## 📖 在线演示

访问应用内的 `/plugins` 页面查看所有组件的实时演示和交互式文档。

---

## 🚀 开发指南

### 添加新组件

1. 在相应目录下创建组件文件
2. 在 `components/index.ts` 中导出
3. 在 `views/Plugins.vue` 中添加组件信息
4. 在 `views/previews/` 下创建预览组件

### 组件规范

- ✅ 使用 TypeScript 和 Composition API
- ✅ 提供 Props 类型定义
- ✅ 添加清晰的注释
- ✅ 包含使用示例
- ✅ 支持响应式设计
- ✅ 遵循无障碍标准

---

## 📄 许可证

本组件库是 Smart Dashboard 项目的一部分。

---

**最后更新:** 2026-01-18
