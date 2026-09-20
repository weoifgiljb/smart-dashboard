import axios from 'axios'
import { getRefreshToken, setAuthTokens } from '@/api/authTokens'

const apiBase = (import.meta as { env?: { VITE_API_BASE?: string } }).env?.VITE_API_BASE || '/api'

export interface RefreshResult {
  token: string
  refreshToken?: string
}

export async function refreshAuthToken(): Promise<RefreshResult | null> {
  const refreshToken = getRefreshToken()
  try {
    const res = await axios.post(
      `${apiBase}/auth/refresh`,
      refreshToken ? { refreshToken } : {},
      {
        withCredentials: true,
        headers: refreshToken ? { 'Refresh-Token': refreshToken } : {},
      },
    )
    const data = res.data || {}
    const newToken = (data.token || data.accessToken) as string | undefined
    if (!newToken) return null
    setAuthTokens(newToken, data.refreshToken)
    return { token: newToken, refreshToken: data.refreshToken }
  } catch {
    return null
  }
}
