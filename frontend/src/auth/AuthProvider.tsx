import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import {
  fetchMe,
  loginRequest,
  registerRequest,
  type AuthResponse,
  type LoginPayload,
  type RegisterPayload,
  type User,
} from '../api/auth'
import { TOKEN_KEY, UNAUTHORIZED_EVENT } from '../api/client'
import { AuthContext } from './AuthContext'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  // Kayıtlı bir token varsa, sunucuyla doğrulanana kadar "yükleniyor" durumunda kalınır
  const [loading, setLoading] = useState<boolean>(() => localStorage.getItem(TOKEN_KEY) !== null)

  useEffect(() => {
    if (localStorage.getItem(TOKEN_KEY) === null) {
      return
    }
    let cancelled = false
    fetchMe()
      .then((me) => {
        if (!cancelled) setUser(me)
      })
      .catch(() => {
        localStorage.removeItem(TOKEN_KEY)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  // Axios interceptor 401 görünce tetikler
  useEffect(() => {
    const handleUnauthorized = () => setUser(null)
    window.addEventListener(UNAUTHORIZED_EVENT, handleUnauthorized)
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, handleUnauthorized)
  }, [])

  const startSession = useCallback((response: AuthResponse) => {
    localStorage.setItem(TOKEN_KEY, response.token)
    setUser(response.user)
  }, [])

  const login = useCallback(
    async (payload: LoginPayload) => startSession(await loginRequest(payload)),
    [startSession],
  )

  const register = useCallback(
    async (payload: RegisterPayload) => startSession(await registerRequest(payload)),
    [startSession],
  )

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({ user, loading, login, register, logout }),
    [user, loading, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
