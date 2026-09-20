import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getUserInfo } from '@/api/auth'
import { clearAuthTokens } from '@/api/authTokens'
import { useUserStore } from '@/store/user'

vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  register: vi.fn(),
  getUserInfo: vi.fn(),
  logout: vi.fn(),
}))

describe('user session bootstrap', () => {
  beforeEach(() => {
    clearAuthTokens()
    setActivePinia(createPinia())
    vi.mocked(getUserInfo).mockReset()
  })

  it('restores an authenticated session from /auth/me without memory tokens', async () => {
    vi.mocked(getUserInfo).mockResolvedValue({
      id: 'u1',
      username: 'alice',
      email: 'alice@example.com',
    })
    const store = useUserStore()
    expect(store.isAuthenticated).toBe(false)
    await store.ensureSession()
    expect(store.isAuthenticated).toBe(true)
    expect(store.user?.username).toBe('alice')
  })

  it('stays anonymous when /auth/me is unauthorized', async () => {
    vi.mocked(getUserInfo).mockRejectedValue(new Error('Unauthorized'))
    const store = useUserStore()
    await store.ensureSession()
    expect(store.isAuthenticated).toBe(false)
    expect(store.user).toBeNull()
  })
})
