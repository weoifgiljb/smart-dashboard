import { expect, test } from '@playwright/test'
import { seedAnonymousSession, seedAuthenticatedSession } from './helpers/auth'

test.describe('unauthenticated pages', () => {
  test.beforeEach(async ({ page }) => {
    await seedAnonymousSession(page)
  })

  test('register page shows account fields', async ({ page }) => {
    await page.goto('/register')
    await expect(page.getByRole('heading', { name: '创建账号' })).toBeVisible()
    await expect(page.getByPlaceholder('用户名')).toBeVisible()
    await expect(page.getByPlaceholder('电子邮箱')).toBeVisible()
    await expect(page.getByPlaceholder('设置密码')).toBeVisible()
  })

  test('login page can open register', async ({ page }) => {
    await page.goto('/login')
    await page.getByText('立即注册').click()
    await expect(page).toHaveURL(/\/register/)
    await expect(page.getByRole('heading', { name: '创建账号' })).toBeVisible()
  })
})

test.describe('authenticated smoke', () => {
  test.beforeEach(async ({ page }) => {
    await seedAuthenticatedSession(page)
  })

  test('year heatmap shows a full-year contribution grid', async ({ page }) => {
    await page.goto('/calendar')
    await expect(page.locator('.layout-container')).toBeVisible({ timeout: 15000 })
    await expect(page.getByText('年度活动热力')).toBeVisible()
    await expect(page.getByText(/天有活动/)).toBeVisible()
    await expect(page.getByTitle('2026-09-20：热力 9')).toBeVisible()
  })

  test('sidebar visits every main page', async ({ page }) => {
    await page.goto('/')
    await expect(page.locator('.layout-container')).toBeVisible({ timeout: 15000 })
    await expect(page.locator('.app-title').first()).toBeVisible()
    await expect(page.getByText('欢迎，alice')).toBeVisible()
    await expect(page.locator('.main').getByText('今日节律')).toBeVisible()

    const visits = [
      { menu: '日历 & 打卡', heading: '日历与打卡' },
      { menu: '背单词', heading: '单词本' },
      { menu: '番茄钟', heading: '番茄专注' },
      { menu: '任务（增强）', heading: '任务清单' },
      { menu: '我的日记', heading: '我的日记' },
      { menu: 'AI问答', heading: 'AI 助手' },
      { menu: '书籍推送', heading: '发现下一本好书' },
    ]

    for (const { menu, heading } of visits) {
      await page.locator('.el-menu-item', { hasText: menu }).click()
      await expect(page.locator('.main').getByText(heading).first()).toBeVisible()
    }
  })

  test('vocabulary review shows recognize mode', async ({ page }) => {
    await page.goto('/vocabulary/review')
    await expect(page.locator('.layout-container')).toBeVisible({ timeout: 15000 })
    await expect(page.getByText('识记')).toBeVisible()
    await expect(page.locator('.word-primary')).toHaveText('focus')
  })

  test('book detail shows the stubbed book', async ({ page }) => {
    await page.goto('/books/book-1')
    await expect(page.locator('.layout-container')).toBeVisible({ timeout: 15000 })
    await expect(page.getByText('书籍详情')).toBeVisible()
    await expect(page.getByText('Deep Work')).toBeVisible()
  })

  test('unknown route shows not found', async ({ page }) => {
    await page.goto('/no-such-page')
    await expect(page.locator('.layout-container')).toBeVisible({ timeout: 15000 })
    await expect(page.getByText('页面不存在')).toBeVisible()
  })

  test('logout returns to login', async ({ page }) => {
    await page.goto('/')
    await expect(page.locator('.layout-container')).toBeVisible({ timeout: 15000 })
    await page.locator('.header-right').getByRole('button', { name: '退出' }).click()
    await expect(page).toHaveURL(/\/login/)
  })
})
