import { ref } from 'vue'
import {
  createConversation as apiCreate,
  deleteConversation as apiDelete,
  listConversationMessages,
  listConversations as apiList,
  renameConversation as apiRename,
} from '@/api/ai'
import { useUserStore } from '@/store/user'
import { ChatMessageType, type ChatMessage, type ChatTurn, type Conversation } from '@/types/chat'

export function turnsToMessages(turns: ChatTurn[]): ChatMessage[] {
  const messages: ChatMessage[] = []
  for (const turn of turns) {
    messages.push({
      type: ChatMessageType.User,
      content: turn.question,
      time: turn.createTime,
    })
    messages.push({
      type: ChatMessageType.Ai,
      content: turn.answer,
      time: turn.createTime,
    })
  }
  return messages
}

export function activeConversationStorageKey(userId: string) {
  return `aiActiveConversationId:${userId}`
}

export function useConversations() {
  const userStore = useUserStore()
  const conversations = ref<Conversation[]>([])
  const activeId = ref<string | null>(null)
  const messages = ref<ChatMessage[]>([])
  const loadingList = ref(false)
  const loadingMessages = ref(false)
  const loadSeq = ref(0)

  function currentUserId() {
    return userStore.user?.id || 'anon'
  }

  function persistActive(id: string | null) {
    const key = activeConversationStorageKey(currentUserId())
    if (!id) {
      sessionStorage.removeItem(key)
      return
    }
    sessionStorage.setItem(key, id)
  }

  function readPersistedActive() {
    return sessionStorage.getItem(activeConversationStorageKey(currentUserId()))
  }

  async function refreshList() {
    conversations.value = (await apiList()) || []
  }

  async function selectConversation(id: string) {
    const seq = ++loadSeq.value
    activeId.value = id
    persistActive(id)
    messages.value = []
    loadingMessages.value = true
    try {
      const turns = await listConversationMessages(id)
      if (seq !== loadSeq.value) return
      messages.value = turnsToMessages(turns || [])
    } finally {
      if (seq === loadSeq.value) loadingMessages.value = false
    }
  }

  async function ensureActive() {
    loadingList.value = true
    try {
      await refreshList()
      const persisted = readPersistedActive()
      const existing = conversations.value.find((item) => item.id === persisted)
      if (existing) {
        await selectConversation(existing.id)
        return
      }
      if (conversations.value.length > 0) {
        await selectConversation(conversations.value[0].id)
        return
      }
      const created = await apiCreate()
      await refreshList()
      await selectConversation(created.id)
    } finally {
      loadingList.value = false
    }
  }

  async function createConversation() {
    const created = await apiCreate()
    await refreshList()
    await selectConversation(created.id)
    return created
  }

  async function rename(id: string, title: string) {
    const updated = await apiRename(id, title)
    await refreshList()
    return updated
  }

  async function remove(id: string) {
    await apiDelete(id)
    if (activeId.value === id) {
      persistActive(null)
      activeId.value = null
      messages.value = []
    }
    await refreshList()
    if (conversations.value.length > 0) {
      await selectConversation(conversations.value[0].id)
      return
    }
    const created = await apiCreate()
    await refreshList()
    await selectConversation(created.id)
  }

  return {
    conversations,
    activeId,
    messages,
    loadingList,
    loadingMessages,
    loadSeq,
    ensureActive,
    createConversation,
    selectConversation,
    rename,
    remove,
    refreshList,
  }
}
