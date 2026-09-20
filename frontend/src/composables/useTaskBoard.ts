import { computed, nextTick, ref, watch } from 'vue'
import {
  createTask as apiCreate,
  updateTask as apiUpdate,
  deleteTask as apiDelete,
} from '@/api/tasks'
import type { Task } from '@/types/task'
import { useTasksStore } from '@/store/tasks'
import {
  useTaskGanttQuery,
  useTaskKanbanQuery,
  useTaskStatsQuery,
  useTasksQuery,
} from '@/api/hooks/useTasks'

export function useTaskBoard() {
  const store = useTasksStore()
  const q = ref(store.filters.q || '')
  const status = ref(store.filters.status || '')
  const priority = ref(store.filters.priority || '')
  const tab = ref<'table' | 'kanban' | 'gantt' | 'stats'>('table')

  const filters = computed(() => ({
    q: q.value,
    status: status.value,
    priority: priority.value,
  }))

  const { data, refetch } = useTasksQuery(filters)
  const kanbanEnabled = computed(() => tab.value === 'kanban')
  const statsEnabled = computed(() => tab.value === 'stats')
  const ganttEnabled = computed(() => tab.value === 'gantt')

  const now = new Date()
  const ganttStart = ref(`${now.getFullYear()}-01-01T00:00:00`)
  const ganttEnd = ref(`${now.getFullYear()}-12-31T23:59:59`)

  const { data: kanbanData } = useTaskKanbanQuery(kanbanEnabled)
  const { data: statsData } = useTaskStatsQuery(statsEnabled)
  const { data: ganttData } = useTaskGanttQuery(ganttEnabled, ganttStart, ganttEnd)

  const tasks = computed<Task[]>(() => data.value || [])
  const kanbanColumns = computed(() => kanbanData.value || [])
  const aggregateStats = computed(() => statsData.value)
  const ganttTasks = computed<Task[]>(
    () => ganttData.value || tasks.value.filter((x) => x.startDate && x.dueDate),
  )

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
    () => tasks.value.length,
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
    apiUpdate(t.id!, { status: s as Task['status'] }).then(refresh)
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
      startDate: t.startDate?.includes('T') ? t.startDate.slice(0, 10) : t.startDate || '',
      dueDate: t.dueDate?.includes('T') ? t.dueDate.slice(0, 10) : t.dueDate || '',
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
    const payload: Partial<Task> = { ...form.value }
    if (!payload.startDate) delete payload.startDate
    if (!payload.dueDate) delete payload.dueDate
    if (!payload.remindAt) delete payload.remindAt
    if (payload.startDate && /^\d{4}-\d{2}-\d{2}$/.test(payload.startDate)) {
      payload.startDate = `${payload.startDate}T00:00:00`
    }
    if (payload.dueDate && /^\d{4}-\d{2}-\d{2}$/.test(payload.dueDate)) {
      payload.dueDate = `${payload.dueDate}T00:00:00`
    }
    const action =
      isEdit.value && form.value.id ? apiUpdate(form.value.id, payload) : apiCreate(payload)
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
    const source = aggregateStats.value?.byStatus
    const ds = ['todo', 'in_progress', 'blocked', 'done'].map((s) => ({
      name:
        s === 'todo' ? '待办' : s === 'in_progress' ? '进行中' : s === 'blocked' ? '阻塞' : '完成',
      value: source ? Number(source[s] || 0) : tasks.value.filter((x) => x.status === s).length,
    }))
    return {
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      color: ['#94a3b8', '#3b82f6', '#ef4444', '#10b981'],
      series: [
        {
          type: 'pie',
          radius: ['50%', '70%'],
          avoidLabelOverlap: false,
          label: { show: false },
          data: ds,
        },
      ],
    }
  })

  const priorityPieOption = computed(() => {
    const source = aggregateStats.value?.byPriority
    const priorities = [
      { key: 'low', name: '低' },
      { key: 'med', name: '中' },
      { key: 'high', name: '高' },
      { key: 'urgent', name: '紧急' },
    ]
    const ds = priorities.map((p) => ({
      name: p.name,
      value: source
        ? Number(source[p.key] || 0)
        : tasks.value.filter((x) => x.priority === p.key).length,
    }))
    return {
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{ type: 'pie', radius: ['50%', '70%'], label: { show: false }, data: ds }],
    }
  })

  refresh()

  return {
    q,
    status,
    priority,
    tab,
    tasks,
    kanbanColumns,
    aggregateStats,
    ganttTasks,
    refreshTick,
    statusChartKey,
    priorityChartKey,
    dialogVisible,
    isEdit,
    form,
    columns,
    refresh,
    toggleTaskStatus,
    setStatus,
    handleCommand,
    isOverdue,
    openCreate,
    edit,
    remove,
    saveTask,
    nextStatus,
    priorityText,
    priorityType,
    statusType,
    statusPieOption,
    priorityPieOption,
  }
}
