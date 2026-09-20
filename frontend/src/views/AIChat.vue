<template>
  <div class="ai-chat-page">
    <div class="chat-layout">
      <!-- 侧边栏（可选：如果有历史记录列表，可放在这里，目前仅作为装饰或未来扩展） -->
      <div class="chat-sidebar">
        <div class="sidebar-header">
          <el-button type="primary" class="new-chat-btn" @click="clearChat">
            <el-icon><Plus /></el-icon> 新对话
          </el-button>
        </div>
        <div class="history-list">
          <div class="history-label">最近对话</div>
          <!-- 模拟历史记录 -->
          <div class="history-item active">
            <el-icon><ChatLineSquare /></el-icon>
            <span class="history-title">当前对话</span>
          </div>
        </div>
        <div class="sidebar-footer">
          <el-button link @click="exportChat"
            ><el-icon><Download /></el-icon> 导出记录</el-button
          >
        </div>
      </div>

      <!-- 主聊天区 -->
      <div class="chat-main">
        <!-- 顶部栏 -->
        <div class="chat-header">
          <div class="model-info">
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

        <!-- 消息列表 -->
        <div ref="messagesRef" class="messages-container">
          <div v-if="filteredMessages.length === 0 && !loading" class="empty-welcome">
            <div class="welcome-icon">✨</div>
            <h2>你好，我是你的智能助手</h2>
            <p>我可以帮你解答问题、制定计划、翻译文本或提供建议。</p>

            <div class="suggestions-grid">
              <div
                v-for="(q, i) in presetQuestions"
                :key="i"
                class="suggestion-card"
                @click="applyPreset(q)"
              >
                {{ q }}
              </div>
            </div>
          </div>

          <div v-else class="messages-list">
            <div
              v-for="(msg, index) in filteredMessages"
              :key="index"
              :class="['message-row', msg.type]"
            >
              <div class="avatar">
                <div class="avatar-img" :class="msg.type">
                  <el-icon v-if="msg.type === 'ai'"><Cpu /></el-icon>
                  <el-icon v-else><User /></el-icon>
                </div>
              </div>
              <div class="message-bubble">
                <div
                  class="bubble-content markdown-body"
                  v-html="renderMarkdown(msg.content)"
                ></div>
                <div class="bubble-footer">
                  <span class="time">{{ formatTime(msg.time) }}</span>
                  <div class="actions">
                    <el-icon class="action-icon" @click="copyText(msg.content)"
                      ><CopyDocument
                    /></el-icon>
                    <el-icon
                      v-if="msg.type === 'ai'"
                      class="action-icon"
                      @click="regenerateResponse(index)"
                      ><Refresh
                    /></el-icon>
                  </div>
                </div>
              </div>
            </div>

            <!-- Loading Indicator -->
            <div v-if="loading" class="message-row ai">
              <div class="avatar">
                <div class="avatar-img ai">
                  <el-icon><Cpu /></el-icon>
                </div>
              </div>
              <div class="message-bubble loading-bubble">
                <div class="typing-dots"><span></span><span></span><span></span></div>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Download,
  ChatLineSquare,
  Cpu,
  User,
  CopyDocument,
  Refresh,
  Promotion,
} from '@element-plus/icons-vue'
import { sendChatMessage, getChatHistory, streamChatMessage } from '@/api/ai'

const messages = ref<any[]>([])
const inputMessage = ref('')
const loading = ref(false)
const messagesRef = ref<HTMLElement>()
const searchQuery = ref('')

const presetQuestions = ref<string[]>([
  '📅 帮我规划今天的日程',
  '🍅 解释一下番茄工作法',
  '📝 帮我写一份周报摘要',
  '💪 制定一周健身计划',
])

// 过滤消息
const filteredMessages = computed(() => {
  if (!searchQuery.value.trim()) return messages.value
  const query = searchQuery.value.toLowerCase()
  return messages.value.filter((msg) => msg.content.toLowerCase().includes(query))
})

