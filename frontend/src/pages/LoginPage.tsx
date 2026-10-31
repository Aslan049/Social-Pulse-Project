import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { getErrorMessage } from '../api/errors'
import { useAuth } from '../auth/useAuth'
import { isValidEmail } from '../utils/validation'

export default function LoginPage() {
  const { login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)

    if (!isValidEmail(email)) {
      setError('Geçerli bir e-posta adresi girin')
      return
    }
    if (password.length === 0) {
      setError('Parola gerekli')
      return
    }

    setSubmitting(true)
    try {
      await login({ email: email.trim(), password })
      // Başarılı girişte GuestRoute kullanıcıyı otomatik olarak /dashboard'a yönlendirir
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="auth-page">
      <form className="card" onSubmit={handleSubmit} noValidate>
        <h1>SocialPulse</h1>
        <p className="subtitle">Hesabınıza giriş yapın</p>

        <label htmlFor="email">E-posta</label>
        <input
          id="email"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />

        <label htmlFor="password">Parola</label>
        <input
          id="password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />

        {error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}

        <button type="submit" disabled={submitting}>
          {submitting ? 'Giriş yapılıyor...' : 'Giriş yap'}
        </button>

        <p className="switch">
          Hesabınız yok mu? <Link to="/register">Kayıt olun</Link>
        </p>
      </form>
    </main>
  )
}
