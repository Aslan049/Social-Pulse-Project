import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './useAuth'

/** Giriş yapmamış kullanıcıyı /login sayfasına yönlendirir. */
export function ProtectedRoute() {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return <p className="status">Yükleniyor...</p>
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  return <Outlet />
}

/** Zaten giriş yapmış kullanıcıyı giriş/kayıt sayfalarından uzak tutar. */
export function GuestRoute() {
  const { user, loading } = useAuth()

  if (loading) {
    return <p className="status">Yükleniyor...</p>
  }
  if (user) {
    return <Navigate to="/dashboard" replace />
  }
  return <Outlet />
}
