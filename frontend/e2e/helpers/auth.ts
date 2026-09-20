import type { Page } from '@playwright/test'
import { mockApi, type MockApiOptions } from './mockApi'

export async function seedAuthenticatedSession(page: Page, options?: MockApiOptions) {
  await mockApi(page, options)
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
