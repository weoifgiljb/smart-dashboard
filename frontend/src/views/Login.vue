<template>
  <div class="auth-page">
    <div class="auth-card">
      <!-- 左侧品牌区 -->
      <div class="brand-section">
        <div class="brand-content">
          <div class="logo-box">
            <span class="logo-icon">✨</span>
          </div>
          <h1>自律让自由更近</h1>
          <p class="brand-desc">
            规划你的每一天，记录每一次进步。<br />
            从今天开始，遇见更好的自己。
          </p>
          <div class="feature-tags">
            <span>🍅 番茄专注</span>
            <span>📚 单词记忆</span>
            <span>📝 任务管理</span>
          </div>
        </div>
      </div>

      <!-- 右侧表单区 -->
      <div class="form-section">
        <div class="form-header">
          <h2>欢迎回来</h2>
          <p>请登录您的账号以继续</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="auth-form"
          size="large"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名/邮箱" prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <div class="form-options">
            <el-checkbox>记住我</el-checkbox>
            <el-link type="primary" :underline="false">忘记密码？</el-link>
          </div>

          <el-form-item>
            <el-button type="primary" :loading="loading" class="submit-btn" @click="handleLogin">
              立即登录 <el-icon class="el-icon--right"><ArrowRight /></el-icon>
            </el-button>
          </el-form-item>
        </el-form>

        <div class="form-footer">
          还没有账号？
          <el-link type="primary" @click="$router.push('/register')">立即注册</el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const handleLogin = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.loginUser(form)
        ElMessage.success('欢迎回来！')
        router.push('/')
      } catch (error: any) {
        ElMessage.error(error.response?.data?.message || '登录失败，请检查用户名或密码')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f0f2f5;
  padding: 20px;
  background-image:
    radial-gradient(at 0% 0%, hsla(253, 16%, 7%, 1) 0, transparent 50%),
    radial-gradient(at 50% 0%, hsla(225, 39%, 30%, 1) 0, transparent 50%),
    radial-gradient(at 100% 0%, hsla(339, 49%, 30%, 1) 0, transparent 50%);
  background-size: cover;
}

.auth-card {
  display: flex;
  width: 900px;
  min-height: 560px;
  background: white;
  border-radius: 24px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

/* Brand Section */
.brand-section {
  flex: 1;
  background: linear-gradient(135deg, #10b981 0%, #3b82f6 100%);
  padding: 60px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: white;
  position: relative;
  overflow: hidden;
}

.brand-section::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: url("data:image/svg+xml,%3Csvg width='100' height='100' viewBox='0 0 100 100' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M11 18c3.866 0 7-3.134 7-7s-3.134-7-7-7-7 3.134-7 7 3.134 7 7 7zm48 25c3.866 0 7-3.134 7-7s-3.134-7-7-7-7 3.134-7 7 3.134 7 7 7zm-43-7c1.657 0 3-1.343 3-3s-1.343-3-3-3-3 1.343-3 3 1.343 3 3 3zm63 31c1.657 0 3-1.343 3-3s-1.343-3-3-3-3 1.343-3 3 1.343 3 3 3zM34 90c1.657 0 3-1.343 3-3s-1.343-3-3-3-3 1.343-3 3 1.343 3 3 3zm56-76c1.657 0 3-1.343 3-3s-1.343-3-3-3-3 1.343-3 3 1.343 3 3 3zM12 86c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm28-65c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm23-11c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zm-6 60c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm29 22c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zM32 63c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zm57-13c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zm-9-21c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2zM60 91c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2zM35 41c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2zM12 60c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2z' fill='%23ffffff' fill-opacity='0.1' fill-rule='evenodd'/%3E%3C/svg%3E");
  opacity: 0.6;
}

.brand-content {
  position: relative;
  z-index: 1;
}

.logo-box {
  width: 64px;
  height: 64px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 32px;
  backdrop-filter: blur(10px);
}

.logo-icon {
  font-size: 32px;
}

.brand-section h1 {
  font-size: 36px;
  margin: 0 0 16px;
  font-weight: 800;
  line-height: 1.2;
}

.brand-desc {
  font-size: 16px;
  line-height: 1.6;
  opacity: 0.9;
  margin: 0 0 40px;
}

.feature-tags {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.feature-tags span {
  padding: 6px 16px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 20px;
  font-size: 14px;
  backdrop-filter: blur(4px);
}

/* Form Section */
.form-section {
  flex: 1.2;
  padding: 60px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-header {
  margin-bottom: 32px;
}

.form-header h2 {
  font-size: 28px;
  color: #1f2937;
  margin: 0 0 8px;
}

.form-header p {
  color: #6b7280;
  margin: 0;
}

.auth-form :deep(.el-input__wrapper) {
  padding: 12px 16px;
  border-radius: 12px;
  box-shadow: 0 0 0 1px #e5e7eb inset;
}

.auth-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px var(--primary) inset;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.submit-btn {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
}

.form-footer {
  text-align: center;
  font-size: 14px;
  color: #6b7280;
  margin-top: 24px;
}

@media (max-width: 900px) {
  .auth-card {
    flex-direction: column;
    width: 100%;
    max-width: 480px;
  }

  .brand-section {
    padding: 40px;
    display: none; /* Mobile hide for simplicity, or show smaller */
  }

  .form-section {
    padding: 40px;
  }
}
</style>
