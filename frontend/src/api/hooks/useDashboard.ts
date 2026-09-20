import { useQuery } from '@tanstack/vue-query'
import { getDashboardStats, getTodayTasks, getRecentActivities } from '@/api/dashboard'

export function useDashboardStatsQuery() {
  return useQuery({ queryKey: ['dashboard', 'stats'], queryFn: getDashboardStats })
}

export function useTodayTasksQuery() {
  return useQuery({ queryKey: ['dashboard', 'today-tasks'], queryFn: getTodayTasks })
}

export function useRecentActivitiesQuery() {
  return useQuery({ queryKey: ['dashboard', 'recent-activities'], queryFn: getRecentActivities })
}
