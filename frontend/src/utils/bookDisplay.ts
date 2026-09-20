export enum BookEmptyKind {
  Catalog = 'catalog',
  Search = 'search',
  Favorites = 'favorites',
}

export enum BookSort {
  Random = 'random',
  Rating = 'rating',
  New = 'new',
  Hot = 'hot',
}

export function parseBookSort(value: string): BookSort {
  switch (value) {
    case BookSort.Random:
      return BookSort.Random
    case BookSort.Rating:
      return BookSort.Rating
    case BookSort.New:
      return BookSort.New
    case BookSort.Hot:
      return BookSort.Hot
    default:
      return BookSort.Random
  }
}

export function bookEmptyCopy(kind: BookEmptyKind) {
  switch (kind) {
    case BookEmptyKind.Catalog:
      return {
        description: '书架还是空的。随机推荐需要书库里先有书，可以先导入示例书架。',
        action: '导入书目',
      }
    case BookEmptyKind.Search:
      return {
        description: '没有找到相关书籍，换个词试试？',
        action: '清空搜索',
      }
    case BookEmptyKind.Favorites:
      return {
        description: '还没有收藏，去书架上点亮星星吧',
        action: '取消仅看收藏',
      }
    default: {
      const exhaustive: never = kind
      return exhaustive
    }
  }
}

export interface BookItem {
  id: string
  title: string
  author?: string
  cover?: string
  description?: string
  rating?: number
  category?: string
}

function asRecord(value: unknown) {
  if (value && typeof value === 'object') return value as Record<string, unknown>
  return null
}

function asString(value: unknown) {
  return typeof value === 'string' ? value : ''
}

function asNumber(value: unknown) {
  return typeof value === 'number' && Number.isFinite(value) ? value : undefined
}

export function normalizeBook(raw: unknown): BookItem | null {
  const rec = asRecord(raw)
  if (!rec) return null
  const id = asString(rec.id)
  if (!id) return null
  let title = asString(rec.title)
  let author = asString(rec.author)
  let cover = asString(rec.cover)
  const category = asString(rec.category)
  const description = asString(rec.description)
  if (title.endsWith('.jpg') || title.endsWith('.png')) {
    if (category.length > 20) title = category
  }
  if (author.startsWith('http') || author.includes('.jpg')) {
    if (!cover.startsWith('http')) cover = author
    author = ''
  }
  if (cover.includes('placeholder') || cover.includes('no-cover')) cover = ''
  return {
    id,
    title,
    author,
    cover,
    description,
    rating: asNumber(rec.rating),
    category,
  }
}

export function isDisplayableCover(cover?: string) {
  if (!cover) return false
  const value = cover.trim()
  if (!value) return false
  if (value.includes('placeholder') || value.includes('no-cover')) return false
  return true
}

export function parseBooksPayload(data: unknown) {
  if (Array.isArray(data)) {
    const books = data.map(normalizeBook).filter((item): item is BookItem => item !== null)
    return { books, total: books.length }
  }
  const rec = asRecord(data)
  if (rec && Array.isArray(rec.content)) {
    const books = rec.content.map(normalizeBook).filter((item): item is BookItem => item !== null)
    const total = asNumber(rec.totalElements)
    return { books, total: total ?? books.length }
  }
  return { books: [] as BookItem[], total: 0 }
}

export function readPayloadMessage(data: unknown, fallback: string) {
  if (data && typeof data === 'object' && 'message' in data) {
    const message = (data as { message?: unknown }).message
    if (typeof message === 'string' && message) return message
  }
  return fallback
}

export function resolveBookEmptyKind(input: {
  hasKeyword: boolean
  favoritesOnly: boolean
  count: number
}) {
  if (input.count > 0) return null
  if (input.favoritesOnly) return BookEmptyKind.Favorites
  if (input.hasKeyword) return BookEmptyKind.Search
  return BookEmptyKind.Catalog
}
