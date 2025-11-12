import { computed, toRefs } from 'vue'
import { useQuery, keepPreviousData } from '@tanstack/vue-query'
import { listTasks } from '@/api/tasks'

export interface TaskFilters {
  q?: string
  status?: string
  priority?: string
}

export function useTasksQuery(filters: TaskFilters) {
  const { q, status, priority } = toRefs(filters as any)
  const queryKey = computed(() => ['tasks', { q: q?.value || '', status: status?.value || '', priority: priority?.value || '' }])

  const query = useQuery({
    queryKey,
    queryFn: async () => {
      const res: any = await listTasks({
        q: q?.value || '',
        status: status?.value || '',
        priority: priority?.value || ''
      })
      return (res as any).data || (res as any) || []
    },
    placeholderData: keepPreviousData
  })

  return query
}


