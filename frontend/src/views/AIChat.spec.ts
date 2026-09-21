import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import ElementPlus, { ElMessage } from 'element-plus'
import AIChat from './AIChat.vue'
import { sendChatMessage, streamChatMessage } from '@/api/ai'
import { ChatMessageType, type ChatMessage, type Conversation } from '@/types/chat'

const conversations = ref<Conversation[]>([])
const activeId = ref<string | null>(null)
const messages = ref<ChatMessage[]>([])
const loadingMessages = ref(false)
const loadSeq = ref(0)
const createConversation = vi.fn()
const selectConversation = vi.fn()
const rename = vi.fn()
const remove = vi.fn()
const ensureActive = vi.fn()
const refreshList = vi.fn()

vi.mock('@/composables/useConversations', () => ({
  useConversations: () => ({
    conversations,
    activeId,
    messages,
    loadingMessages,
    loadSeq,
    createConversation,
    selectConversation,
    rename,
    remove,
    ensureActive,
    refreshList,
  }),
}))

vi.mock('@/api/ai', () => ({
  sendChatMessage: vi.fn(),
  streamChatMessage: vi.fn(),
}))

function appleBananaMessages(): ChatMessage[] {
  return [
    { type: ChatMessageType.User, content: '苹果问题', time: '2026-01-01T00:00:00' },
    { type: ChatMessageType.Ai, content: '苹果回答', time: '2026-01-01T00:00:01' },
    { type: ChatMessageType.User, content: '香蕉问题', time: '2026-01-01T00:00:02' },
    { type: ChatMessageType.Ai, content: '香蕉回答', time: '2026-01-01T00:00:03' },
  ]
}

describe('AIChat.vue', () => {
  beforeEach(() => {
    conversations.value = [
      { id: 'a', userId: 'u1', title: '对话A', createdAt: '2026-01-01', updatedAt: '2026-01-01' },
    ]
    activeId.value = 'a'
    messages.value = [
      { type: ChatMessageType.User, content: 'A问题', time: '2026-01-01T00:00:00' },
      { type: ChatMessageType.Ai, content: 'A回答', time: '2026-01-01T00:00:01' },
    ]
    createConversation.mockReset().mockImplementation(async () => {
      const created: Conversation = {
        id: 'b',
        userId: 'u1',
        title: '新对话',
        createdAt: '2026-01-02',
        updatedAt: '2026-01-02',
      }
      conversations.value = [created, conversations.value[0]]
      activeId.value = created.id
      messages.value = []
      return created
    })
    selectConversation.mockReset().mockImplementation(async (id: string) => {
      activeId.value = id
      if (id === 'a') {
        messages.value = [
          { type: ChatMessageType.User, content: 'A问题', time: '2026-01-01T00:00:00' },
          { type: ChatMessageType.Ai, content: 'A回答', time: '2026-01-01T00:00:01' },
        ]
      } else {
        messages.value = []
      }
    })
    rename.mockReset()
    remove.mockReset()
    ensureActive.mockReset().mockResolvedValue(undefined)
    refreshList.mockReset().mockResolvedValue(undefined)
    loadingMessages.value = false
    loadSeq.value = 1
    vi.mocked(sendChatMessage).mockReset()
    vi.mocked(streamChatMessage).mockReset()
    vi.mocked(streamChatMessage).mockRejectedValue(new Error('no stream'))
    vi.spyOn(console, 'warn').mockImplementation(() => undefined)
    vi.spyOn(console, 'error').mockImplementation(() => undefined)
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('keeps the previous conversation when creating a new one', async () => {
    const wrapper = mount(AIChat, {
      global: { plugins: [ElementPlus] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('对话A')
    await wrapper.get('.new-chat-btn').trigger('click')
    await flushPromises()

    expect(createConversation).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('对话A')
    expect(wrapper.text()).toContain('新对话')
    expect(wrapper.text()).toContain('你好，我是你的智能助手')
    expect(wrapper.text()).not.toContain('A问题')
  })

  it('shows previous messages after switching back', async () => {
    const wrapper = mount(AIChat, {
      global: { plugins: [ElementPlus] },
    })
    await flushPromises()
    await wrapper.get('.new-chat-btn').trigger('click')
    await flushPromises()

    const items = wrapper.findAll('.history-item')
    const previous = items.find((item) => item.text().includes('对话A'))
    expect(previous).toBeTruthy()
    await previous!.trigger('click')
    await flushPromises()

    expect(selectConversation).toHaveBeenCalledWith('a')
    expect(wrapper.text()).toContain('A问题')
    expect(wrapper.text()).toContain('A回答')
  })

  it('exposes a mobile drawer trigger instead of hiding new chat', () => {
    const wrapper = mount(AIChat, {
      global: { plugins: [ElementPlus] },
    })
    expect(wrapper.find('.mobile-sidebar-btn').exists()).toBe(true)
    expect(wrapper.find('.desktop-sidebar').exists()).toBe(true)
  })

  it('regenerates the searched message instead of the filtered index', async () => {
    vi.useFakeTimers()
    messages.value = appleBananaMessages()
    vi.mocked(sendChatMessage).mockResolvedValue({ answer: '新答案' })
    const wrapper = mount(AIChat, {
      global: { plugins: [ElementPlus] },
    })
    await flushPromises()
    await wrapper.get('.search-input input').setValue('香蕉')
    await flushPromises()

    await wrapper.get('[data-action=regenerate]').trigger('click')
    await flushPromises()
    await vi.runAllTimersAsync()
    await flushPromises()

    expect(sendChatMessage).toHaveBeenCalledWith('香蕉问题', 'a', { replaceLast: true })
    expect(messages.value[1].content).toBe('苹果回答')
    expect(messages.value[3].content).toContain('新答案')
  })

  it('rolls back both bubbles when send fails without a second toast', async () => {
    const errorSpy = vi.spyOn(ElMessage, 'error')
    vi.mocked(sendChatMessage).mockRejectedValue(new Error('fail'))
    const wrapper = mount(AIChat, {
      global: { plugins: [ElementPlus] },
    })
    await flushPromises()
    await wrapper.get('textarea').setValue('新问题')
    await wrapper.get('.send-btn').trigger('click')
    await flushPromises()

    expect(messages.value.map((item) => item.content)).toEqual(['A问题', 'A回答'])
    expect(errorSpy).not.toHaveBeenCalled()
    errorSpy.mockRestore()
  })

  it('does not write an in-flight reply onto a switched session', async () => {
    let resolveSend: (value: unknown) => void = () => undefined
    vi.mocked(sendChatMessage).mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveSend = resolve
        }),
    )
    const wrapper = mount(AIChat, {
      global: { plugins: [ElementPlus] },
    })
    await flushPromises()
    await wrapper.get('textarea').setValue('在途问题')
    await wrapper.get('.send-btn').trigger('click')
    await flushPromises()

    loadSeq.value += 1
    activeId.value = 'b'
    messages.value = []
    resolveSend({ answer: '迟到的回答' })
    await flushPromises()

    expect(messages.value).toEqual([])
    expect(refreshList).toHaveBeenCalled()
  })

  it('shows no-match copy instead of the welcome page', async () => {
    const wrapper = mount(AIChat, {
      global: { plugins: [ElementPlus] },
    })
    await flushPromises()
    await wrapper.get('.search-input input').setValue('zzzzz')
    await flushPromises()

    expect(wrapper.text()).toContain('无匹配消息')
    expect(wrapper.text()).not.toContain('你好，我是你的智能助手')
    expect(wrapper.find('.empty-welcome').exists()).toBe(false)
  })
})
