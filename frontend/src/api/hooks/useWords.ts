import { useQuery } from '@tanstack/vue-query'
import { getTodayWords, getWords } from '@/api/words'

export function useWordsQuery() {
  return useQuery({ queryKey: ['words'], queryFn: getWords })
}

export function useTodayWordsQuery() {
  return useQuery({ queryKey: ['words', 'today'], queryFn: getTodayWords })
}
