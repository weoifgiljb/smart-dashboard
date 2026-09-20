<template>
  <div class="auth-page">
    <div class="auth-card">
      <!-- 左侧品牌区 -->
      <div class="brand-section">
        <div class="brand-content">
          <div class="logo-box">
            <span class="logo-icon">🚀</span>
          </div>
          <h1>加入我们</h1>
          <p class="brand-desc">
            开启你的自我提升之旅。<br />
            所有的伟大，都源于一个开始。
          </p>
          <div class="feature-tags">
            <span>📊 数据可视化</span>
            <span>🧠 科学记忆</span>
            <span>🤖 AI 助手</span>
          </div>
        </div>
      </div>

      <!-- 右侧表单区 -->
      <div class="form-section">
        <div class="form-header">
          <h2>创建账号</h2>
          <p>填写以下信息完成注册</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" class="auth-form" size="large">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="email">
            <el-input v-model="form.email" placeholder="电子邮箱" prefix-icon="Message" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="设置密码"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="确认密码"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="loading" class="submit-btn" @click="handleRegister">
              立即注册 <el-icon class="el-icon--right"><ArrowRight /></el-icon>
            </el-button>
          </el-form-item>
        </el-form>

        <div class="form-footer">
          已有账号？
          <el-link type="primary" @click="$router.push('/login')">直接登录</el-link>
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
  email: '',
  password: '',
  confirmPassword: '',
})

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

const handleRegister = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.registerUser({
          username: form.username,
          email: form.email,
          password: form.password,
        })
        ElMessage.success('注册成功，欢迎加入！')
        router.push('/')
      } catch (error: any) {
        ElMessage.error(error.response?.data?.message || '注册失败，请重试')
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
  min-height: 600px;
  background: white;
  border-radius: 24px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

/* Brand Section */
.brand-section {
  flex: 1;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
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
    display: none;
  }

  .form-section {
    padding: 40px;
  }
}
</style>
