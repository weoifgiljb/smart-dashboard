import { ref } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ElementPlus from 'element-plus'
import { useRouter } from 'vue-router'
import Dashboard from './Dashboard.vue'

const push = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: vi.fn(),
}))

vi.mock('@/composables/useDashboard', () => ({
  useDashboard: () => ({
    stats: ref({
      checkInDays: 3,
      wordCount: 10,
      pomodoroCount: 4,
      totalDays: 8,
    }),
    todayTasks: ref({
      hasCheckedIn: true,
      todayWordCount: 2,
      todayPomodoroCount: 1,
    }),
    rhythm: ref({
      nextAction: 'CHECK_IN',
      reason: '今天还没打卡，先记下这一天的起点。',
      ctaLabel: '立即打卡',
      ctaPath: '/calendar',
      hasCheckedIn: false,
      dueWordCount: 0,
      heat: {
        checkIn: 0,
        pomodoro: 4,
        word: 2,
        task: 9,
        total: 15,
        pomodoroCount: 2,
        wordCount: 2,
        taskCount: 3,
      },
      dueWords: [],
      focusTask: null,
    }),
    recentActivities: ref([]),
    heatValueOption: ref({}),
    pomodoroOption: ref({}),
    wordsOption: ref({}),
    dateRange: ref(null),
    dialogVisible: ref(false),
    dialogTitle: ref(''),
    dialogData: ref([]),
    dialogType: ref('pomodoro'),
    shortcuts: [],
    handleChartClick: vi.fn(),
    formatActivityTime: vi.fn(),
  }),
}))

vi.mock('@/components/charts/BaseChart.vue', () => ({
  default: {
    name: 'BaseChart',
    template: '<div class="base-chart-stub" />',
  },
}))

describe('Dashboard.vue', () => {
  beforeEach(() => {
    push.mockReset()
    vi.mocked(useRouter).mockReturnValue({ push } as never)
  })

  it('renders rhythm CTA and heat formula', () => {
    const wrapper = mount(Dashboard, {
      global: { plugins: [ElementPlus] },
    })

    expect(wrapper.text()).toContain('今日节律')
    expect(wrapper.text()).toContain('立即打卡')
    expect(wrapper.text()).toContain('打卡×1 + 番茄×2 + 单词×1 + 任务×3')
    expect(wrapper.text()).toContain('打卡')
    expect(wrapper.text()).toContain('番茄')
    expect(wrapper.text()).toContain('单词')
    expect(wrapper.text()).toContain('任务')
    expect(wrapper.findAll('.kpi-card')).toHaveLength(4)
    expect(wrapper.findAll('.chart-card')).toHaveLength(3)
  })

  it('navigates to the rhythm CTA path', async () => {
    const wrapper = mount(Dashboard, {
      global: { plugins: [ElementPlus] },
    })

    await wrapper.get('.action-btn').trigger('click')
    expect(push).toHaveBeenCalledWith('/calendar')
  })

  it('shows pomodoro duration in minutes', () => {
    const wrapper = mount(Dashboard, {
      global: { plugins: [ElementPlus] },
    })
    expect(wrapper.vm.formatDurationMinutes(25)).toBe(25)
    expect(wrapper.vm.formatDurationMinutes(5)).toBe(5)
  })
})
