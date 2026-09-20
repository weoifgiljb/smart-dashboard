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

  const setRefreshToken = (refreshToken?: string) => {
    if (refreshToken) {
      localStorage.setItem('refreshToken', refreshToken)
    }
  }

  const logout = () => {
    user.value = null
    token.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    isAuthenticated.value = false
  }

  const loginUser = async (loginData: LoginRequest) => {
    const response = await login(loginData)
    setToken(response.token)
    setRefreshToken(response.refreshToken)
    if (response.user) {
      setUser(response.user)
    } else {
      await fetchUserInfo()
    }
    return response
  }

  const registerUser = async (registerData: RegisterRequest) => {
    const response = await register(registerData)
    setToken(response.token)
    setRefreshToken(response.refreshToken)
    if (response.user) {
      setUser(response.user)
    } else {
      await fetchUserInfo()
    }
    return response
  }

  const fetchUserInfo = async () => {
    if (token.value) {
      try {
        const userData = await getUserInfo()
        setUser(userData)
      } catch {
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
    setRefreshToken,
    logout,
    loginUser,
    registerUser,
    fetchUserInfo,
  }
})
