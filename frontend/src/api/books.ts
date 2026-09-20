import request from './request'
import { BookSort } from '@/utils/bookDisplay'

export const getBooks = (page = 0, size = 10, sortBy: BookSort = BookSort.Rating) => {
  if (sortBy === BookSort.Random) {
    return request.get('/books/random', {
      params: { limit: size },
    })
  }
  return request.get('/books', {
    params: { page, size, sortBy },
  })
}

export const getRandomBooks = (limit = 12) => {
  return request.get('/books/random', {
    params: { limit },
  })
}

export const getBooksByIds = (ids: string[]) => {
  if (!ids.length) return Promise.resolve([])
  return request.get('/books/by-ids', {
    params: { ids: ids.join(',') },
  })
}

export const getAllBooks = () => {
  return request.get('/books/all')
}

export const getBooksByCategory = (category: string) => {
  return request.get(`/books/category/${category}`)
}

export const searchBooks = (keyword: string) => {
  return request.get('/books/search', {
    params: { keyword },
  })
}

export const getBookById = (id: string) => {
  return request.get(`/books/${id}`)
}

export const importBooks = (csvUrl: string, limit = 24) => {
  return request.post('/books/import', { csvUrl, limit }, { timeout: 60000 })
}

export const importSampleBooks = () => {
  return request.post('/books/import/sample')
}
