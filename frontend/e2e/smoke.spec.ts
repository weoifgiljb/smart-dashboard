import { expect, test } from '@playwright/test'
import { seedAnonymousSession, seedAuthenticatedSession } from './helpers/auth'
import { previousMonthKey, sampleDiary, todayDateKey } from './helpers/mockApi'

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

  test('diary requires login', async ({ page }) => {
    await page.goto('/diary')
    await expect(page).toHaveURL(/\/login/)
  })

  test('login page can open register', async ({ page }) => {
    await page.goto('/login')
    await page.getByText('立即注册').click()
    await expect(page).toHaveURL(/\/register/)
    await expect(page.getByRole('heading', { name: '创建账号' })).toBeVisible()
  })

  test('login page uses the same app chrome as the dashboard', async ({ page }) => {
    await page.goto('/login')
    await expect(page.getByRole('heading', { name: '自律组件' })).toBeVisible()
    await expect(page.getByRole('switch')).toBeVisible()
    await expect(page.getByRole('heading', { name: '欢迎回来' })).toBeVisible()
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

test.describe('authenticated diary', () => {
  test('empty list disables export and shows empty state', async ({ page }) => {
    await seedAuthenticatedSession(page)
    await page.goto('/diary')
    await expect(page.locator('.layout-container')).toBeVisible({ timeout: 15000 })
    await expect(page.getByRole('heading', { name: '我的日记' })).toBeVisible()
    await expect(page.getByRole('button', { name: '写日记' })).toBeVisible()
    await expect(page.getByRole('button', { name: '导出 PDF' })).toBeDisabled()
    await expect(page.getByRole('button', { name: '导出 Word' })).toBeDisabled()
    await expect(page.getByText('还没有写过日记，开始记录第一篇吧！')).toBeVisible()
  })

  test('fixture list shows date mood tags and image', async ({ page }) => {
    const day = todayDateKey()
    await seedAuthenticatedSession(page, { diaries: [sampleDiary()] })
    await page.goto('/diary')
    await expect(page.locator('.layout-container')).toBeVisible({ timeout: 15000 })
    await expect(page.locator(`[data-diary-date="${day}"]`)).toBeVisible()
    await expect(page.getByText(day)).toBeVisible()
    await expect(page.getByText('开心')).toBeVisible()
    await expect(page.getByText('# 生活')).toBeVisible()
    await expect(page.getByText('# 学习')).toBeVisible()
    await expect(page.getByText(/更新于 \d{2}:\d{2}/)).toBeVisible()
    await expect(page.locator('.diary-image')).toBeVisible()
  })

  test('shows past month diaries in the default list', async ({ page }) => {
    const prev = `${previousMonthKey()}-15`
    await seedAuthenticatedSession(page, {
      diaries: [
        sampleDiary({ content: '本月日记' }),
        sampleDiary({
          id: 'd2',
          diaryDate: prev,
          content: '上月日记',
          imageUrl: '',
          updatedAt: `${prev}T12:00:00`,
        }),
      ],
    })
    await page.goto('/diary')
    await expect(page.getByText('本月日记')).toBeVisible({ timeout: 15000 })
    await expect(page.getByText('上月日记')).toBeVisible()
    await expect(page.locator(`[data-diary-date="${prev}"]`)).toBeVisible()
  })

  test('can browse a previous month diary', async ({ page }) => {
    const prev = `${previousMonthKey()}-15`
    await seedAuthenticatedSession(page, {
      diaries: [
        sampleDiary({ content: '本月日记' }),
        sampleDiary({
          id: 'd2',
          diaryDate: prev,
          content: '上月日记',
          imageUrl: '',
          updatedAt: `${prev}T12:00:00`,
        }),
      ],
    })
    await page.goto('/diary')
    await expect(page.getByText('上月日记')).toBeVisible({ timeout: 15000 })
    await page.getByRole('button', { name: '本月', exact: true }).click()
    await expect(page.getByText('本月日记')).toBeVisible()
    await expect(page.getByText('上月日记')).toHaveCount(0)
    await page.getByRole('button', { name: '上一月' }).click()
    await expect(page.getByText('上月日记')).toBeVisible()
    await expect(page.locator(`[data-diary-date="${prev}"]`)).toBeVisible()
    await expect(page.getByText('本月日记')).toHaveCount(0)
    await page.getByRole('button', { name: '全部', exact: true }).click()
    await expect(page.getByText('本月日记')).toBeVisible()
    await expect(page.getByText('上月日记')).toBeVisible()
  })

  test('long unspaced content wraps inside the card', async ({ page }) => {
    await seedAuthenticatedSession(page, {
      diaries: [sampleDiary({ content: '2'.repeat(240), imageUrl: '' })],
    })
    await page.goto('/diary')
    await expect(page.locator('.diary-content')).toBeVisible({ timeout: 15000 })
    const wrapped = await page
      .locator('.diary-content')
      .evaluate((el) => el.scrollWidth <= el.clientWidth + 1)
    expect(wrapped).toBe(true)
    const card = page.locator('.diary-item-card')
    const contentBox = await page.locator('.diary-content').boundingBox()
    const cardBox = await card.boundingBox()
    expect(contentBox && cardBox).toBeTruthy()
    expect(contentBox!.width).toBeLessThanOrEqual(cardBox!.width + 2)
  })

  test('writing today opens existing entry and keeps a single diary', async ({ page }) => {
    await seedAuthenticatedSession(page, {
      diaries: [
        sampleDiary({
          diaryDate: todayDateKey(),
          content: '原文内容',
          updatedAt: new Date().toISOString(),
        }),
      ],
    })
    await page.goto('/diary')
    await expect(page.locator('.layout-container')).toBeVisible({ timeout: 15000 })
    await page.getByRole('button', { name: '写日记' }).click()
    await expect(page.getByText('编辑今天的日记')).toBeVisible()
    await expect(page.locator('.el-dialog .el-date-editor input')).toBeDisabled()
    await expect(page.locator('.diary-editor textarea')).toHaveValue('原文内容')
    await page.locator('.diary-editor textarea').fill('覆盖后的内容')
    await page.getByRole('button', { name: '保存' }).click()
    await expect(page.getByText('保存成功')).toBeVisible()
    await expect(page.getByText('覆盖后的内容')).toBeVisible()
    await expect(page.locator('.diary-item-card')).toHaveCount(1)
  })

  test('edit locks date and updates content', async ({ page }) => {
    await seedAuthenticatedSession(page, { diaries: [sampleDiary({ content: '旧文' })] })
    await page.goto('/diary')
    await expect(page.locator('.layout-container')).toBeVisible({ timeout: 15000 })
    await page.getByRole('button', { name: '编辑' }).click()
    await expect(page.getByText('编辑日记')).toBeVisible()
    await expect(page.locator('.el-dialog .el-date-editor input')).toBeDisabled()
    await page.locator('.diary-editor textarea').fill('新的正文')
    await page.getByRole('button', { name: '保存' }).click()
    await expect(page.getByText('保存成功')).toBeVisible()
    await expect(page.getByText('新的正文')).toBeVisible()
  })

  test('delete confirm removes an entry and cancel does not', async ({ page }) => {
    await seedAuthenticatedSession(page, { diaries: [sampleDiary()] })
    await page.goto('/diary')
    await expect(page.getByText('今天很好')).toBeVisible({ timeout: 15000 })
    await page.getByRole('button', { name: '删除' }).click()
    await page.getByRole('button', { name: '取消' }).click()
    await expect(page.getByText('今天很好')).toBeVisible()
    await page.getByRole('button', { name: '删除' }).click()
    await page.locator('.el-message-box').getByRole('button', { name: '确定' }).click()
    await expect(page.getByText('删除成功')).toBeVisible()
    await expect(page.getByText('还没有写过日记，开始记录第一篇吧！')).toBeVisible()
  })

  test('delete 404 does not show success', async ({ page }) => {
    await seedAuthenticatedSession(page, {
      diaries: [sampleDiary()],
      deleteNotFoundIds: ['d1'],
    })
    await page.goto('/diary')
    await expect(page.getByText('今天很好')).toBeVisible({ timeout: 15000 })
    await page.getByRole('button', { name: '删除' }).click()
    await page.locator('.el-message-box').getByRole('button', { name: '确定' }).click()
    await expect(page.getByText('日记不存在')).toBeVisible()
    await expect(page.getByText('删除成功')).toHaveCount(0)
    await expect(page.getByText('今天很好')).toBeVisible()
  })

  test('export downloads files and surfaces json blob errors', async ({ page }) => {
    await seedAuthenticatedSession(page, { diaries: [sampleDiary()] })
    await page.goto('/diary')
    await expect(page.getByRole('button', { name: '导出 PDF' })).toBeEnabled({ timeout: 15000 })

    const pdfDownload = page.waitForEvent('download')
    await page.getByRole('button', { name: '导出 PDF' }).click()
    await expect(page.getByRole('button', { name: '导出 PDF' })).toHaveClass(/is-loading/)
    const pdf = await pdfDownload
    expect(pdf.suggestedFilename()).toMatch(/^diaries-\d{4}-\d{2}-\d{2}\.pdf$/)

    const wordDownload = page.waitForEvent('download')
    await page.getByRole('button', { name: '导出 Word' }).click()
    const word = await wordDownload
    expect(word.suggestedFilename()).toMatch(/^diaries-\d{4}-\d{2}-\d{2}\.docx$/)
  })

  test('export json blob shows failure', async ({ page }) => {
    await seedAuthenticatedSession(page, {
      diaries: [sampleDiary()],
      exportJsonError: true,
    })
    await page.goto('/diary')
    await expect(page.getByRole('button', { name: '导出 PDF' })).toBeEnabled({ timeout: 15000 })
    await page.getByRole('button', { name: '导出 PDF' }).click()
    await expect(page.getByText('导出失败')).toBeVisible()
  })

  test('validation moods preview and meme', async ({ page }) => {
    await seedAuthenticatedSession(page)
    await page.goto('/diary')
    await expect(page.locator('.layout-container')).toBeVisible({ timeout: 15000 })
    await page.getByRole('button', { name: '写日记' }).click()
    await expect(page.getByText('写日记').nth(0)).toBeVisible()
    await page.getByRole('button', { name: '保存' }).click()
    await expect(page.getByText('请填写日记内容')).toBeVisible()
    await expect(page.getByText('😄')).toBeVisible()
    await expect(page.getByText('😐')).toBeVisible()
    await expect(page.getByText('😭')).toBeVisible()
    await expect(page.getByText('💪')).toBeVisible()
    await expect(page.getByText('😫')).toBeVisible()
    await page.getByText('💪').click()
    await page.getByRole('button', { name: '智能配图 (RAG)' }).click()
    await expect(page.getByText('请先填写日记内容')).toBeVisible()
    await page.locator('.diary-editor textarea').fill('**今天**去跑步')
    await expect(page.locator('.diary-preview')).toContainText('今天')
    await page.getByRole('button', { name: '智能配图 (RAG)' }).click()
    await expect(page.locator('.composer-image')).toBeVisible()
    await page.locator('.composer-image-clear').click()
    await expect(page.locator('.composer-image')).toHaveCount(0)
  })

  test('dark editor is not forced white', async ({ page }) => {
    await page.addInitScript(() => localStorage.setItem('theme', 'dark'))
    await seedAuthenticatedSession(page)
    await page.goto('/diary')
    await expect(page.locator('.layout-container')).toBeVisible({ timeout: 15000 })
    await page.getByRole('button', { name: '写日记' }).click()
    const bg = await page
      .locator('.diary-editor .el-textarea__inner')
      .evaluate((el) => getComputedStyle(el).backgroundColor)
    expect(bg.replace(/\s/g, '')).not.toBe('rgb(255,255,255)')
    expect(bg.replace(/\s/g, '')).not.toBe('rgba(255,255,255,1)')
  })

  test('sidebar can reopen diary list in the same session', async ({ page }) => {
    await seedAuthenticatedSession(page, { diaries: [sampleDiary()] })
    await page.goto('/diary')
    await expect(page.getByText('今天很好')).toBeVisible({ timeout: 15000 })
    await page.locator('.el-menu-item', { hasText: '日历 & 打卡' }).click()
    await expect(page.locator('.main').getByText('日历与打卡').first()).toBeVisible()
    await page.locator('.el-menu-item', { hasText: '我的日记' }).click()
    await expect(page.getByText('今天很好')).toBeVisible()
  })
})
