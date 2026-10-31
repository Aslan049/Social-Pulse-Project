import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { getErrorMessage, getFieldErrors } from '../api/errors'
import { useAuth } from '../auth/useAuth'
import { isValidEmail } from '../utils/validation'

type FieldErrors = Partial<Record<'fullName' | 'email' | 'password' | 'confirm', string>>

export default function RegisterPage() {
  const { register } = useAuth()
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  function validate(): FieldErrors {
    const errors: FieldErrors = {}
    if (fullName.trim().length === 0) errors.fullName = 'Ad soyad gerekli'
    else if (fullName.trim().length > 150) errors.fullName = 'Ad soyad en fazla 150 karakter olabilir'

    if (!isValidEmail(email)) errors.email = 'Geçerli bir e-posta adresi girin'

    if (password.length < 8) errors.password = 'Parola en az 8 karakter olmalı'
    else if (password.length > 72) errors.password = 'Parola en fazla 72 karakter olabilir'

    if (confirm !== password) errors.confirm = 'Parolalar eşleşmiyor'
    return errors
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)

    const errors = validate()
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) {
      return
    }

    setSubmitting(true)
    try {
      await register({ fullName: fullName.trim(), email: email.trim(), password })
      // Başarılı kayıtta GuestRoute kullanıcıyı otomatik olarak /dashboard'a yönlendirir
    } catch (err) {
      const serverErrors = getFieldErrors(err)
      if (Object.keys(serverErrors).length > 0) {
        setFieldErrors({
          fullName: serverErrors.fullName,
          email: serverErrors.email,
          password: serverErrors.password,
        })
      }
      setError(getErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="auth-page">
      <form className="card" onSubmit={handleSubmit} noValidate>
        <h1>SocialPulse</h1>
        <p className="subtitle">Yeni hesap oluşturun</p>

        <label htmlFor="fullName">Ad soyad</label>
        <input
          id="fullName"
          autoComplete="name"
          value={fullName}
          onChange={(e) => setFullName(e.target.value)}
        />
        {fieldErrors.fullName && <p className="field-error">{fieldErrors.fullName}</p>}

        <label htmlFor="email">E-posta</label>
        <input
          id="email"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        {fieldErrors.email && <p className="field-error">{fieldErrors.email}</p>}

        <label htmlFor="password">Parola</label>
        <input
          id="password"
          type="password"
          autoComplete="new-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        {fieldErrors.password && <p className="field-error">{fieldErrors.password}</p>}

        <label htmlFor="confirm">Parola (tekrar)</label>
        <input
          id="confirm"
          type="password"
          autoComplete="new-password"
          value={confirm}
          onChange={(e) => setConfirm(e.target.value)}
        />
        {fieldErrors.confirm && <p className="field-error">{fieldErrors.confirm}</p>}

        {error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}

        <button type="submit" disabled={submitting}>
          {submitting ? 'Hesap oluşturuluyor...' : 'Kayıt ol'}
        </button>

        <p className="switch">
          Zaten hesabınız var mı? <Link to="/login">Giriş yapın</Link>
        </p>
      </form>
    </main>
  )
}
