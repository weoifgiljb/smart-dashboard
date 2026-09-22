<template>
  <el-container class="layout-container">
    <el-header class="header">
      <div class="header-left">
        <el-button
          link
          style="margin-right: 16px; color: var(--color-text)"
          @click="toggleCollapse"
        >
          <el-icon :size="24">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
        </el-button>
        <!-- Breadcrumb or Page Title could go here -->
      </div>
      <div class="header-right">
        <el-select v-model="mood" class="mood-select" size="small" @change="onMoodChange">
          <el-option label="池核" :value="MoodName.Pool" />
          <el-option label="海洋核" :value="MoodName.Ocean" />
          <el-option label="水晶核" :value="MoodName.Crystal" />
          <el-option label="雨核" :value="MoodName.Rain" />
        </el-select>
        <el-switch
          v-model="isDark"
          inline-prompt
          active-text="暗"
          inactive-text="亮"
          style="margin-right: 16px"
          @change="toggleTheme"
        />
        <span>欢迎，{{ userStore.user?.username }}</span>
        <UiButton variant="danger" style="margin-left: 20px" @click="handleLogout">退出</UiButton>
      </div>
    </el-header>
    <el-container>
      <el-aside :width="isCollapse ? '64px' : '240px'" class="aside">
        <div class="logo-container">
          <h2 v-show="!isCollapse" class="app-title">自律组件</h2>
          <h2 v-show="isCollapse" class="app-title">自</h2>
        </div>
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :collapse-transition="false"
          router
          class="menu"
        >
          <el-menu-item index="/">
            <el-icon><House /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/calendar">
            <el-icon><Calendar /></el-icon>
            <span>日历 & 打卡</span>
          </el-menu-item>
          <el-menu-item index="/words">
            <el-icon><Reading /></el-icon>
            <span>背单词</span>
          </el-menu-item>
          <el-menu-item index="/pomodoro">
            <el-icon><Timer /></el-icon>
            <span>番茄钟</span>
          </el-menu-item>
          <el-menu-item index="/tasks">
            <el-icon><DataAnalysis /></el-icon>
            <span>任务（增强）</span>
          </el-menu-item>
          <el-menu-item index="/diary">
            <el-icon><Notebook /></el-icon>
            <span>我的日记</span>
          </el-menu-item>
          <el-menu-item index="/ai-chat">
            <el-icon><ChatDotRound /></el-icon>
            <span>AI问答</span>
          </el-menu-item>
          <el-menu-item index="/books">
            <el-icon><Reading /></el-icon>
            <span>书籍推送（扩展）</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <Suspense>
            <template #default>
              <ErrorBoundary>
                <component :is="Component" />
              </ErrorBoundary>
            </template>
            <template #fallback>
              <SkeletonPage />
            </template>
          </Suspense>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import ErrorBoundary from '@/components/ErrorBoundary.vue'
import SkeletonPage from '@/components/SkeletonPage.vue'
import UiButton from '@/components/ui/AppButton.vue'
import { MoodName, useTheme } from '@/composables/useTheme'
import {
  House,
  Calendar,
  Reading,
  Timer,
  DataAnalysis,
  ChatDotRound,
  Notebook,
  Fold,
  Expand,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)
const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const activeMenu = computed(() => route.path)

const handleLogout = async () => {
  await userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}

const { isDark, mood, toggleTheme, setMood } = useTheme()

function onMoodChange(value: string | number | boolean) {
  switch (value) {
    case MoodName.Pool:
    case MoodName.Ocean:
    case MoodName.Crystal:
    case MoodName.Rain:
      setMood(value)
      break
    default:
      setMood(MoodName.Ocean)
  }
}
</script>

<style scoped lang="less">
.layout-container {
  height: 100vh;
  background: var(--color-bg);
  min-width: 0;
}

.layout-container > :deep(.el-container) {
  min-width: 0;
  flex: 1;
}

.header {
  background: var(--color-bg-elevated);
  color: var(--color-text);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 56px;
  border-bottom: 1px solid var(--color-border);
  z-index: 10;
  min-width: 0;
  gap: 12px;
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.header-left {
  flex-shrink: 0;
}

.header-right {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.header-right > span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 12em;
}

.mood-select {
  width: 112px;
  margin-right: 8px;
}

.aside {
  --color-bg: var(--sidebar-local-bg);
  --color-text: var(--sidebar-local-text);
  --color-text-secondary: var(--sidebar-local-text-secondary);
  --color-border: var(--sidebar-local-border);
  background: var(--color-bg);
  color: var(--color-text);
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  transition: width 0.2s ease;
  flex-shrink: 0;
  overflow: hidden;
}

.logo-container {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
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

.menu {
  border-right: none;
  flex: 1;
  padding: 12px 8px;
  background: transparent;
}

:deep(.el-menu) {
  background: transparent;
  border-right: none;
}

:deep(.el-menu-item) {
  border-radius: var(--radius-lg);
  margin-bottom: 4px;
  color: var(--color-text-secondary);
  height: 46px;
  line-height: 46px;
}

:deep(.el-menu-item:hover) {
  background-color: rgba(255, 255, 255, 0.06);
  color: var(--color-primary);
}

:deep(.el-menu-item.is-active) {
  background-color: var(--color-primary-soft);
  color: var(--color-primary);
  font-weight: 600;
}

:deep(.el-menu-item .el-icon) {
  font-size: 18px;
}

.main {
  min-width: 0;
  background: var(--color-bg);
  padding: 24px;
  overflow-x: hidden;
  overflow-y: auto;
}
</style>
