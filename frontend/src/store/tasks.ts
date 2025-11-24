import { defineStore } from 'pinia'
import type { Task } from '../types/task'

interface Filters {
  q?: string
  status?: string
  priority?: string
  tag?: string
}

export const useTasksStore = defineStore('tasks', {
  state: () => ({
    filters: JSON.parse(localStorage.getItem('task_filters') || '{}') as Filters,
    list: [] as Task[],
    loading: false,
  }),
  actions: {
    setFilters(f: Filters) {
      this.filters = { ...this.filters, ...f }
      localStorage.setItem('task_filters', JSON.stringify(this.filters))
    },
    setList(items: Task[]) {
      this.list = items
    },
    setLoading(v: boolean) {
      this.loading = v
    },
  },
})
