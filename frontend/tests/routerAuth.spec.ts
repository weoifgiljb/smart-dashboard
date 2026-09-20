import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import { isAuthEntryPath, routeRequiresAuth } from '@/router/authGuard'
import { routes } from '@/router/routes'

function resolve(path: string) {
  const router = createRouter({ history: createMemoryHistory(), routes })
  return router.resolve(path)
}

describe('auth routes', () => {
  it('does not treat login as an authenticated layout catch-all', () => {
    const login = resolve('/login')
    expect(login.name).toBe('Login')
    expect(isAuthEntryPath(login.path)).toBe(true)
    expect(routeRequiresAuth(login)).toBe(false)
  })

  it('keeps diary on the named diary route and requires auth', () => {
    const diary = resolve('/diary')
    expect(diary.name).toBe('Diary')
    expect(routeRequiresAuth(diary)).toBe(true)
  })
})
