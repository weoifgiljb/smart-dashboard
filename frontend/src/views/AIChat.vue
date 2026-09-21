<template>
  <div class="ai-chat-page">
    <div class="chat-layout">
      <ChatSidebar
        class="desktop-sidebar"
        :conversations="conversations"
        :active-id="activeId"
        @new="onNewConversation"
        @export="exportChat"
        @select="onSelect"
        @rename="onRename"
        @delete="onDelete"
      />

      <div class="chat-main">
        <div class="chat-header">
          <div class="model-info">
            <el-button class="mobile-sidebar-btn" text @click="sidebarOpen = true">
              <el-icon><Menu /></el-icon>
            </el-button>
            <span class="model-name">AI 助手</span>
            <el-tag size="small" type="success" effect="light" round>Online</el-tag>
          </div>
          <div class="header-actions">
            <el-input
              v-model="searchQuery"
              placeholder="搜索..."
              prefix-icon="Search"
              clearable
              class="search-input"
            />
          </div>
        </div>

        <div ref="messagesRef" class="messages-container">
          <div v-if="messages.length === 0 && !loading && !loadingMessages" class="empty-welcome">
            <div class="welcome-icon">✨</div>
            <h2>你好，我是你的智能助手</h2>
            <p>我可以帮你解答问题、制定计划、翻译文本或提供建议。</p>

            <div class="suggestions-grid">
              <div class="suggestion-card" @click="applyPreset(presetQuestions[0])">
                {{ presetQuestions[0] }}
              </div>
              <div class="suggestion-card" @click="applyPreset(presetQuestions[1])">
                {{ presetQuestions[1] }}
              </div>
              <div class="suggestion-card" @click="applyPreset(presetQuestions[2])">
                {{ presetQuestions[2] }}
              </div>
              <div class="suggestion-card" @click="applyPreset(presetQuestions[3])">
                {{ presetQuestions[3] }}
              </div>
            </div>
          </div>

          <div
            v-else-if="filteredMessages.length === 0 && !loading && !loadingMessages"
            class="empty-search"
          >
            无匹配消息
          </div>

          <ChatMessageList
            v-else
            :messages="filteredMessages"
            :loading="loading || loadingMessages"
            @copy="copyText"
            @regenerate="regenerateResponse"
          />
        </div>

        <div class="input-area">
          <div class="input-box">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :autosize="{ minRows: 1, maxRows: 5 }"
              placeholder="输入消息... (Shift+Enter 换行)"
              class="custom-textarea"
              @keydown.enter.exact.prevent="onEnter"
              @keydown.shift.enter.stop
            />
            <el-button
              type="primary"
              circle
              class="send-btn"
              :disabled="!inputMessage.trim() || loading"
              @click="sendMessage"
            >
              <el-icon><Promotion /></el-icon>
            </el-button>
          </div>
          <div class="input-footer">
            <span>AI 生成内容仅供参考</span>
          </div>
        </div>
      </div>
    </div>

    <el-drawer v-model="sidebarOpen" title="对话" size="280px" direction="ltr" class="chat-drawer">
      <ChatSidebar
        :conversations="conversations"
        :active-id="activeId"
        @new="onNewConversationFromDrawer"
        @export="exportChat"
        @select="onSelectFromDrawer"
        @rename="onRename"
        @delete="onDelete"
      />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Menu, Promotion } from '@element-plus/icons-vue'
import ChatMessageList from '@/components/ChatMessageList.vue'
import ChatSidebar from '@/components/ChatSidebar.vue'
import { sendChatMessage, streamChatMessage } from '@/api/ai'
import { useConversations } from '@/composables/useConversations'
import { ChatMessageType, type ChatMessage, type Conversation } from '@/types/chat'

const {
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
} = useConversations()

const inputMessage = ref('')
const loading = ref(false)
const messagesRef = ref<HTMLElement>()
const searchQuery = ref('')
const sidebarOpen = ref(false)

const presetQuestions = [
  '📅 帮我规划今天的日程',
  '🍅 解释一下番茄工作法',
  '📝 帮我写一份周报摘要',
  '💪 制定一周健身计划',
] as const

