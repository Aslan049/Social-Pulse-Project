import { api } from './client'

export interface User {
  id: number
  email: string
  fullName: string
}

export interface AuthResponse {
  token: string
  tokenType: string
  expiresInSeconds: number
  user: User
}

export interface RegisterPayload {
  email: string
  password: string
  fullName: string
}

export interface LoginPayload {
  email: string
  password: string
}

export const registerRequest = (payload: RegisterPayload) =>
  api.post<AuthResponse>('/api/auth/register', payload).then((r) => r.data)

export const loginRequest = (payload: LoginPayload) =>
  api.post<AuthResponse>('/api/auth/login', payload).then((r) => r.data)

export const fetchMe = () => api.get<User>('/api/users/me').then((r) => r.data)
