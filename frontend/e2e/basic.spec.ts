import { test, expect } from '@playwright/test'
import { seedAnonymousSession } from './helpers/auth'

test.beforeEach(async ({ page }) => {
  await seedAnonymousSession(page)
})

test('home redirects to login when unauthenticated', async ({ page }) => {
  await page.goto('/')
  await expect(page).toHaveURL(/\/login/, { timeout: 10000 })
})

test('login page shows username and password fields', async ({ page }) => {
  await page.goto('/login')
  await expect(page.getByPlaceholder('用户名/邮箱')).toBeVisible()
  await expect(page.getByPlaceholder('密码')).toBeVisible()
})
