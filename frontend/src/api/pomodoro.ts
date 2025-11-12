import request from './request'

export const startPomodoro = (data: any) => {
  return request.post('/pomodoro', data)
}

export const getPomodoroStats = () => {
  return request.get('/pomodoro/stats')
}

export const getPomodoroHistory = () => {
  return request.get('/pomodoro/history')
}


