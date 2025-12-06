import request from './request'

export const sendChatMessage = (question: string, conversationId?: string) => {
  return request.post('/ai/chat', { question, conversationId })
}

export const getChatHistory = (conversationId?: string) => {
  return request.get('/ai/history', { params: { conversationId } })
}

export const createConversation = (title?: string) => {
  return request.post('/ai/conversations', { title })
}

export const getConversations = () => {
  return request.get('/ai/conversations')
}

export const generateBookImage = (bookId: string) => {
  return request.post(`/ai/image/book/${bookId}`)
}

export const generateWordImage = (wordId: string) => {
  return request.post(`/ai/image/word/${wordId}`)
}

// 流式聊天（优先使用，后端不支持时由调用方回退到 sendChatMessage）
export async function streamChatMessage(
  question: string,
  onChunk: (text: string) => void,
  conversationId?: string,
): Promise<void> {
  const apiBase = (import.meta as any).env?.VITE_API_BASE || '/api'
  const token = localStorage.getItem('token') || ''

  // Construct body with conversationId
  const body = JSON.stringify({ question, conversationId })

  const res = await fetch(`${apiBase}/ai/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream, text/plain, */*',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body,
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