onMounted(async () => {
  await loadHistory()
})

const onEnter = () => {
  if (!loading.value && inputMessage.value.trim()) {
    sendMessage()
  }
}

const loadHistory = async () => {
  try {
    const data: any = await getChatHistory()
    const allMessages: any[] = []
    data.forEach((item: any) => {
      allMessages.push({
        type: 'user',
        content: item.question,
        time: item.createTime,
      })
      allMessages.push({
        type: 'ai',
        content: item.answer,
        time: item.createTime,
      })
    })
    messages.value = allMessages.sort(
      (a: any, b: any) => new Date(a.time).getTime() - new Date(b.time).getTime(),
    )
    await scrollToBottom()
  } catch (error) {
    console.error('获取聊天历史失败', error)
  }
}

const typeStream = async (fullText: string, onChunk: (s: string) => void) => {
  const chunks = fullText.split(/(\s+|[,.，。!！?？])/).filter(Boolean)
  for (const c of chunks) {
    onChunk(c)
    await new Promise((r) => setTimeout(r, Math.min(80, 20 + c.length * 5)))
  }
}

const persist = () => {
  try {
    localStorage.setItem('aiChatMessages', JSON.stringify(messages.value))
  } catch {
    /* ignore */
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim()) return

  const question = inputMessage.value
  inputMessage.value = ''
  messages.value.push({
    type: 'user',
    content: question,
    time: new Date(),
  })

  await scrollToBottom()
  loading.value = true

  try {
    const aiMsg = {
      type: 'ai',
      content: '',
      time: new Date(),
    } as any
    messages.value.push(aiMsg)
    await scrollToBottom()

    try {
      await streamChatMessage(question, async (chunk) => {
        aiMsg.content += chunk
        await scrollToBottom()
      })
    } catch (e) {
      console.warn('Stream failed, falling back to normal request:', e)
      const response: any = await sendChatMessage(question)
      // 兼容多种返回格式
      const ans =
        typeof response === 'string'
          ? response
          : response.answer || response.text || response.content || ''
      await typeStream(String(ans), (chunk) => {
        aiMsg.content += chunk
      })
    }
    persist()
  } catch (error: any) {
    // 如果是取消请求或非关键错误，可以忽略
    console.error(error)
    ElMessage.error(error.response?.data?.message || '发送失败')
    // 移除失败的消息占位
    messages.value.pop()
  } finally {
    loading.value = false
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTo({
      top: messagesRef.value.scrollHeight,
      behavior: 'smooth',
    })
  }
}

