import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  createConversation,
  deleteConversation,
  listConversationMessages,
  listConversations,
  renameConversation,
} from '@/api/ai'
import {
  activeConversationStorageKey,
  turnsToMessages,
  useConversations,
} from '@/composables/useConversations'
import { useUserStore } from '@/store/user'
import { ChatMessageType, type ChatTurn, type Conversation } from '@/types/chat'

vi.mock('@/api/ai', () => ({
  listConversations: vi.fn(),
  createConversation: vi.fn(),
  listConversationMessages: vi.fn(),
  renameConversation: vi.fn(),
  deleteConversation: vi.fn(),
}))

function conv(id: string, title: string): Conversation {
  return { id, userId: 'u1', title, createdAt: '2026-01-01', updatedAt: '2026-01-01' }
}

function turn(id: string, conversationId: string, question: string, answer: string): ChatTurn {
  return { id, conversationId, question, answer, createTime: '2026-01-01T00:00:00' }
}

describe('useConversations', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    const store = useUserStore()
    store.setUser({ id: 'u1', username: 'alice', email: 'a@b.c', createTime: '2026-01-01' })
    sessionStorage.clear()
    vi.mocked(listConversations).mockReset()
    vi.mocked(createConversation).mockReset()
    vi.mocked(listConversationMessages).mockReset()
    vi.mocked(renameConversation).mockReset()
    vi.mocked(deleteConversation).mockReset()
  })

  it('turnsToMessages keeps user then ai order', () => {
    const messages = turnsToMessages([turn('1', 'a', '问', '答')])
    expect(messages[0]).toMatchObject({ type: ChatMessageType.User, content: '问' })
    expect(messages[1]).toMatchObject({ type: ChatMessageType.Ai, content: '答' })
  })

  it('createConversation keeps the previous session in the list', async () => {
    const sessionA = conv('a', '对话A')
    const sessionB = conv('b', '新对话')
    vi.mocked(listConversations)
      .mockResolvedValueOnce([sessionA])
      .mockResolvedValueOnce([sessionB, sessionA])
    vi.mocked(listConversationMessages).mockImplementation(async (id: string) => {
      if (id === 'a') return [turn('t1', 'a', 'A问题', 'A回答')]
      return []
    })
    vi.mocked(createConversation).mockResolvedValue(sessionB)

    const api = useConversations()
    await api.ensureActive()
    expect(api.messages.value.map((m) => m.content)).toEqual(['A问题', 'A回答'])

    await api.createConversation()

    expect(api.conversations.value.map((item) => item.id)).toEqual(['b', 'a'])
    expect(api.conversations.value.some((item) => item.title === '对话A')).toBe(true)
    expect(api.activeId.value).toBe('b')
    expect(api.messages.value).toEqual([])
  })

  it('selectConversation isolates messages by session', async () => {
    vi.mocked(listConversations).mockResolvedValue([conv('b', 'B'), conv('a', 'A')])
    vi.mocked(listConversationMessages).mockImplementation(async (id: string) => {
      if (id === 'a') return [turn('t1', 'a', 'A问题', 'A回答')]
      return [turn('t2', 'b', 'B问题', 'B回答')]
    })

    const api = useConversations()
    await api.ensureActive()
    expect(api.activeId.value).toBe('b')
    expect(api.messages.value.map((m) => m.content)).toEqual(['B问题', 'B回答'])

    await api.selectConversation('a')
    expect(api.messages.value.map((m) => m.content)).toEqual(['A问题', 'A回答'])
    expect(api.messages.value.some((m) => m.content.includes('B'))).toBe(false)
  })

  it('restores the persisted active conversation after reload', async () => {
    sessionStorage.setItem(activeConversationStorageKey('u1'), 'a')
    vi.mocked(listConversations).mockResolvedValue([conv('b', 'B'), conv('a', 'A')])
    vi.mocked(listConversationMessages).mockImplementation(async (id: string) => {
      if (id === 'a') return [turn('t1', 'a', 'A问题', 'A回答')]
      return [turn('t2', 'b', 'B问题', 'B回答')]
    })

    const api = useConversations()
    await api.ensureActive()
    expect(api.activeId.value).toBe('a')
    expect(api.messages.value[0].content).toBe('A问题')
  })

  it('removing the current session switches to the remaining one', async () => {
    vi.mocked(listConversations)
      .mockResolvedValueOnce([conv('b', 'B'), conv('a', 'A')])
      .mockResolvedValueOnce([conv('a', 'A')])
    vi.mocked(listConversationMessages).mockImplementation(async (id: string) => {
      if (id === 'a') return [turn('t1', 'a', 'A问题', 'A回答')]
      return [turn('t2', 'b', 'B问题', 'B回答')]
    })
    vi.mocked(deleteConversation).mockResolvedValue(undefined)

    const api = useConversations()
    await api.ensureActive()
    expect(api.activeId.value).toBe('b')
    await api.remove('b')
    expect(deleteConversation).toHaveBeenCalledWith('b')
    expect(api.activeId.value).toBe('a')
    expect(api.messages.value.map((m) => m.content)).toEqual(['A问题', 'A回答'])
  })

  it('clears messages immediately when switching sessions', async () => {
    let release!: () => void
    vi.mocked(listConversations).mockResolvedValue([conv('b', 'B'), conv('a', 'A')])
    vi.mocked(listConversationMessages).mockImplementation(async (id: string) => {
      if (id === 'a') {
        await new Promise<void>((resolve) => {
          release = resolve
        })
        return [turn('t1', 'a', 'A问题', 'A回答')]
      }
      return [turn('t2', 'b', 'B问题', 'B回答')]
    })

    const api = useConversations()
    await api.ensureActive()
    expect(api.messages.value.map((m) => m.content)).toEqual(['B问题', 'B回答'])

    const pending = api.selectConversation('a')
    expect(api.messages.value).toEqual([])
    expect(api.loadingMessages.value).toBe(true)
    release()
    await pending
    expect(api.messages.value.map((m) => m.content)).toEqual(['A问题', 'A回答'])
  })
})
