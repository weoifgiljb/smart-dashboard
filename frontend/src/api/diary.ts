import request from './request'

export interface Diary {
  id?: string
  userId?: string
  content: string
  mood?: string
  tags?: string[]
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