const filteredMessages = computed(() => {
  if (!searchQuery.value.trim()) return messages.value
  const query = searchQuery.value.toLowerCase()
  return messages.value.filter((msg) => msg.content.toLowerCase().includes(query))
})

onMounted(async () => {
  await ensureActive()
  await scrollToBottom()
})

const onEnter = () => {
  if (!loading.value && inputMessage.value.trim()) {
    sendMessage()
  }
}

function chatAnswer(response: unknown): string {
  if (typeof response === 'string') return response
  if (response && typeof response === 'object') {
    const record = response as Record<string, unknown>
    if (typeof record.answer === 'string') return record.answer
    if (typeof record.text === 'string') return record.text
    if (typeof record.content === 'string') return record.content
  }
  return ''
}

const typeStream = async (fullText: string, onChunk: (s: string) => void) => {
  const chunks = fullText.split(/(\s+|[,.，。!！?？])/).filter(Boolean)
  for (const c of chunks) {
    onChunk(c)
    await new Promise((r) => setTimeout(r, Math.min(80, 20 + c.length * 5)))
  }
}

async function requireConversationId() {
  if (activeId.value) return activeId.value
  const created = await createConversation()
  return created.id
}

function stillCurrent(startedId: string, startedSeq: number) {
  return activeId.value === startedId && loadSeq.value === startedSeq
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || loading.value) return

  const question = inputMessage.value
  inputMessage.value = ''
  const conversationId = await requireConversationId()
  const startedSeq = loadSeq.value
  const userMsg: ChatMessage = {
    type: ChatMessageType.User,
    content: question,
    time: new Date().toISOString(),
  }
  const aiMsg: ChatMessage = {
    type: ChatMessageType.Ai,
    content: '',
    time: new Date().toISOString(),
  }
  messages.value.push(userMsg, aiMsg)
  const insertedAt = messages.value.length - 2

  await scrollToBottom()
  loading.value = true

  const rollbackSend = () => {
    if (!stillCurrent(conversationId, startedSeq)) return
    if (messages.value.length === insertedAt + 2) {
      messages.value.splice(insertedAt, 2)
    }
  }

  try {
    try {
      await streamChatMessage(question, conversationId, async (chunk) => {
        if (!stillCurrent(conversationId, startedSeq)) return
        aiMsg.content += chunk
        await scrollToBottom()
      })
    } catch (e) {
      console.warn('Stream failed, falling back to normal request:', e)
      const response = await sendChatMessage(question, conversationId)
      if (!stillCurrent(conversationId, startedSeq)) {
        await refreshList()
        return
      }
      await typeStream(chatAnswer(response), (chunk) => {
        if (!stillCurrent(conversationId, startedSeq)) return
        aiMsg.content += chunk
      })
    }
    await refreshList()
  } catch (error: unknown) {
    console.error(error)
    rollbackSend()
  } finally {
    loading.value = false
  }
}

const scrollToBottom = async () => {
  await nextTick()
  const el = messagesRef.value
  if (!el || typeof el.scrollTo !== 'function') return
  el.scrollTo({
    top: el.scrollHeight,
    behavior: 'smooth',
  })
}

const applyPreset = (q: string) => {
  inputMessage.value = q
}

const copyText = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}

const regenerateResponse = async (aiMessage: ChatMessage) => {
  if (loading.value) return
  const index = messages.value.indexOf(aiMessage)
  if (index <= 0) return
  const userMsg = messages.value[index - 1]
  if (userMsg.type !== ChatMessageType.User) return

  const question = userMsg.content
  const conversationId = await requireConversationId()
  const startedSeq = loadSeq.value
  const previousContent = aiMessage.content
  aiMessage.content = ''
  loading.value = true
  try {
    const response = await sendChatMessage(question, conversationId, { replaceLast: true })
    if (!stillCurrent(conversationId, startedSeq)) {
      await refreshList()
      return
    }
    await typeStream(chatAnswer(response), (chunk) => {
      if (!stillCurrent(conversationId, startedSeq)) return
      aiMessage.content += chunk
    })
    await refreshList()
  } catch {
    if (stillCurrent(conversationId, startedSeq) && !aiMessage.content) {
      aiMessage.content = previousContent
    }
  } finally {
    loading.value = false
  }
}

