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

<style lang="less">
@import '../styles/auth.less';
</style>
