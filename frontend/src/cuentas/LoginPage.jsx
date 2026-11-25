import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { adminLogin, login } from '../api/account.js'
import { errorMessage } from '../api/catalog.js'
import { saveSession } from '../catalogo/session.js'
import '../catalogo/catalogo.css'

// RF-02 (clientes) y RF-33 (administradores). Mismo formulario, distinto endpoint y destino.
// El backend no dice si falló el usuario o la contraseña: el mensaje es el mismo en ambos casos.
export default function LoginPage({ admin = false }) {
  const navigate = useNavigate()
  const location = useLocation()
  const [error, setError] = useState(null)
  const [sending, setSending] = useState(false)

  async function submit(event) {
    event.preventDefault()
    const form = new FormData(event.target)
    setError(null)
    setSending(true)
    try {
      const data = admin
        ? await adminLogin(form.get('user').trim(), form.get('password'))
        : await login(form.get('user').trim(), form.get('password'))
      saveSession(data.token)
      // Vuelve a la página que pidió el inicio de sesión; si no, a la entrada del rol
      navigate(location.state?.from ?? (admin ? '/admin/catalogo' : '/perfiles'), { replace: true })
    } catch (e) {
      setError(errorMessage(e))
      setSending(false)
    }
  }

  return (
    <main className="cat cat-narrow">
      <h1>CineConecta</h1>
      <h2>{admin ? 'Administración' : 'Iniciar sesión'}</h2>
      <form className="cat-form" onSubmit={submit}>
        <label>{admin ? 'Usuario o correo' : 'Correo'}
          <input name="user" type={admin ? 'text' : 'email'} autoComplete="username" required autoFocus />
        </label>
        <label>Contraseña
          <input name="password" type="password" autoComplete="current-password" required />
        </label>
        {error && <p className="cat-error" role="alert">{error}</p>}
        <button type="submit" className="cat-primary" disabled={sending}>
          {sending ? 'Ingresando…' : 'Iniciar sesión'}
        </button>
      </form>
      {admin
        ? <p className="cat-muted"><Link to="/login">Soy cliente</Link></p>
        : (
          <>
            <p>¿No tienes cuenta? <Link to="/registro">Regístrate</Link></p>
            <p className="cat-muted"><Link to="/admin/login">Ingreso de administradores</Link></p>
          </>
        )}
    </main>
  )
}