const onNewConversation = async () => {
  await createConversation()
  await scrollToBottom()
}

const onNewConversationFromDrawer = async () => {
  await onNewConversation()
  sidebarOpen.value = false
}

const onSelect = async (conversation: Conversation) => {
  await selectConversation(conversation.id)
  await scrollToBottom()
}

const onSelectFromDrawer = async (conversation: Conversation) => {
  await onSelect(conversation)
  sidebarOpen.value = false
}

const onRename = async (conversation: Conversation) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入新的会话标题', '重命名', {
      inputValue: conversation.title,
      inputPattern: /^(?!\s*$).{1,40}$/,
      inputErrorMessage: '标题需为 1–40 字',
      confirmButtonText: '保存',
      cancelButtonText: '取消',
    })
    await rename(conversation.id, String(value).trim())
  } catch {
    // cancelled
  }
}

const onDelete = async (conversation: Conversation) => {
  try {
    await ElMessageBox.confirm('删除后无法恢复', '删除对话', { type: 'warning' })
    await remove(conversation.id)
  } catch {
    // cancelled
  }
}

const exportChat = () => {
  if (!messages.value.length) return
  let content = '# 对话记录\n\n'
  messages.value.forEach((m) => {
    const speaker = m.type === ChatMessageType.User ? 'User' : 'AI'
    content += `### ${speaker}\n${m.content}\n\n`
  })
  const blob = new Blob([content], { type: 'text/markdown' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `chat_${Date.now()}.md`
  a.click()
}
</script>

<style scoped lang="less">
.ai-chat-page {
  height: calc(100vh - 20px);
  padding: 10px;
  box-sizing: border-box;
  background: var(--app-bg);
}

.chat-layout {
  display: flex;
  height: 100%;
  background: var(--color-bg-elevated);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: var(--shadow-md);
  border: 1px solid var(--border);
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  min-width: 0;
}

.chat-header {
  height: 60px;
  border-bottom: 1px solid var(--color-bg-muted);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
}

.model-info {
  display: flex;
  align-items: center;
  gap: 4px;
}

.model-name {
  font-weight: 700;
  margin-right: 8px;
  color: var(--app-text);
}

.mobile-sidebar-btn {
  display: none;
}

.search-input {
  width: 200px;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: var(--color-bg-elevated);
}

.empty-welcome,
.empty-search {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: var(--text-secondary);
}

.welcome-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.suggestions-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-top: 32px;
  max-width: 600px;
  width: 100%;
}

.suggestion-card {
  padding: 16px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
  font-size: 14px;
}

.suggestion-card:hover {
  border-color: var(--primary);
  background: var(--color-info-soft);
}

.input-area {
  padding: 20px;
  background: var(--color-bg-elevated);
  border-top: 1px solid var(--color-bg-muted);
}

.input-box {
  position: relative;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 4px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.02);
  transition: border-color 0.2s;
}

.input-box:focus-within {
  border-color: var(--primary);
  box-shadow: 0 2px 8px rgba(var(--primary-rgb), 0.1);
}

.custom-textarea :deep(.el-textarea__inner) {
  border: none;
  box-shadow: none;
  background: transparent;
  padding: 10px 50px 10px 12px;
  resize: none;
}

.send-btn {
  position: absolute;
  right: 8px;
  bottom: 8px;
}

.input-footer {
  text-align: center;
  margin-top: 8px;
  font-size: 11px;
  color: var(--color-text-muted);
}

.chat-drawer :deep(.el-drawer__body) {
  padding: 0;
}

.chat-drawer :deep(.chat-sidebar) {
  width: 100%;
  border-right: none;
}

@media (max-width: 768px) {
  .desktop-sidebar {
    display: none;
  }
  .mobile-sidebar-btn {
    display: inline-flex;
  }
  .search-input {
    width: 140px;
  }
}
</style>
