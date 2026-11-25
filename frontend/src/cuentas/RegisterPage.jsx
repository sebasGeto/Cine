import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { login, register } from '../api/account.js'
import { errorMessage } from '../api/catalog.js'
import { saveSession } from '../catalogo/session.js'
import '../catalogo/catalogo.css'

// RF-01. El correo repetido lo rechaza el índice único de PostgreSQL (409 EMAIL_ALREADY_EXISTS);
// aquí solo se valida lo que el usuario puede corregir antes de enviar.
// Una cuenta nueva no tiene suscripción, así que después de registrarse va a los planes.
export default function RegisterPage() {
  const navigate = useNavigate()
  const [error, setError] = useState(null)
  const [sending, setSending] = useState(false)

  async function submit(event) {
    event.preventDefault()
    const form = new FormData(event.target)
    const email = form.get('email').trim()
    const password = form.get('password')
    if (password !== form.get('confirm')) {
      setError('Las contraseñas no coinciden')
      return
    }
    setError(null)
    setSending(true)
    try {
      await register({ fullName: form.get('fullName').trim(), email, password })
      const data = await login(email, password)
      saveSession(data.token)
      navigate('/planes', { replace: true, state: { welcome: true } })
    } catch (e) {
      setError(errorMessage(e))
      setSending(false)
    }
  }

  return (
    <main className="cat cat-narrow">
      <h1>CineConecta</h1>
      <h2>Crear cuenta</h2>
      <form className="cat-form" onSubmit={submit}>
        <label>Nombre completo
          <input name="fullName" autoComplete="name" maxLength={150} required autoFocus />
        </label>
        <label>Correo
          <input name="email" type="email" autoComplete="email" maxLength={254} required />
        </label>
        <label>Contraseña
          <input name="password" type="password" autoComplete="new-password" minLength={8} required />
        </label>
        <label>Repetir contraseña
          <input name="confirm" type="password" autoComplete="new-password" minLength={8} required />
        </label>
        {error && <p className="cat-error" role="alert">{error}</p>}
        <button type="submit" className="cat-primary" disabled={sending}>
          {sending ? 'Creando…' : 'Crear cuenta'}
        </button>
      </form>
      <p>¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link></p>
    </main>
  )
}
