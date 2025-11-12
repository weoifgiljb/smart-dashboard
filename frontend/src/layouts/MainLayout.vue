<template>
  <el-container class="layout-container">
    <el-header class="header">
      <div class="header-left">
        <h2>自律组件</h2>
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
      <el-aside width="200px" class="aside">
        <el-menu
          :default-active="activeMenu"
          router
          class="menu"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
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
  ChatDotRound
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

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
}

.header {
  background: #409eff;
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.header-left h2 {
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
}

.aside {
  background: #304156;
}

.menu {
  border-right: none;
  height: 100%;
}

.main {
  background: var(--app-bg);
  padding: 20px;
}
</style>



