import { describe, expect, it } from 'vitest'
import {
  BookEmptyKind,
  BookSort,
  bookEmptyCopy,
  isDisplayableCover,
  normalizeBook,
  parseBookSort,
  parseBooksPayload,
  readPayloadMessage,
  resolveBookEmptyKind,
} from '@/utils/bookDisplay'

describe('bookDisplay', () => {
  it('uses catalog copy when random recommend is empty', () => {
    expect(resolveBookEmptyKind({ hasKeyword: false, favoritesOnly: false, count: 0 })).toBe(
      BookEmptyKind.Catalog,
    )
    expect(bookEmptyCopy(BookEmptyKind.Catalog).description).toContain('书架还是空的')
    expect(bookEmptyCopy(BookEmptyKind.Catalog).description).toContain('导入')
    expect(bookEmptyCopy(BookEmptyKind.Catalog).action).toBe('导入书目')
    expect(bookEmptyCopy(BookEmptyKind.Search).description).toContain('换个词试试')
    expect(bookEmptyCopy(BookEmptyKind.Favorites).description).toContain('还没有收藏')
    expect(bookEmptyCopy(BookEmptyKind.Favorites).action).toBe('取消仅看收藏')
  })

  it('prefers favorites empty over search empty', () => {
    expect(resolveBookEmptyKind({ hasKeyword: true, favoritesOnly: true, count: 0 })).toBe(
      BookEmptyKind.Favorites,
    )
    expect(resolveBookEmptyKind({ hasKeyword: true, favoritesOnly: false, count: 0 })).toBe(
      BookEmptyKind.Search,
    )
  })

  it('hides placeholder and no-cover images', () => {
    expect(isDisplayableCover('/no-cover.svg')).toBe(false)
    expect(isDisplayableCover('https://example.com/cover.jpg')).toBe(true)
    expect(normalizeBook({ id: 'b1', title: 'X', cover: '/no-cover.svg' })?.cover).toBe('')
  })

  it('parses random list and paged payload', () => {
    expect(parseBooksPayload([{ id: 'b1', title: 'Deep Work' }]).books[0]?.title).toBe('Deep Work')
    expect(
      parseBooksPayload({ content: [{ id: 'b1', title: 'Deep Work' }], totalElements: 9 }).total,
    ).toBe(9)
  })

  it('reads import payload message', () => {
    expect(readPayloadMessage({ message: '已放入 8 本示例书' }, 'fallback')).toBe(
      '已放入 8 本示例书',
    )
    expect(readPayloadMessage(null, 'fallback')).toBe('fallback')
  })

  it('keeps random as a real sort value', () => {
    expect(parseBookSort('random')).toBe(BookSort.Random)
    expect(parseBookSort('hot')).toBe(BookSort.Hot)
  })
})
