<template>
  <div class="ai-chat-page">
    <el-card class="chat-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon class="header-icon"><ChatDotRound /></el-icon>
            <h3>AI智能助手</h3>
            <el-tag size="small" type="success" effect="plain">在线</el-tag>
          </div>
          <div class="header-actions">
            <el-tooltip content="搜索对话" placement="bottom">
              <el-button link @click="showSearch = !showSearch">
                <el-icon><Search /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="导出对话" placement="bottom">
              <el-button link @click="exportChat">
                <el-icon><Download /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="清空对话" placement="bottom">
              <el-button link @click="clearChat">
                <el-icon><Delete /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </div>
      </template>

      <div class="chat-container">
        <!-- 搜索框 -->
        <el-collapse-transition>
          <div v-show="showSearch" class="search-bar">
            <el-input
              v-model="searchQuery"
              placeholder="搜索对话内容..."
              clearable
              @input="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
        </el-collapse-transition>

        <!-- 消息列表 -->
        <div class="chat-messages" ref="messagesRef">
          <div v-if="filteredMessages.length === 0" class="empty-state">
            <el-icon class="empty-icon"><ChatLineRound /></el-icon>
            <p>{{ searchQuery ? '未找到相关消息' : '开始与AI对话吧！' }}</p>
          </div>

          <div v-for="(msg, index) in filteredMessages" :key="index" :class="['message', msg.type]">
            <template v-if="msg.type === 'ai'">
              <div class="avatar">
                <el-avatar :size="40" class="ai-avatar">
                  <el-icon><Cpu /></el-icon>
                </el-avatar>
              </div>
              <div class="bubble">
                <div class="message-content" v-html="renderMarkdown(msg.content)"></div>
                <div class="message-meta">
                  <span class="message-time">{{ formatTime(msg.time) }}</span>
                  <div class="message-actions">
                    <el-button link size="small" @click="copyText(msg.content)">
                      <el-icon><DocumentCopy /></el-icon> 复制
                    </el-button>
                    <el-button link size="small" @click="regenerateResponse(index)">
                      <el-icon><Refresh /></el-icon> 重新生成
                    </el-button>
                  </div>
                </div>
              </div>
              <div class="avatar-placeholder"></div>
            </template>
            <template v-else>
              <div class="avatar-placeholder"></div>
              <div class="bubble">
                <div class="message-content" v-html="renderMarkdown(msg.content)"></div>
                <div class="message-meta">
                  <span class="message-time">{{ formatTime(msg.time) }}</span>
                  <div class="message-actions">
                    <el-button link size="small" @click="copyText(msg.content)">
                      <el-icon><DocumentCopy /></el-icon> 复制
                    </el-button>
                  </div>
                </div>
              </div>
              <div class="avatar">
                <el-avatar :size="40" class="user-avatar">
                  <el-icon><User /></el-icon>
                </el-avatar>
              </div>
            </template>
          </div>

          <!-- 加载动画 -->
          <div v-if="loading" class="message ai">
            <div class="avatar">
              <el-avatar :size="40" class="ai-avatar">
                <el-icon><Cpu /></el-icon>
              </el-avatar>
            </div>
            <div class="bubble">
              <div class="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
            <div class="avatar-placeholder"></div>
          </div>
        </div>

        <!-- 预设问题 -->
        <div v-if="messages.length === 0" class="preset-questions">
          <div class="preset-title">💡 试试这些问题</div>
          <div class="preset-grid">
            <div
              v-for="(q, i) in presetQuestions"
              :key="i"
              class="preset-card"
              @click="applyPreset(q)"
            >
              <el-icon class="preset-icon"><ChatLineSquare /></el-icon>
              <span>{{ q }}</span>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="chat-input-wrapper">
          <div class="chat-input">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="3"
              :autosize="{ minRows: 1, maxRows: 6 }"
              placeholder="输入您的问题... (Enter发送，Shift+Enter换行)"
              @keydown.enter.exact.prevent="onEnter"
              @keydown.shift.enter.stop
              :disabled="loading"
              class="input-textarea"
            />
            <div class="input-actions">
              <div class="input-hint">
                <el-text size="small" type="info">
                  {{ inputMessage.length }} / 2000
                </el-text>
              </div>
              <el-button
                type="primary"
                @click="sendMessage"
                :loading="loading"
                :disabled="!inputMessage.trim()"
                round
              >
                <el-icon v-if="!loading"><Promotion /></el-icon>
                {{ loading ? '思考中...' : '发送' }}
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ChatDotRound,
  Search,
  Download,
  Delete,
  ChatLineRound,
  Cpu,
  User,
  DocumentCopy,
  Refresh,
  ChatLineSquare,
  Promotion
} from '@element-plus/icons-vue'
import { sendChatMessage, getChatHistory, streamChatMessage } from '@/api/ai'

const messages = ref<any[]>([])
const inputMessage = ref('')
const loading = ref(false)
const messagesRef = ref<HTMLElement>()
const showSearch = ref(false)
const searchQuery = ref('')

