export const ACCESS_STORAGE_KEY = 'sd.access_token'
export const REFRESH_STORAGE_KEY = 'sd.refresh_token'

let accessToken: string | null = null
let refreshToken: string | null = null

function readStored(key: string) {
  try {
    return sessionStorage.getItem(key)
  } catch {
    return null
  }
}

function writeStored(key: string, value: string | null) {
  try {
    if (value) sessionStorage.setItem(key, value)
    else sessionStorage.removeItem(key)
  } catch {
    // 隐私模式写不了就只留内存
  }
}

export function hydrateAuthTokens() {
  accessToken = readStored(ACCESS_STORAGE_KEY)
  refreshToken = readStored(REFRESH_STORAGE_KEY)
}

export function getAccessToken(): string | null {
  return accessToken
}

export function getRefreshToken(): string | null {
  return refreshToken
}

export function setAuthTokens(access?: string | null, refresh?: string | null): void {
  if (access !== undefined) {
    accessToken = access || null
    writeStored(ACCESS_STORAGE_KEY, accessToken)
  }
  if (refresh !== undefined) {
    refreshToken = refresh || null
    writeStored(REFRESH_STORAGE_KEY, refreshToken)
  }
}

export function clearAuthTokens(): void {
  accessToken = null
  refreshToken = null
  writeStored(ACCESS_STORAGE_KEY, null)
  writeStored(REFRESH_STORAGE_KEY, null)
}

hydrateAuthTokens()
