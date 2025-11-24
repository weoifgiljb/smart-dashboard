import request from './request'

export const getWords = () => {
  return request.get('/words') as Promise<any>
}

export const addWord = (data: any) => {
  return request.post('/words', data) as Promise<any>
}

export const deleteWord = (id: string) => {
  return request.delete(`/words/${id}`) as Promise<any>
}

export const reviewWord = (id: string) => {
  return request.post(`/words/${id}/review`) as Promise<any>
}

export const getTodayWords = () => {
  return request.get('/words/today') as Promise<any>
}

export const importWords = (data: any) => {
  return request.post('/words/import', data) as Promise<any>
}

export const updateWordStatus = (id: string, status: 'todo' | 'done') => {
  return request.post(`/words/${id}/status`, { status }) as Promise<any>
}

export const getBooks = () => {
  return request.get('/words/books') as Promise<any>
}

export const importDefaultWords = () => {
  return request.post('/words/import-default', {}) as Promise<any>
}
