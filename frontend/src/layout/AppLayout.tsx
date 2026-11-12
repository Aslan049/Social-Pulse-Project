import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

export default function AppLayout() {
  const { user, logout } = useAuth()

  return (
    <>
      <header className="app-header">
        <strong className="brand">SocialPulse</strong>
        <nav className="app-nav" aria-label="Ana menü">
          <NavLink to="/dashboard" className={({ isActive }) => (isActive ? 'active' : undefined)}>
            Panel
          </NavLink>
          <NavLink to="/compare" className={({ isActive }) => (isActive ? 'active' : undefined)}>
            Karşılaştırma
          </NavLink>
          <NavLink to="/upload" className={({ isActive }) => (isActive ? 'active' : undefined)}>
            Veri yükle
          </NavLink>
        </nav>
        <div className="app-user">
          <span>{user?.fullName}</span>
          <button type="button" className="secondary" onClick={logout}>
            Çıkış yap
          </button>
        </div>
      </header>
      <main>
        <Outlet />
      </main>
    </>
  )
}
