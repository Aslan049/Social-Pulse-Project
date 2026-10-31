import { useAuth } from '../auth/useAuth'

export default function DashboardPage() {
  const { user, logout } = useAuth()

  return (
    <main className="dashboard">
      <header>
        <h1>SocialPulse</h1>
        <button type="button" className="secondary" onClick={logout}>
          Çıkış yap
        </button>
      </header>
      <section className="card">
        <h2>Merhaba, {user?.fullName}</h2>
        <p>{user?.email}</p>
        <p className="subtitle">CSV yükleme ve analiz ekranları sonraki günlerde eklenecek.</p>
      </section>
    </main>
  )
}
