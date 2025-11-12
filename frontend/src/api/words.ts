import request from './request'

export const getWords = () => {
  return request.get('/words')
}

export const addWord = (data: any) => {
  return request.post('/words', data)
}

export const deleteWord = (id: string) => {
  return request.delete(`/words/${id}`)
}

export const reviewWord = (id: string) => {
  return request.post(`/words/${id}/review`)
}

export const getTodayWords = () => {
  return request.get('/words/today')
}

export const importWords = (data: any) => {
  return request.post('/words/import', data)
}

export const updateWordStatus = (id: string, status: 'todo' | 'done') => {
  return request.post(`/words/${id}/status`, { status })
}

export const getBooks = () => {
  return request.get('/words/books')
}

export const importDefaultWords = () => {
  return request.post('/words/import-default', {})
}







