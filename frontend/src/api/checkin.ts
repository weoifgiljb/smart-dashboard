import request from './request'

export const checkIn = () => {
  return request.post('/checkin')
}

export const getCheckInStats = () => {
  return request.get('/checkin/stats')
}

export const getCheckInHistory = (params: { page?: number; size?: number }) => {
  return request.get('/checkin/history', { params })
}

// 获取打卡历史及热力值信息
export const getCheckInHistoryWithHeatValue = () => {
  return request.get('/checkin/history-with-heatvalue')
}
