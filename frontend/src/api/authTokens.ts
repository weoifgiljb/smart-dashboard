let accessToken: string | null = null
let refreshToken: string | null = null

export function getAccessToken(): string | null {
  return accessToken
}

export function getRefreshToken(): string | null {
  return refreshToken
}

export function setAuthTokens(access?: string | null, refresh?: string | null): void {
  if (access !== undefined) {
    accessToken = access || null
  }
  if (refresh !== undefined) {
    refreshToken = refresh || null
  }
}

export function clearAuthTokens(): void {
  accessToken = null
  refreshToken = null
}
