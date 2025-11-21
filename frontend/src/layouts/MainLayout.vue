<template>
  <el-container class="layout-container">
    <el-header class="header">
      <div class="header-left">
        <el-button link @click="toggleCollapse" style="margin-right: 16px; color: var(--app-text)">
          <el-icon :size="24">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
        </el-button>
        <!-- Breadcrumb or Page Title could go here -->
      </div>
      <div class="header-right">
        <el-switch
          v-model="isDark"
          inline-prompt
          active-text="暗"
          inactive-text="亮"
          @change="toggleTheme"
          style="margin-right: 16px"
        />
        <span>欢迎，{{ userStore.user?.username }}</span>
        <UiButton type="danger" @click="handleLogout" style="margin-left: 20px">退出</UiButton>
      </div>
    </el-header>
    <el-container>
      <el-aside :width="isCollapse ? '64px' : '240px'" class="aside">
        <div class="logo-container">
          <h2 class="app-title" v-show="!isCollapse">自律组件</h2>
          <h2 class="app-title" v-show="isCollapse">自</h2>
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
            <span>书籍推送</span>
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
import UiButton from '@/components/ui/Button.vue'
import {
  House,
  Calendar,
  Reading,
  Timer,
  DataAnalysis,
  ChatDotRound,
  Notebook,
  Fold,
  Expand
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)
const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const activeMenu = computed(() => route.path)

const handleLogout = () => {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}

// 主题切换
const isDark = ref(localStorage.getItem('theme') === 'dark')
const toggleTheme = () => {
  const theme = isDark.value ? 'dark' : 'light'
  document.documentElement.setAttribute('data-theme', theme)
  localStorage.setItem('theme', theme)
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  background-color: var(--app-bg);
}

.header {
  background: var(--header-bg);
  color: var(--app-text);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 64px;
  box-shadow: var(--shadow-sm);
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
}

.aside {
  background: var(--sidebar-bg);
  border-right: 1px solid var(--el-border-color-light);
  display: flex;
  flex-direction: column;
  transition: width 0.3s;
}

.logo-container {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid var(--el-border-color-light);
}

.app-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 1px;
}

.menu {
  border-right: none;
  flex: 1;
  padding: 16px 8px;
  background: transparent;
}

:deep(.el-menu-item) {
  border-radius: var(--radius-md);
  margin-bottom: 4px;
  color: var(--text-secondary);
  height: 50px;
  line-height: 50px;
}

:deep(.el-menu-item:hover) {
  background-color: var(--app-bg);
  color: var(--primary);
}

:deep(.el-menu-item.is-active) {
  background-color: var(--primary-light);
  color: var(--primary);
  font-weight: 600;
}

:deep(.el-menu-item .el-icon) {
  font-size: 18px;
}

.main {
  background: var(--app-bg);
  padding: 24px;
  overflow-y: auto;
}
</style>



