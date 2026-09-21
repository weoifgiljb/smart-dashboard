export enum ChatMessageType {
  User = 'user',
  Ai = 'ai',
}

export interface ChatMessage {
  type: ChatMessageType
  content: string
  time: string
}

export interface Conversation {
  id: string
  userId: string
  title: string
  createdAt: string
  updatedAt: string
}

export interface ChatTurn {
  id: string
  conversationId: string
  question: string
  answer: string
  createTime: string
}
