import { afterEach, describe, expect, it, vi } from 'vitest'
import axios from 'axios'
import { refreshAuthToken } from '@/api/authRefresh'

vi.mock('axios', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('refreshAuthToken', () => {
  afterEach(() => {
    localStorage.clear()
    vi.resetAllMocks()
  })

  it('returns null when no refresh token', async () => {
    await expect(refreshAuthToken()).resolves.toBeNull()
  })

  it('stores rotated tokens on success', async () => {
    localStorage.setItem('refreshToken', 'old-refresh')
    vi.mocked(axios.post).mockResolvedValue({
      data: { token: 'new-access', refreshToken: 'new-refresh' },
    })
    const result = await refreshAuthToken()
    expect(result).toEqual({ token: 'new-access', refreshToken: 'new-refresh' })
    expect(localStorage.getItem('token')).toBe('new-access')
    expect(localStorage.getItem('refreshToken')).toBe('new-refresh')
    expect(axios.post).toHaveBeenCalled()
  })

  it('returns null when refresh request fails', async () => {
    localStorage.setItem('refreshToken', 'old-refresh')
    vi.mocked(axios.post).mockRejectedValue(new Error('401'))
    await expect(refreshAuthToken()).resolves.toBeNull()
  })
})
