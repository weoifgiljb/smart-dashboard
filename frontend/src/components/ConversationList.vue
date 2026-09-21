<script lang="ts">
import { defineComponent, h, type PropType } from 'vue'
import { ElIcon } from 'element-plus'
import { ChatLineSquare, Delete, EditPen } from '@element-plus/icons-vue'
import type { Conversation } from '@/types/chat'

export default defineComponent({
  name: 'ConversationList',
  props: {
    conversations: {
      type: Array as PropType<Conversation[]>,
      required: true,
    },
    activeId: {
      type: String as PropType<string | null>,
      default: null,
    },
  },
  emits: {
    select: (_conversation: Conversation) => true,
    rename: (_conversation: Conversation) => true,
    delete: (_conversation: Conversation) => true,
  },
  setup(props, { emit }) {
    return () =>
      h(
        'div',
        { class: 'conversation-list' },
        props.conversations.map((conversation) =>
          h(
            'div',
            {
              key: conversation.id,
              class: ['history-item', props.activeId === conversation.id ? 'active' : ''],
              onClick: () => emit('select', conversation),
            },
            [
              h(ElIcon, { class: 'history-icon' }, () => h(ChatLineSquare)),
              h('span', { class: 'history-title' }, conversation.title || '新对话'),
              h(
                'span',
                {
                  class: 'history-actions',
                  onClick: (event: MouseEvent) => event.stopPropagation(),
                },
                [
                  h(
                    ElIcon,
                    {
                      class: 'history-action',
                      title: '重命名',
                      onClick: () => emit('rename', conversation),
                    },
                    () => h(EditPen),
                  ),
                  h(
                    ElIcon,
                    {
                      class: 'history-action danger',
                      title: '删除',
                      onClick: () => emit('delete', conversation),
                    },
                    () => h(Delete),
                  ),
                ],
              ),
            ],
          ),
        ),
      )
  },
})
</script>

<style scoped lang="less">
.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
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

.history-icon {
  flex-shrink: 0;
}

.history-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-actions {
  display: flex;
  gap: 6px;
  opacity: 0;
  flex-shrink: 0;
}

.history-item:hover .history-actions,
.history-item.active .history-actions {
  opacity: 1;
}

.history-action {
  cursor: pointer;
  font-size: 14px;
}

.history-action.danger:hover {
  color: var(--color-danger, #f56c6c);
}
</style>
