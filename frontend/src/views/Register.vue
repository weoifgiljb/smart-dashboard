<template>
  <AuthLayout title="创建账号" lead="填写以下信息完成注册">
    <el-form ref="formRef" :model="form" :rules="rules" class="auth-form" size="large">
      <el-form-item prop="username">
        <el-input v-model="form.username" placeholder="用户名">
          <template #prefix>
            <el-icon><User /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item prop="email">
        <el-input v-model="form.email" placeholder="电子邮箱">
          <template #prefix>
            <el-icon><Message /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="form.password" type="password" placeholder="设置密码" show-password>
          <template #prefix>
            <el-icon><Lock /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          placeholder="确认密码"
          show-password
        >
          <template #prefix>
            <el-icon><Lock /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" class="submit-btn" @click="handleRegister">
          立即注册
        </el-button>
      </el-form-item>
    </el-form>
    <p class="form-footer">
      已有账号？
      <el-link type="primary" @click="$router.push('/login')">直接登录</el-link>
    </p>
  </AuthLayout>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { isAxiosError } from 'axios'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Message, User } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/store/user'
import AuthLayout from '@/layouts/AuthLayout.vue'

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

function validateConfirmPassword(
  _rule: unknown,
  value: unknown,
  callback: (error?: Error) => void,
) {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码至少 8 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

const handleRegister = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.registerUser({
        username: form.username,
        email: form.email,
        password: form.password,
      })
      ElMessage.success('注册成功，欢迎加入！')
      router.push('/')
    } catch (error) {
      if (!isAxiosError(error)) ElMessage.error('注册失败，请重试')
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
