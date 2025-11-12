import request from './request'

export const getCalendarData = (params?: { start?: string; end?: string }) => {
  return request.get('/calendar', { params })
}

export const getCalendarDay = (date: string) => {
  return request.get('/calendar/day', { params: { date } })
}











