import { computed, type Ref, toRef } from 'vue'
import { useQuery, keepPreviousData } from '@tanstack/vue-query'
import { listTasks, aggregateKanban, aggregateStats, aggregateGantt } from '@/api/tasks'
import type { Task } from '@/types/task'
import { unwrapList } from './unwrap'

export interface TaskFilters {
  q?: string
  status?: string
  priority?: string
}

export interface TaskKanbanColumn {
  status: string
  items: Task[]
}

export interface TaskStats {
  byStatus: Record<string, number>
  byPriority: Record<string, number>
  overdue: number
}

export function useTasksQuery(filters: TaskFilters | Ref<TaskFilters>) {
  const filterRef = toRef(filters)
  const queryKey = computed(() => [
    'tasks',
    {
      q: filterRef.value.q || '',
      status: filterRef.value.status || '',
      priority: filterRef.value.priority || '',
    },
  ])

  return useQuery({
    queryKey,
    queryFn: async () => {
      const res = await listTasks({
        q: filterRef.value.q || '',
        status: filterRef.value.status || '',
        priority: filterRef.value.priority || '',
      })
      return unwrapList<Task>(res)
    },
    placeholderData: keepPreviousData,
  })
}

export function useTaskKanbanQuery(enabled: Ref<boolean>) {
  return useQuery({
    queryKey: ['tasks', 'aggregate', 'kanban'],
    queryFn: async () => unwrapList<TaskKanbanColumn>(await aggregateKanban()),
    enabled,
  })
}

export function useTaskStatsQuery(enabled: Ref<boolean>) {
  return useQuery({
    queryKey: ['tasks', 'aggregate', 'stats'],
    queryFn: async () => (await aggregateStats()) as unknown as TaskStats,
    enabled,
  })
}

export function useTaskGanttQuery(enabled: Ref<boolean>, start: Ref<string>, end: Ref<string>) {
  return useQuery({
    queryKey: computed(() => ['tasks', 'aggregate', 'gantt', start.value, end.value]),
    queryFn: async () => unwrapList<Task>(await aggregateGantt(start.value, end.value)),
    enabled,
  })
}
