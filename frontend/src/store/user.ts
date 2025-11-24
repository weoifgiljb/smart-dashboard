import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login, register, getUserInfo } from '@/api/auth'
import type { LoginRequest, RegisterRequest, User } from '@/types/user'

export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(localStorage.getItem('token'))

  const isAuthenticated = ref(!!token.value)

  const setUser = (userData: User) => {
    user.value = userData
  }

  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
    isAuthenticated.value = true
  }

  const logout = () => {
    user.value = null
    token.value = null
    localStorage.removeItem('token')
    isAuthenticated.value = false
  }

  const loginUser = async (loginData: LoginRequest) => {
    const response = await login(loginData)
    setToken(response.token)
    await fetchUserInfo()
    return response
  }

  const registerUser = async (registerData: RegisterRequest) => {
    const response = await register(registerData)
    setToken(response.token)
    await fetchUserInfo()
    return response
  }

  const fetchUserInfo = async () => {
    if (token.value) {
      try {
        const userData = await getUserInfo()
        setUser(userData)
      } catch (error) {
        logout()
      }
    }
  }

  if (token.value) {
    fetchUserInfo()
  }

  return {
    user,
    token,
    isAuthenticated,
    setUser,
    setToken,
    logout,
    loginUser,
    registerUser,
    fetchUserInfo,
  }
})
