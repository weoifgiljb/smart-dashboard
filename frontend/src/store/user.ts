import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login, register, getUserInfo, logout as logoutRequest } from '@/api/auth'
import { clearAuthTokens, getAccessToken, setAuthTokens } from '@/api/authTokens'
import type { LoginRequest, RegisterRequest, User } from '@/types/user'

export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(getAccessToken())
  const isAuthenticated = ref(false)
  let sessionPromise: Promise<void> | null = null

  const setUser = (userData: User) => {
    user.value = userData
  }

  const applyTokens = (access?: string | null, refresh?: string | null) => {
    setAuthTokens(access, refresh)
    token.value = getAccessToken()
  }

  const setToken = (newToken: string) => {
    applyTokens(newToken, undefined)
    isAuthenticated.value = true
  }

  const setRefreshToken = (refreshToken?: string) => {
    if (refreshToken) {
      applyTokens(undefined, refreshToken)
    }
  }

  const clearSession = () => {
    user.value = null
    token.value = null
    isAuthenticated.value = false
    clearAuthTokens()
    sessionPromise = null
  }

  const logout = async () => {
    try {
      await logoutRequest()
    } catch {
      // Cookie 清理由服务端尽力完成；本地会话仍要丢掉
    }
    clearSession()
  }

  const loginUser = async (loginData: LoginRequest) => {
    const response = await login(loginData)
    applyTokens(response.token, response.refreshToken)
    isAuthenticated.value = true
    sessionPromise = Promise.resolve()
    if (response.user) {
      setUser(response.user)
    } else {
      await fetchUserInfo()
    }
    return response
  }

  const registerUser = async (registerData: RegisterRequest) => {
    const response = await register(registerData)
    applyTokens(response.token, response.refreshToken)
    isAuthenticated.value = true
    sessionPromise = Promise.resolve()
    if (response.user) {
      setUser(response.user)
    } else {
      await fetchUserInfo()
    }
    return response
  }

  const fetchUserInfo = async () => {
    try {
      const userData = await getUserInfo()
      setUser(userData)
      isAuthenticated.value = true
    } catch {
      user.value = null
      token.value = null
      isAuthenticated.value = false
      clearAuthTokens()
    }
  }

  const ensureSession = () => {
    if (isAuthenticated.value && user.value) {
      return Promise.resolve()
    }
    if (!sessionPromise) {
      sessionPromise = fetchUserInfo()
    }
    return sessionPromise
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
    ensureSession,
  }
})
