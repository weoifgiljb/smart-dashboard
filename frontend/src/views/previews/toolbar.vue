<template>
  <div class="preview-demo">
    <Toolbar
      v-model="searchText"
      search-placeholder="搜索任务..."
      @search="handleSearch"
      @refresh="handleRefresh"
    >
      <template #filters>
        <el-select v-model="status" placeholder="状态" clearable style="width: 120px">
          <el-option label="待办" value="todo" />
          <el-option label="进行中" value="in_progress" />
          <el-option label="已完成" value="done" />
        </el-select>
        <el-select v-model="priority" placeholder="优先级" clearable style="width: 120px">
          <el-option label="低" value="low" />
          <el-option label="中" value="med" />
          <el-option label="高" value="high" />
        </el-select>
      </template>
      <template #actions>
        <el-button type="primary">新建任务</el-button>
      </template>
    </Toolbar>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Toolbar from '@/components/common/Toolbar.vue'

const searchText = ref('')
const status = ref('')
const priority = ref('')

const handleSearch = (val: string) => {
  console.log('Search:', val)
}

const handleRefresh = () => {
  console.log('Refresh')
}
</script>

<style scoped>
.preview-demo {
  padding: 20px;
  background: #f9fafb;
}
</style>
