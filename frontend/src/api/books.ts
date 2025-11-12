import request from './request'

/**
 * 获取推荐书籍（支持分页和排序）
 * @param page 页码（从0开始）
 * @param size 每页条数
 * @param sortBy 排序方式：rating|hot|new
 */
export const getBooks = (page: number = 0, size: number = 10, sortBy: string = 'rating') => {
  return request.get('/books', {
    params: { page, size, sortBy }
  })
}

/**
 * 获取所有推荐书籍（不分页）
 */
export const getAllBooks = () => {
  return request.get('/books/all')
}

/**
 * 按分类获取书籍
 */
export const getBooksByCategory = (category: string) => {
  return request.get(`/books/category/${category}`)
}

/**
 * 搜索书籍
 */
export const searchBooks = (keyword: string) => {
  return request.get('/books/search', {
    params: { keyword }
  })
}

/**
 * 根据ID获取书籍详情
 */
export const getBookById = (id: string) => {
  return request.get(`/books/${id}`)
}









