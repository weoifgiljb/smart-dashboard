import type { Page } from '@playwright/test'
import { mockApi } from './mockApi'

export async function seedAuthenticatedSession(page: Page) {
  await page.addInitScript(() => {
    localStorage.setItem('token', 'smoke-token')
  })
  await mockApi(page)
}
