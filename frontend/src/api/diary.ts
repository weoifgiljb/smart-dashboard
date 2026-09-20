import request from './request'
import { DiaryMood } from '@/utils/diaryDisplay'

export interface Diary {
  id?: string
  userId?: string
  content: string
  mood?: DiaryMood | string
  tags?: string[]
  imageUrl?: string
  diaryDate: string
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

export const matchMeme = (content: string) => {
  return request.post<string>('/diaries/match-meme', { content })
}

export const exportPdf = () => {
  return request.get<Blob>('/diaries/export/pdf', { responseType: 'blob' })
}

export const exportWord = () => {
  return request.get<Blob>('/diaries/export/word', { responseType: 'blob' })
}

function jsonErrorMessage(text: string) {
  try {
    const parsed: unknown = JSON.parse(text)
    if (parsed && typeof parsed === 'object' && 'message' in parsed) {
      const fromApi = (parsed as { message?: unknown }).message
      if (typeof fromApi === 'string' && fromApi) return fromApi
    }
  } catch {
    // keep default
  }
  return '导出失败'
}

async function readBlobText(blob: Blob) {
  if (typeof blob.text === 'function') {
    return blob.text()
  }
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(reader.error || new Error('读取导出结果失败'))
    reader.readAsText(blob)
  })
}

export async function asDownloadBlob(data: unknown, fallbackType: string) {
  if (data instanceof Blob) {
    const looksJson = data.type.includes('json')
    const text = await readBlobText(data)
    if (looksJson || text.trimStart().startsWith('{') || text.trimStart().startsWith('[')) {
      throw new Error(jsonErrorMessage(text))
    }
    return data
  }
  return new Blob([String(data)], { type: fallbackType })
}

export function triggerDownload(blob: Blob, filename: string) {
  const link = document.createElement('a')
  const href = window.URL.createObjectURL(blob)
  link.href = href
  link.download = filename
  link.click()
  window.URL.revokeObjectURL(href)
}
