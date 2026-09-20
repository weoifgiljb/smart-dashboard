import { test, expect } from '@playwright/test'

test('home redirects to login when unauthenticated', async ({ page }) => {
  await page.goto('/')
  await page.waitForLoadState('domcontentloaded')
  expect(page.url()).toContain('/login')
})
