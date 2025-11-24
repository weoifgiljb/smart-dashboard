import request from './request'

export interface Diary {
  id?: string
  userId?: string
  content: string
  mood?: string
  tags?: string[]
  imageUrl?: string
  diaryDate: string // YYYY-MM-DD
  createdAt?: string
  updatedAt?: string
}

export const getDiaries = () => {
  return request.get<Diary[]>('/diaries')
}

export const saveDiary = (diary: Diary) => {
  return request.post<Diary>('/diaries', diary)
}

export const deleteDiary = (id: string) => {
  return request.delete(`/diaries/${id}`)
}

export const exportPdf = () => {
  return request.get('/diaries/export/pdf', { responseType: 'blob' })
}

export const exportWord = () => {
  return request.get('/diaries/export/word', { responseType: 'blob' })
}

export const matchMeme = (content: string) => {
  return request.post<string>('/diaries/match-meme', { content })
}