const presetQuestions = ref<string[]>([
  '今天如何更高效地安排待办？',
  '解释一下番茄工作法的最佳实践',
  '把这段中文翻译成英文：保持热爱，奔赴山海。',
  '给我一个3天英语学习计划',
  '帮我制定一周3次的健身计划'
])

// 过滤消息
const filteredMessages = computed(() => {
  if (!searchQuery.value.trim()) {
    return messages.value
  }
  const query = searchQuery.value.toLowerCase()
  return messages.value.filter(msg =>
    msg.content.toLowerCase().includes(query)
  )
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
        time: item.createTime
      })
      allMessages.push({
        type: 'ai',
        content: item.answer,
        time: item.createTime
      })
    })
    messages.value = allMessages.sort((a: any, b: any) =>
      new Date(a.time).getTime() - new Date(b.time).getTime()
    )
    await scrollToBottom()
  } catch (error) {
    console.error('获取聊天历史失败', error)
  }
}

const typeStream = async (fullText: string, onChunk: (s: string) => void) => {
  // 将文本切成小段模拟流式渲染
  const chunks = fullText.split(/(\s+|[,.，。!！?？])/).filter(Boolean)
  for (const c of chunks) {
    onChunk(c)
    await new Promise((r) => setTimeout(r, Math.min(120, 20 + c.length * 8)))
  }
}

const persist = () => {
  try {
    localStorage.setItem('aiChatMessages', JSON.stringify(messages.value))
  } catch {}
}

const restore = () => {
  try {
    const raw = localStorage.getItem('aiChatMessages')
    if (raw) {
      const arr = JSON.parse(raw)
      if (Array.isArray(arr)) {
        messages.value = arr
      }
    }
  } catch {}
}

const sendMessage = async () => {
  if (!inputMessage.value.trim()) {
    ElMessage.warning('请输入问题')
    return
  }

  if (inputMessage.value.length > 2000) {
    ElMessage.warning('问题长度不能超过2000字符')
    return
  }

  const question = inputMessage.value
  inputMessage.value = ''
  messages.value.push({
    type: 'user',
    content: question,
    time: new Date()
  })

  await scrollToBottom()
  loading.value = true

  try {
    // 优先流式，失败则回退
    const aiMsg = {
      type: 'ai',
      content: '',
      time: new Date()
    } as any
    messages.value.push(aiMsg)
    await scrollToBottom()
    try {
      await streamChatMessage(question, async (chunk) => {
        aiMsg.content += chunk
        await scrollToBottom()
      })
    } catch {
      const response: any = await sendChatMessage(question)
      await typeStream(String(response.answer || ''), (chunk) => {
        aiMsg.content += chunk
      })
    }
    persist()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '发送失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTo({
      top: messagesRef.value.scrollHeight,
      behavior: 'smooth'
    })
  }
}

const formatTime = (time: Date | string) => {
  const date = typeof time === 'string' ? new Date(time) : time
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  // 小于1分钟
  if (diff < 60000) {
    return '刚刚'
  }
  // 小于1小时
  if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`
  }
  // 今天
  if (date.toDateString() === now.toDateString()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  // 其他
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const applyPreset = (q: string) => {
  inputMessage.value = q
}

const renderMarkdown = (text: string) => {
  if (!text) return ''

  // 简单的文本渲染，避免复杂的Markdown解析
  let html = text
    // 转义HTML特殊字符
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')

  // 代码块（必须在行内代码之前处理）
  html = html.replace(/```([\s\S]*?)```/g, (_match: string, code: string) => {
    return `<pre><code>${code.trim()}</code></pre>`
  })

  // 行内代码
  html = html.replace(/`([^`\n]+?)`/g, '<code>$1</code>')

  // 粗体
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')

  // 斜体（避免与粗体冲突）
  html = html.replace(/(?<!\*)\*([^*\n]+?)\*(?!\*)/g, '<em>$1</em>')

  // 链接
  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')

  // 换行
  html = html.replace(/\n/g, '<br/>')

  return html
}

const copyText = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text || '')
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败')
  }
}

const regenerateResponse = async (index: number) => {
  if (loading.value) return

  // 找到对应的用户问题
  const userMsgIndex = index - 1
  if (userMsgIndex < 0 || messages.value[userMsgIndex].type !== 'user') {
    ElMessage.warning('无法重新生成')
    return
  }

  const question = messages.value[userMsgIndex].content

  // 删除旧的AI回复
  messages.value.splice(index, 1)

  loading.value = true
  try {
    const aiMsg = {
      type: 'ai',
      content: '',
      time: new Date()
    } as any
    messages.value.splice(index, 0, aiMsg)
    await scrollToBottom()
    try {
      await streamChatMessage(question, async (chunk) => {
        aiMsg.content += chunk
        await scrollToBottom()
      })
    } catch {
      const response: any = await sendChatMessage(question)
      await typeStream(String(response.answer || ''), (chunk) => {
        aiMsg.content += chunk
      })
    }
    persist()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '重新生成失败')
  } finally {
    loading.value = false
  }
}

const clearChat = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要清空所有对话记录吗？此操作不可恢复。',
      '清空对话',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    messages.value = []
    persist()
    ElMessage.success('对话已清空')
  } catch {
    // 用户取消
  }
}

