export interface User {
  id: string
  username: string
  email: string
  createTime: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  email: string
}

export interface AuthResponse {
  token: string
  user: User
}
