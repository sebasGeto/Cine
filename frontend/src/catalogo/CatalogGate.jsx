import { useEffect, useState } from 'react'
import api from '../api/client.js'
import { errorMessage } from '../api/catalog.js'
import { currentProfileId, currentRole } from './session.js'
import './catalogo.css'

// PROVISIONAL: muestra el catálogo solo si hay sesión del rol pedido (y perfil, si es cliente).
// Las pantallas de inicio de sesión y de perfiles son de otras áreas; mientras no existan, este
// componente pide esos datos con los endpoints que ya hay. Cuando existan, basta con reemplazar
// SignIn y ProfilePicker por una redirección a ellas.
export default function CatalogGate({ role, children }) {
  const [session, setSession] = useState(() => ({ role: currentRole(), profileId: currentProfileId() }))
  const refresh = () => setSession({ role: currentRole(), profileId: currentProfileId() })

  function signOut() {
    localStorage.removeItem('token')
    localStorage.removeItem('profileId')
    refresh()
  }

  function changeProfile() {
    localStorage.removeItem('profileId')
    refresh()
  }

  if (session.role !== role) return <SignIn role={role} onDone={refresh} />
  if (role === 'CLIENT' && !session.profileId) return <ProfilePicker onDone={refresh} onSignOut={signOut} />
  return (
    <div className="cat">
      <div className="cat-session">
        {role === 'CLIENT' && <button type="button" onClick={changeProfile}>Cambiar perfil</button>}
        <button type="button" onClick={signOut}>Cerrar sesión</button>
      </div>
      {children(session.profileId)}
    </div>
  )
}

function SignIn({ role, onDone }) {
  const admin = role === 'ADMIN'
  const [error, setError] = useState(null)

  async function submit(event) {
    event.preventDefault()
    const form = new FormData(event.target)
    const user = form.get('user')
    const password = form.get('password')
    setError(null)
    try {
      const { data } = admin
        ? await api.post('/auth/admin/login', { username: user, password })
        : await api.post('/auth/login', { email: user, password })
      localStorage.setItem('token', data.token)
      localStorage.removeItem('profileId')
      onDone()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <main className="cat cat-narrow">
      <h1>{admin ? 'Administración del catálogo' : 'Catálogo'}</h1>
      <form className="cat-form" onSubmit={submit}>
        <label>{admin ? 'Usuario o correo' : 'Correo'}
          <input name="user" type={admin ? 'text' : 'email'} autoComplete="username" required />
        </label>
        <label>Contraseña
          <input name="password" type="password" autoComplete="current-password" required />
        </label>
        {error && <p className="cat-error" role="alert">{error}</p>}
        <button type="submit" className="cat-primary">Iniciar sesión</button>
      </form>
    </main>
  )
}

function ProfilePicker({ onDone, onSignOut }) {
  // null = cargando; [] = no hay lista de perfiles y se escribe el número
  const [profiles, setProfiles] = useState(null)

  useEffect(() => {
    let active = true
    api.get('/profiles')
      .then((r) => active && setProfiles(r.data.filter((p) => p.isActive)))
      .catch(() => active && setProfiles([]))
    return () => { active = false }
  }, [])

  function choose(id) {
    localStorage.setItem('profileId', id)
    onDone()
  }

  return (
    <main className="cat cat-narrow">
      <h1>¿Quién está viendo?</h1>
      {profiles?.map((p) => (
        <button key={p.profileId} type="button" onClick={() => choose(p.profileId)}>{p.name}</button>
      ))}
      {profiles?.length === 0 && (
        <form className="cat-form" onSubmit={(e) => { e.preventDefault(); choose(new FormData(e.target).get('id')) }}>
          <label>Número de perfil
            <input name="id" type="number" min="1" required />
          </label>
          <button type="submit" className="cat-primary">Continuar</button>
        </form>
      )}
      <p><button type="button" onClick={onSignOut}>Cerrar sesión</button></p>
    </main>
  )
}
