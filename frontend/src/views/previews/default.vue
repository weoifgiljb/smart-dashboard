<template>
  <div class="default-preview">
    <div class="preview-placeholder">
      <el-icon size="64" color="#d1d5db"><Box /></el-icon>
      <p>组件预览暂未创建</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Box } from '@element-plus/icons-vue'
</script>

<style scoped>
.default-preview {
  padding: 60px 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-placeholder {
  text-align: center;
  color: #9ca3af;
}

.preview-placeholder p {
  margin-top: 16px;
  font-size: 14px;
}
</style>
