import request from './request'

export const getDashboardStats = () => {
  return request.get('/dashboard/stats')
}

export const getTodayTasks = () => {
  return request.get('/dashboard/today-tasks')
}

export const getRecentActivities = () => {
  return request.get('/dashboard/recent-activities')
}

export const getTodayRhythm = () => {
  return request.get('/dashboard/rhythm')
}