const formatTime = (time: Date | string) => {
  const date = typeof time === 'string' ? new Date(time) : time
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const applyPreset = (q: string) => {
  inputMessage.value = q
  // remove emoji if present at start for cleaner input
  // 使用简单的 Unicode 范围匹配 Emoji，避免复杂的代理对问题
  if (/^[\u2000-\u3300]/.test(q) || /^[\uD83C-\uD83E]/.test(q)) {
    // keep it or remove it, user preference. Let's keep it.
  }
}

const renderMarkdown = (text: string) => {
  if (!text) return ''
  let html = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

  // Code blocks
  html = html.replace(/```([\s\S]*?)```/g, (_, code) => `<pre><code>${code.trim()}</code></pre>`)
  // Inline code
  html = html.replace(/`([^`\n]+?)`/g, '<code>$1</code>')
  // Bold
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  // Line breaks
  html = html.replace(/\n/g, '<br/>')
  return html
}

const copyText = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}

const regenerateResponse = async (index: number) => {
  if (loading.value) return
  const userMsgIndex = index - 1
  if (userMsgIndex < 0 || messages.value[userMsgIndex].type !== 'user') return

  const question = messages.value[userMsgIndex].content
  messages.value.splice(index, 1) // remove old

  loading.value = true
  try {
    const aiMsg = { type: 'ai', content: '', time: new Date() } as any
    messages.value.splice(index, 0, aiMsg)
    await scrollToBottom()

    // Logic same as sendMessage
    try {
      await streamChatMessage(question, async (chunk) => {
        aiMsg.content += chunk
        await scrollToBottom()
      })
    } catch (e) {
      console.warn('Stream regeneration failed:', e)
      const response: any = await sendChatMessage(question)
      const ans =
        typeof response === 'string'
          ? response
          : response.answer || response.text || response.content || ''
      await typeStream(String(ans), (chunk) => {
        aiMsg.content += chunk
      })
    }
    persist()
  } catch {
    ElMessage.error('重新生成失败')
    // 恢复原来的空消息或删除
    if (!messages.value[index].content) {
      messages.value.splice(index, 1)
    }
  } finally {
    loading.value = false
  }
}

const clearChat = async () => {
  try {
    await ElMessageBox.confirm('确定清空对话吗？', '提示', { type: 'warning' })
    messages.value = []
    persist()
  } catch {
    // user cancelled confirm
  }
}

const exportChat = () => {
  if (!messages.value.length) return
  let content = '# 对话记录\n\n'
  messages.value.forEach((m) => {
    content += `### ${m.type === 'user' ? 'User' : 'AI'}\n${m.content}\n\n`
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

/* Sidebar */
.chat-sidebar {
  width: 260px;
  background: var(--color-bg);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 20px;
}

.new-chat-btn {
  width: 100%;
  justify-content: flex-start;
  font-weight: 600;
}

.history-list {
  flex: 1;
  padding: 0 12px;
  overflow-y: auto;
}

.history-label {
  font-size: 12px;
  color: var(--text-light);
  margin-bottom: 8px;
  padding-left: 8px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.2s;
}

.history-item:hover {
  background: var(--color-border);
}

.history-item.active {
  background: var(--color-info-soft);
  color: var(--primary);
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid var(--border);
}

/* Main Chat */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
}

.chat-header {
  height: 60px;
  border-bottom: 1px solid var(--color-bg-muted);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
}

.model-name {
  font-weight: 700;
  margin-right: 8px;
  color: var(--app-text);
}

.search-input {
  width: 200px;
}

/* Messages */
.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: var(--color-bg-elevated);
}

.empty-welcome {
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

/* Message Rows */
.message-row {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.message-row.user {
  flex-direction: row-reverse;
}

.avatar-img {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.avatar-img.ai {
  background: var(--color-bg-muted);
  color: var(--primary);
}

.avatar-img.user {
  background: var(--primary-light);
  color: var(--primary);
}

.message-bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  position: relative;
  font-size: 15px;
  line-height: 1.6;
}

.message-row.ai .message-bubble {
  background: var(--color-bg);
  border-top-left-radius: 2px;
  color: var(--color-text);
}

.message-row.user .message-bubble {
  background: var(--primary);
  color: white;
  border-top-right-radius: 2px;
}

.bubble-content :deep(pre) {
  background: var(--color-text);
  color: var(--color-border);
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
}

.bubble-content :deep(code) {
  font-family: monospace;
}

.message-row.user .bubble-content :deep(a) {
  color: white;
  text-decoration: underline;
}

.bubble-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 6px;
  font-size: 11px;
  opacity: 0.7;
}

.actions {
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}

.message-bubble:hover .actions {
  opacity: 1;
}

.action-icon {
  cursor: pointer;
}

/* Loading Dots */
.loading-bubble {
  padding: 12px 20px;
}

.typing-dots span {
  display: inline-block;
  width: 6px;
  height: 6px;
  background: var(--color-text-muted);
  border-radius: 50%;
  margin: 0 2px;
  animation: typing 1.4s infinite both;
}

.typing-dots span:nth-child(1) {
  animation-delay: 0s;
}
.typing-dots span:nth-child(2) {
  animation-delay: 0.2s;
}
.typing-dots span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%,
  80%,
  100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

/* Input Area */
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

@media (max-width: 768px) {
  .chat-sidebar {
    display: none;
  }
  .message-bubble {
    max-width: 85%;
  }
}
</style>
