import axios from 'axios'

const apiBase = (import.meta as { env?: { VITE_API_BASE?: string } }).env?.VITE_API_BASE || '/api'

export interface RefreshResult {
  token: string
  refreshToken?: string
}

export async function refreshAuthToken(): Promise<RefreshResult | null> {
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) return null
  try {
    const res = await axios.post(
      `${apiBase}/auth/refresh`,
      { refreshToken },
      { headers: { 'Refresh-Token': refreshToken } },
    )
    const data = res.data || {}
    const newToken = (data.token || data.accessToken) as string | undefined
    if (!newToken) return null
    localStorage.setItem('token', newToken)
    if (data.refreshToken) {
      localStorage.setItem('refreshToken', data.refreshToken)
    }
    return { token: newToken, refreshToken: data.refreshToken }
  } catch {
    return null
  }
}
