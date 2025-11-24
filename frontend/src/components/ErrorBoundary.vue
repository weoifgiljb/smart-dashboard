<template>
  <div v-if="hasError" class="error-boundary">
    <div class="result">
      <div class="title">页面加载失败</div>
      <div class="subtitle">请重试或返回首页</div>
      <div class="actions">
        <button class="btn primary" @click="retry">重试</button>
        <button class="btn" @click="goHome">返回首页</button>
      </div>
      <pre v-if="devMessage" class="dev-message">{{ devMessage }}</pre>
    </div>
  </div>
  <slot v-else />
</template>

<script setup lang="ts">
import { ref, onErrorCaptured } from 'vue'
import { useRouter } from 'vue-router'

defineOptions({ name: 'ErrorBoundary' })

const hasError = ref(false)
const devMessage = ref('')
const router = useRouter()

onErrorCaptured((err) => {
  hasError.value = true
  devMessage.value = import.meta.env.DEV ? String(err) : ''
  // 返回 true 阻止继续冒泡
  return true
})

const retry = () => {
  hasError.value = false
  devMessage.value = ''
  // 通过路由刷新当前组件
  router.replace({ path: '/_reload' }).then(() => {
    router.go(-1)
  })
}

const goHome = () => {
  router.push('/')
}
</script>

<style scoped>
.error-boundary {
  padding: 24px;
}
.result {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
  background: #fff;
}
.title {
  font-weight: 700;
  margin-bottom: 6px;
}
.subtitle {
  color: #666;
  margin-bottom: 12px;
}
.actions {
  display: flex;
  gap: 8px;
}
.btn {
  padding: 6px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
}
.btn.primary {
  background: #409eff;
  color: #fff;
  border-color: #409eff;
}
.dev-message {
  margin-top: 12px;
  color: #909399;
  font-size: 12px;
  word-break: break-all;
}
</style>
