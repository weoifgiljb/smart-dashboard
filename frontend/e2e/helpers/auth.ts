import type { Page } from '@playwright/test'
import { mockApi } from './mockApi'

export async function seedAuthenticatedSession(page: Page) {
  await mockApi(page)
}

export async function seedAnonymousSession(page: Page) {
  await page.route((url) => {
    const path = url.pathname
    return path.includes('/auth/me') || path.includes('/auth/refresh')
  }, async (route) => {
    await route.fulfill({
      status: 401,
      contentType: 'application/json; charset=utf-8',
      body: JSON.stringify({ message: 'Unauthorized' }),
    })
  })
}
