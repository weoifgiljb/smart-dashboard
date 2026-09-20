import { afterEach, describe, expect, it } from 'vitest'
import { favoriteStorageKey, loadFavoriteIds, saveFavoriteIds, toggleFavoriteId } from '@/utils/bookFavorites'

describe('bookFavorites', () => {
  afterEach(() => {
    localStorage.clear()
  })

  it('scopes favorites to the current user and migrates the legacy key', () => {
    expect(favoriteStorageKey('u1')).toBe('favoriteBooks:u1')
    localStorage.setItem('favoriteBooks', JSON.stringify(['old']))
    expect(loadFavoriteIds('u1')).toEqual(['old'])
    expect(loadFavoriteIds('u1')).toEqual(['old'])
    saveFavoriteIds('u1', ['old', 'new'])
    expect(JSON.parse(String(localStorage.getItem('favoriteBooks:u1')))).toEqual(['old', 'new'])
  })

  it('keeps two users from sharing favorites', () => {
    saveFavoriteIds('u1', ['a'])
    saveFavoriteIds('u2', ['b'])
    expect(loadFavoriteIds('u1')).toEqual(['a'])
    expect(loadFavoriteIds('u2')).toEqual(['b'])
  })

  it('toggles an id in the set', () => {
    const ids = new Set<string>()
    expect(toggleFavoriteId(ids, 'b1')).toBe(true)
    expect(ids.has('b1')).toBe(true)
    expect(toggleFavoriteId(ids, 'b1')).toBe(false)
    expect(ids.has('b1')).toBe(false)
  })
})
