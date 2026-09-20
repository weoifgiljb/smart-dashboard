import type { Page, Route } from '@playwright/test'

const SAMPLE_WORD = {
  id: 'w1',
  word: 'focus',
  translation: '专注',
  example: 'Stay in focus.',
}

const SAMPLE_BOOK = {
  id: 'book-1',
  title: 'Deep Work',
  author: 'Cal Newport',
  description: 'Rules for focused success.',
}

const SMOKE_USER = {
  id: 'u1',
  username: 'alice',
  email: 'alice@example.com',
  createTime: '2026-01-01T00:00:00',
}

function json(route: Route, data: unknown) {
  return route.fulfill({
    status: 200,
    contentType: 'application/json; charset=utf-8',
    headers: { 'access-control-allow-origin': '*' },
    body: JSON.stringify(data),
  })
}

function apiPath(url: URL) {
  return url.pathname.replace(/^\/api/, '') || url.pathname
}

function isApiRequest(url: URL) {
  return (
    url.pathname.startsWith('/api/') ||
    url.pathname === '/api' ||
    url.port === '8080' ||
    url.hostname === 'api.github.com'
  )
}

function payloadFor(url: URL, method: string) {
  const path = apiPath(url)

  if (path.endsWith('/auth/me')) return SMOKE_USER
  if (path.includes('/dashboard/rhythm')) {
    return {
      nextAction: 'CHECK_IN',
      reason: '今天还没打卡，先记下这一天的起点。',
      ctaLabel: '立即打卡',
      ctaPath: '/calendar',
      hasCheckedIn: false,
      dueWordCount: 1,
      heat: {
        checkIn: 0,
        pomodoro: 0,
        word: 0,
        task: 0,
        total: 0,
        pomodoroCount: 0,
        wordCount: 0,
        taskCount: 0,
      },
      dueWords: [SAMPLE_WORD],
      focusTask: null,
    }
  }
  if (path.includes('/dashboard/stats')) {
    return { checkInDays: 0, wordCount: 1, pomodoroCount: 0, totalDays: 0 }
  }
  if (path.includes('/dashboard/today-tasks')) {
    return { hasCheckedIn: false, todayWordCount: 1, todayPomodoroCount: 0 }
  }
  if (path.includes('/dashboard/recent-activities')) return []
  if (path.includes('/words/today')) return [SAMPLE_WORD]
  if ((path === '/words' || path.endsWith('/words')) && method === 'GET') return [SAMPLE_WORD]
  if (path.includes('/checkin/stats')) {
    return { consecutiveDays: 0, totalDays: 0, hasCheckedInToday: false }
  }
  if (path.includes('/pomodoro/stats')) return { todayCount: 0, totalCount: 0 }
  if (path.includes('/tasks/aggregate/stats')) {
    return { byStatus: {}, byPriority: {}, overdue: 0 }
  }
  if (/\/books\/[^/]+$/.test(path) && !path.includes('/books/random')) return SAMPLE_BOOK
  if (path.includes('/books')) return { content: [SAMPLE_BOOK], totalElements: 1 }
  if ((path === '/calendar' || path.endsWith('/calendar')) && method === 'GET') {
    return {
      '2026-01-05': { checkin: 1 },
      '2026-02-14': { checkin: 1, pomodoro: 2 },
      '2026-03-08': { checkin: 1, word: 4 },
      '2026-04-21': { checkin: 1, pomodoro: 1, task: 1 },
      '2026-06-11': { pomodoro: 3 },
      '2026-09-18': { checkin: 1, word: 2 },
      '2026-09-19': { checkin: 1, pomodoro: 2, word: 1 },
      '2026-09-20': { checkin: 1, pomodoro: 2, word: 1, task: 1 },
    }
  }
  if (method === 'GET') {
    if (path.includes('stats') || path.includes('/calendar')) return {}
    return []
  }
  return {}
}

export async function mockApi(page: Page) {
  let sessionLive = true
  await page.route(isApiRequest, async (route) => {
    const url = new URL(route.request().url())
    if (url.hostname === 'api.github.com') {
      return json(route, { items: [] })
    }
    const method = route.request().method().toUpperCase()
    if (method === 'OPTIONS') {
      return route.fulfill({
        status: 204,
        headers: {
          'access-control-allow-origin': '*',
          'access-control-allow-headers': '*',
          'access-control-allow-methods': '*',
        },
      })
    }
    const path = apiPath(url)
    if (path.endsWith('/auth/logout') && method === 'POST') {
      sessionLive = false
      return route.fulfill({ status: 204, body: '' })
    }
    if (path.endsWith('/auth/me') && !sessionLive) {
      return route.fulfill({
        status: 401,
        contentType: 'application/json; charset=utf-8',
        body: JSON.stringify({ message: 'Unauthorized' }),
      })
    }
    return json(route, payloadFor(url, method))
  })
}
