import { afterEach, describe, expect, it, vi } from 'vitest'
import axios from 'axios'
import { refreshAuthToken } from '@/api/authRefresh'
import { clearAuthTokens, getAccessToken, getRefreshToken, setAuthTokens } from '@/api/authTokens'

vi.mock('axios', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('refreshAuthToken', () => {
  afterEach(() => {
    clearAuthTokens()
    vi.resetAllMocks()
  })

  it('still posts refresh with credentials when memory token is empty', async () => {
    vi.mocked(axios.post).mockRejectedValue(new Error('401'))
    await expect(refreshAuthToken()).resolves.toBeNull()
    expect(axios.post).toHaveBeenCalledWith(
      expect.stringContaining('/auth/refresh'),
      {},
      expect.objectContaining({ withCredentials: true }),
    )
  })

  it('stores rotated tokens in memory on success', async () => {
    setAuthTokens(null, 'old-refresh')
    vi.mocked(axios.post).mockResolvedValue({
      data: { token: 'new-access', refreshToken: 'new-refresh' },
    })
    const result = await refreshAuthToken()
    expect(result).toEqual({ token: 'new-access', refreshToken: 'new-refresh' })
    expect(getAccessToken()).toBe('new-access')
    expect(getRefreshToken()).toBe('new-refresh')
    expect(axios.post).toHaveBeenCalledWith(
      expect.stringContaining('/auth/refresh'),
      { refreshToken: 'old-refresh' },
      expect.objectContaining({
        withCredentials: true,
        headers: { 'Refresh-Token': 'old-refresh' },
      }),
    )
  })

  it('returns null when refresh request fails', async () => {
    setAuthTokens(null, 'old-refresh')
    vi.mocked(axios.post).mockRejectedValue(new Error('401'))
    await expect(refreshAuthToken()).resolves.toBeNull()
    expect(getAccessToken()).toBeNull()
  })
})
