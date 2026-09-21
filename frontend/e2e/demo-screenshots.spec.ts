import { expect, test } from '@playwright/test'
import { seedAuthenticatedSession } from './helpers/auth'
import path from 'node:path'

const demoDir = path.resolve(__dirname, '../../docs/demo')

test.describe('main-path demo screenshots', () => {
  test.beforeEach(async ({ page }) => {
    await seedAuthenticatedSession(page)
  })

  test('captures the five walkthrough frames', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 800 })

    await page.goto('/')
    await expect(page.locator('.main').getByText('今日节律')).toBeVisible()
    await page.screenshot({ path: path.join(demoDir, '01-home-rhythm.png'), fullPage: true })

    await page.goto('/calendar')
    await expect(page.getByText('日历与打卡')).toBeVisible()
    await page.screenshot({ path: path.join(demoDir, '02-checkin.png'), fullPage: true })

    await page.goto('/pomodoro')
    await expect(page.getByText('番茄专注')).toBeVisible()
    await page.screenshot({ path: path.join(demoDir, '03-pomodoro-task.png'), fullPage: true })

    await page.goto('/vocabulary/review')
    await expect(page.getByText('识记')).toBeVisible()
    await page.screenshot({ path: path.join(demoDir, '04-micro-review.png'), fullPage: true })

    await page.goto('/calendar')
    await expect(page.getByTitle('2026-09-20：热力 9')).toBeVisible()
    await page.getByTitle('2026-09-20：热力 9').click()
    await expect(page.getByText('日记')).toBeVisible()
    await page.screenshot({ path: path.join(demoDir, '05-calendar-heat.png'), fullPage: true })
  })
})
