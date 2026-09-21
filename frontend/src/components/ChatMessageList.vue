<script lang="ts">
import { defineComponent, h, type PropType } from 'vue'
import { ElIcon } from 'element-plus'
import { CopyDocument, Cpu, Refresh, User } from '@element-plus/icons-vue'
import { ChatMessageType, type ChatMessage } from '@/types/chat'
import { renderChatMarkdown } from '@/utils/chatMarkdown'

function isAi(type: ChatMessageType) {
  switch (type) {
    case ChatMessageType.Ai:
      return true
    case ChatMessageType.User:
      return false
    default: {
      const unreachable: never = type
      return unreachable
    }
  }
}

function formatTime(time: Date | string) {
  const date = typeof time === 'string' ? new Date(time) : time
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

export default defineComponent({
  name: 'ChatMessageList',
  props: {
    messages: {
      type: Array as PropType<ChatMessage[]>,
      required: true,
    },
    loading: {
      type: Boolean,
      default: false,
    },
  },
  emits: {
    copy: (_text: string) => true,
    regenerate: (_message: ChatMessage) => true,
  },
  setup(props, { emit }) {
    return () => {
      const rows = props.messages.map((msg, index) => {
        const ai = isAi(msg.type)
        return h(
          'div',
          { key: `${msg.type}-${index}-${msg.time}`, class: ['message-row', msg.type] },
          [
            h('div', { class: 'avatar' }, [
              h('div', { class: ['avatar-img', msg.type] }, [h(ElIcon, () => h(ai ? Cpu : User))]),
            ]),
            h('div', { class: 'message-bubble' }, [
              h('div', {
                class: 'bubble-content markdown-body',
                innerHTML: renderChatMarkdown(msg.content),
              }),
              h('div', { class: 'bubble-footer' }, [
                h('span', { class: 'time' }, formatTime(msg.time)),
                h('div', { class: 'actions' }, [
                  h(
                    ElIcon,
                    { class: 'action-icon', onClick: () => emit('copy', msg.content) },
                    () => h(CopyDocument),
                  ),
                  ai
                    ? h(
                        'span',
                        {
                          'data-action': 'regenerate',
                          class: 'action-icon',
                          onClick: () => emit('regenerate', msg),
                        },
                        [h(ElIcon, () => h(Refresh))],
                      )
                    : null,
                ]),
              ]),
            ]),
          ],
        )
      })
      if (props.loading) {
        rows.push(
          h('div', { key: 'loading', class: 'message-row ai' }, [
            h('div', { class: 'avatar' }, [
              h('div', { class: 'avatar-img ai' }, [h(ElIcon, () => h(Cpu))]),
            ]),
            h('div', { class: 'message-bubble loading-bubble' }, [
              h('div', { class: 'typing-dots' }, [h('span'), h('span'), h('span')]),
            ]),
          ]),
        )
      }
      return h('div', { class: 'messages-list' }, rows)
    }
  },
})
</script>

<style scoped lang="less">
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

@media (max-width: 768px) {
  .message-bubble {
    max-width: 85%;
  }
}
</style>