const exportChat = () => {
  if (messages.value.length === 0) {
    ElMessage.warning('暂无对话记录')
    return
  }

  let content = '# AI对话记录\n\n'
  content += `导出时间: ${new Date().toLocaleString('zh-CN')}\n\n`
  content += '---\n\n'

  messages.value.forEach((msg, _index) => {
    const role = msg.type === 'user' ? '👤 用户' : '🤖 AI助手'
    const time = formatTime(msg.time)
    content += `## ${role} (${time})\n\n${msg.content}\n\n`
  })

  const blob = new Blob([content], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `AI对话记录_${new Date().getTime()}.md`
  a.click()
  URL.revokeObjectURL(url)

  ElMessage.success('对话已导出')
}

const handleSearch = () => {
  // 搜索时自动滚动到第一个匹配项
  nextTick(() => {
    if (filteredMessages.value.length > 0) {
      messagesRef.value?.scrollTo({ top: 0, behavior: 'smooth' })
    }
  })
}

// 持久化加载
restore()
</script>

<style scoped>
.ai-chat-page {
  padding: 20px;
  min-height: calc(100vh - 60px);
  height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-sizing: border-box;
}

.chat-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  font-size: 24px;
  color: #409eff;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.header-actions .el-button {
  font-size: 18px;
}

.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.search-bar {
  padding: 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: linear-gradient(to bottom, #f5f7fa 0%, #ffffff 100%);
  scroll-behavior: smooth;
}

.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.message {
  margin-bottom: 24px;
  display: flex;
  gap: 12px;
  align-items: flex-start;
  animation: fadeIn 0.3s ease-in;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message.user {
  flex-direction: row-reverse;
}

.message.ai {
  flex-direction: row;
}

.avatar {
  flex-shrink: 0;
  width: 40px;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 4px;
}

.avatar-placeholder {
  flex-shrink: 0;
  width: 40px;
}

.ai-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.user-avatar {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.message .bubble {
  flex: 1;
  max-width: calc(100% - 100px);
  min-width: 0;
}

.message-content {
  padding: 12px 16px;
  border-radius: 12px;
  word-wrap: break-word;
  word-break: break-word;
  line-height: 1.6;
  font-size: 14px;
  overflow-wrap: break-word;
}

.message.user .message-content {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 12px 12px 4px 12px;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.message.user .message-content :deep(code) {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}

.message.user .message-content :deep(pre) {
  background: rgba(0, 0, 0, 0.2);
}

.message.user .message-content :deep(pre code) {
  background: transparent;
  color: #fff;
}

.message.user .message-content :deep(a) {
  color: #fff;
  text-decoration: underline;
}

.message.ai .message-content {
  background: #fff;
  color: #333;
  border: 1px solid #e4e7ed;
  border-radius: 12px 12px 12px 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.message-content :deep(pre) {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  overflow-y: hidden;
  margin: 8px 0;
  max-width: 100%;
}

.message-content :deep(code) {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  word-break: break-all;
}

.message-content :deep(pre code) {
  background: transparent;
  padding: 0;
  white-space: pre;
  word-break: normal;
  overflow-wrap: normal;
}

.message-content :deep(a) {
  color: #409eff;
  text-decoration: none;
  word-break: break-all;
}

.message-content :deep(a:hover) {
  text-decoration: underline;
}

.message-content :deep(strong) {
  font-weight: 600;
}

.message-content :deep(em) {
  font-style: italic;
}

.message-meta {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.message-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.message:hover .message-actions {
  opacity: 1;
}

.message-time {
  color: #909399;
}

/* 打字机效果 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 12px 12px 12px 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #909399;
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.5;
  }
  30% {
    transform: translateY(-10px);
    opacity: 1;
  }
}

.preset-questions {
  padding: 20px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
}

.preset-title {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 12px;
}

.preset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.preset-card {
  padding: 12px 16px;
  background: linear-gradient(135deg, #f5f7fa 0%, #ffffff 100%);
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #606266;
}

.preset-card:hover {
  border-color: #409eff;
  background: linear-gradient(135deg, #ecf5ff 0%, #ffffff 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
}

.preset-icon {
  font-size: 16px;
  color: #409eff;
}

.chat-input-wrapper {
  padding: 16px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
}

.chat-input {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.input-textarea :deep(.el-textarea__inner) {
  border-radius: 8px;
  resize: none;
  font-size: 14px;
  line-height: 1.6;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.input-hint {
  color: #909399;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .ai-chat-page {
    padding: 10px;
    height: calc(100vh - 20px);
  }

  .chat-messages {
    padding: 12px;
  }

  .message {
    gap: 8px;
  }

  .avatar,
  .avatar-placeholder {
    width: 32px;
  }

  .message .bubble {
    max-width: calc(100% - 80px);
  }

  .preset-grid {
    grid-template-columns: 1fr;
  }

  .header-left h3 {
    font-size: 16px;
  }

  .header-actions .el-button {
    font-size: 16px;
  }

  .message-content {
    font-size: 13px;
    padding: 10px 12px;
  }
}
</style>



