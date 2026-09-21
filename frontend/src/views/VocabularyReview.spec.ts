import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ElementPlus from 'element-plus'
import VocabularyReview from './VocabularyReview.vue'
import { getTodayWords, reviewWord, WordReviewResult } from '@/api/words'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ query: {} }),
}))

vi.mock('@/api/words', () => ({
  getTodayWords: vi.fn(),
  getWords: vi.fn(),
  reviewWord: vi.fn(),
  WordReviewResult: {
    Unknown: 'unknown',
    Vague: 'vague',
    Known: 'known',
  },
}))

describe('VocabularyReview.vue', () => {
  beforeEach(() => {
    vi.mocked(getTodayWords).mockResolvedValue([
      { id: 'w1', word: 'accident', translation: '事故' },
      { id: 'w2', word: 'ability', translation: '能力' },
    ])
    vi.mocked(reviewWord).mockResolvedValue({})
    vi.stubGlobal(
      'SpeechSynthesisUtterance',
      class {
        text = ''
        lang = ''
        constructor(text: string) {
          this.text = text
        }
      },
    )
    vi.stubGlobal('speechSynthesis', { speak: vi.fn() })
  })

  it('does not reuse the flipped card when marking 认识', async () => {
    const wrapper = mount(VocabularyReview, {
      global: { plugins: [ElementPlus] },
    })
    await flushPromises()

    expect(wrapper.get('.word-primary').text()).toBe('accident')
    await wrapper.get('.flip-card-wrapper').trigger('click')
    expect(wrapper.get('.flip-card-wrapper').classes()).toContain('flipped')

    const previousCard = wrapper.get('.flip-card-wrapper').element
    await wrapper.get('.review-btn.known').trigger('click')
    await flushPromises()

    expect(wrapper.get('.word-primary').text()).toBe('ability')
    expect(wrapper.get('.flip-card-wrapper').classes()).not.toContain('flipped')
    expect(wrapper.get('.flip-card-wrapper').element).not.toBe(previousCard)
    expect(wrapper.get('.word-meaning').text()).toBe('能力')
    expect(reviewWord).toHaveBeenCalledWith('w1', WordReviewResult.Known)
  })

  it('sends 模糊 and 不认识 as distinct review results', async () => {
    const wrapper = mount(VocabularyReview, {
      global: { plugins: [ElementPlus] },
    })
    await flushPromises()
    await wrapper.get('.flip-card-wrapper').trigger('click')
    await wrapper.get('.review-btn.vague').trigger('click')
    await flushPromises()
    expect(reviewWord).toHaveBeenCalledWith('w1', WordReviewResult.Vague)

    await wrapper.get('.flip-card-wrapper').trigger('click')
    await wrapper.get('.review-btn.unknown').trigger('click')
    await flushPromises()
    expect(reviewWord).toHaveBeenCalledWith('w2', WordReviewResult.Unknown)
  })

  it('shows loading copy while today words are still fetching', () => {
    vi.mocked(getTodayWords).mockReturnValue(new Promise(() => undefined) as never)
    const wrapper = mount(VocabularyReview, {
      global: { plugins: [ElementPlus] },
    })
    expect(wrapper.text()).toContain('正在加载到期单词')
    expect(wrapper.text()).not.toContain('加载中或暂无单词')
  })

  it('shows an empty queue after today words resolve to none', async () => {
    vi.mocked(getTodayWords).mockResolvedValue([])
    const wrapper = mount(VocabularyReview, {
      global: { plugins: [ElementPlus] },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('今日没有到期单词')
    expect(wrapper.text()).not.toContain('加载中或暂无单词')
  })

  it('shows retry when loading today words fails', async () => {
    vi.mocked(getTodayWords).mockRejectedValue(new Error('network'))
    const wrapper = mount(VocabularyReview, {
      global: { plugins: [ElementPlus] },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('加载到期单词失败，请重试')
    expect(wrapper.text()).not.toContain('今日没有到期单词')
  })
})
