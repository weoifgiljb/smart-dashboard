import { afterEach, describe, expect, it } from 'vitest'
import {
  ACCESS_STORAGE_KEY,
  REFRESH_STORAGE_KEY,
  clearAuthTokens,
  getAccessToken,
  getRefreshToken,
  hydrateAuthTokens,
  setAuthTokens,
} from '@/api/authTokens'

describe('authTokens session restore', () => {
  afterEach(() => {
    clearAuthTokens()
  })

  it('rehydrates tokens from sessionStorage after a reload', () => {
    sessionStorage.setItem(ACCESS_STORAGE_KEY, 'access-from-session')
    sessionStorage.setItem(REFRESH_STORAGE_KEY, 'refresh-from-session')
    hydrateAuthTokens()
    expect(getAccessToken()).toBe('access-from-session')
    expect(getRefreshToken()).toBe('refresh-from-session')
  })
})
