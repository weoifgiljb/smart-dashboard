import request from './request'
import { getAccessToken } from './authTokens'
import type { ChatTurn, Conversation } from '@/types/chat'

export type { ChatTurn, Conversation }

export const listConversations = () => {
  return request.get('/ai/conversations') as Promise<Conversation[]>
}

export const createConversation = () => {
  return request.post('/ai/conversations') as Promise<Conversation>
}

export const renameConversation = (id: string, title: string) => {
  return request.patch(`/ai/conversations/${id}`, { title }) as Promise<Conversation>
}

export const deleteConversation = (id: string) => {
  return request.delete(`/ai/conversations/${id}`) as Promise<void>
}

export const listConversationMessages = (id: string) => {
  return request.get(`/ai/conversations/${id}/messages`) as Promise<ChatTurn[]>
}

export const sendChatMessage = (
  question: string,
  conversationId: string,
  options?: { replaceLast?: boolean },
) => {
  return request.post('/ai/chat', {
    question,
    conversationId,
    replaceLast: options?.replaceLast === true,
  }) as Promise<unknown>
}

export const getChatHistory = () => {
  return request.get('/ai/history') as Promise<ChatTurn[]>
}

export const generateBookImage = (bookId: string) => {
  return request.post(`/ai/image/book/${bookId}`)
}

export const generateWordImage = (wordId: string) => {
  return request.post(`/ai/image/word/${wordId}`)
}

export async function streamChatMessage(
  question: string,
  conversationId: string,
  onChunk: (text: string) => void,
  options?: { replaceLast?: boolean },
): Promise<void> {
  const apiBase = import.meta.env.VITE_API_BASE || '/api'
  const token = getAccessToken() || ''
  const res = await fetch(`${apiBase}/ai/chat/stream`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream, text/plain, */*',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({
      question,
      conversationId,
      replaceLast: options?.replaceLast === true,
    }),
  })
  if (!res.ok || !res.body) {
    throw new Error('流式接口不可用')
  }
  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  // eslint-disable-next-line no-constant-condition
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    const chunk = decoder.decode(value, { stream: true })
    onChunk(chunk)
  }
}
