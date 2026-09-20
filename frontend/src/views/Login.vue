<template>
  <AuthLayout title="欢迎回来" lead="请登录账号以继续">
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      class="auth-form"
      size="large"
      @keyup.enter="handleLogin"
    >
      <el-form-item prop="username">
        <el-input v-model="form.username" placeholder="用户名/邮箱">
          <template #prefix>
            <el-icon><User /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="form.password" type="password" placeholder="密码" show-password>
          <template #prefix>
            <el-icon><Lock /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" class="submit-btn" @click="handleLogin">
          立即登录
        </el-button>
      </el-form-item>
    </el-form>
    <p class="form-footer">
      还没有账号？
      <el-link type="primary" @click="$router.push('/register')">立即注册</el-link>
    </p>
  </AuthLayout>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { isAxiosError } from 'axios'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/store/user'
import AuthLayout from '@/layouts/AuthLayout.vue'
import { safeRedirect } from '@/utils/safeRedirect'

const router = useRouter()
const route = useRoute()
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
    if (!valid) return
    loading.value = true
    try {
      await userStore.loginUser(form)
      ElMessage.success('欢迎回来！')
      router.push(safeRedirect(route.query.redirect))
    } catch (error) {
      if (!isAxiosError(error)) ElMessage.error('登录失败，请检查用户名或密码')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped lang="less">
.submit-btn {
  width: 100%;
  height: 44px;
  font-weight: 600;
}

.form-footer {
  margin: 8px 0 0;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-secondary);
}
</style>
