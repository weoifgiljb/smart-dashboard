import { type Ref, computed } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { getCalendarData, getCalendarDay } from '@/api/calendar'
import { getCheckInStats } from '@/api/checkin'

export function useCalendarDataQuery(start: Ref<string>, end: Ref<string>) {
  return useQuery({
    queryKey: computed(() => ['calendar', start.value, end.value]),
    queryFn: () => getCalendarData({ start: start.value, end: end.value }),
  })
}

export function useCalendarDayQuery(date: Ref<string>, enabled: Ref<boolean>) {
  return useQuery({
    queryKey: computed(() => ['calendar', 'day', date.value]),
    queryFn: () => getCalendarDay(date.value),
    enabled,
  })
}

export function useCheckInStatsQuery() {
  return useQuery({ queryKey: ['checkin', 'stats'], queryFn: getCheckInStats })
}
