import request from './request'
import type { LoginRequest, RegisterRequest, AuthResponse, User } from '@/types/user'

export const login = (data: LoginRequest): Promise<AuthResponse> => {
  return request.post('/auth/login', data)
}

export const register = (data: RegisterRequest): Promise<AuthResponse> => {
  return request.post('/auth/register', data)
}

export const getUserInfo = (): Promise<User> => {
  return request.get('/auth/me')
}
