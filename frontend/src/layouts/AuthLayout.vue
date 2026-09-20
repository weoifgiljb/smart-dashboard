<template>
  <div class="auth-shell">
    <header class="auth-header">
      <h1 class="app-title">自律组件</h1>
      <el-switch
        v-model="isDark"
        inline-prompt
        active-text="暗"
        inactive-text="亮"
        @change="toggleTheme"
      />
    </header>
    <main class="auth-main">
      <el-card class="auth-card" shadow="never">
        <h2>{{ title }}</h2>
        <p class="lead">{{ lead }}</p>
        <slot />
      </el-card>
    </main>
  </div>
</template>

<script setup lang="ts">
import { useTheme } from '@/composables/useTheme'

defineProps<{
  title: string
  lead: string
}>()

const { isDark, toggleTheme } = useTheme()
</script>

<style scoped lang="less">
.auth-shell {
  min-height: 100vh;
  background: var(--color-bg);
  color: var(--color-text);
}

.auth-header {
  height: 56px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--color-bg-elevated);
  border-bottom: 1px solid var(--color-border);
}

.app-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 0.5px;
}

.auth-main {
  display: flex;
  justify-content: center;
  padding: 48px 24px;
}

.auth-card {
  width: min(420px, 100%);
  background: var(--color-bg-elevated);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.auth-card h2 {
  margin: 0 0 8px;
  font-size: 22px;
  color: var(--color-text);
}

.lead {
  margin: 0 0 24px;
  font-size: 14px;
  color: var(--color-text-secondary);
}
</style>
