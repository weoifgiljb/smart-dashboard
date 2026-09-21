<template>
  <div class="chat-sidebar">
    <div class="sidebar-header">
      <el-button type="primary" class="new-chat-btn" @click="$emit('new')">
        <el-icon><Plus /></el-icon> 新对话
      </el-button>
    </div>
    <div class="history-list">
      <div class="history-label">最近对话</div>
      <ConversationList
        :conversations="conversations"
        :active-id="activeId"
        @select="$emit('select', $event)"
        @rename="$emit('rename', $event)"
        @delete="$emit('delete', $event)"
      />
    </div>
    <div class="sidebar-footer">
      <el-button link @click="$emit('export')">
        <el-icon><Download /></el-icon> 导出记录
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Download, Plus } from '@element-plus/icons-vue'
import ConversationList from '@/components/ConversationList.vue'
import type { Conversation } from '@/types/chat'

defineProps<{
  conversations: Conversation[]
  activeId: string | null
}>()

defineEmits<{
  new: []
  export: []
  select: [conversation: Conversation]
  rename: [conversation: Conversation]
  delete: [conversation: Conversation]
}>()
</script>

<style scoped lang="less">
.chat-sidebar {
  width: 260px;
  height: 100%;
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

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid var(--border);
}
</style>
