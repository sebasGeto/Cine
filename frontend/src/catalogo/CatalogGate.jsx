import { Navigate, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { logout } from '../api/account.js'
import { clearSession, currentProfileId, currentProfileName, currentRole, forgetProfile } from './session.js'
import './catalogo.css'

// Protege las páginas: solo se muestran con sesión del rol pedido y, si es cliente, con un perfil
// elegido. Si falta algo redirige a /login, /admin/login o /perfiles; quien inicia sesión vuelve
// a la página que intentaba abrir (state.from).
// needsProfile=false: páginas de la cuenta que no dependen del perfil (perfiles, planes, mi cuenta).
export default function CatalogGate({ role, needsProfile = true, children }) {
  const location = useLocation()
  const navigate = useNavigate()
  const admin = role === 'ADMIN'
  const profileId = currentProfileId()

  if (currentRole() !== role) {
    const from = location.pathname + location.search
    return <Navigate to={admin ? '/admin/login' : '/login'} replace state={{ from }} />
  }
  if (!admin && needsProfile && !profileId) return <Navigate to="/perfiles" replace />

  async function signOut() {
    await logout().catch(() => {})
    clearSession()
    navigate(admin ? '/admin/login' : '/login', { replace: true })
  }

  function changeProfile() {
    forgetProfile()
    navigate('/perfiles')
  }

  return (
    <div className="cat">
      <div className="cat-session">
        {admin && (
          <nav className="cat-admin-nav" aria-label="Administración">
            <NavLink to="/admin/catalogo">Catálogo</NavLink>
            <NavLink to="/admin/informes">Informes</NavLink>
          </nav>
        )}
        {!admin && profileId && <span className="cat-muted">Perfil: <strong>{currentProfileName()}</strong></span>}
        {!admin && profileId && <button type="button" onClick={changeProfile}>Cambiar perfil</button>}
        <button type="button" onClick={signOut}>Cerrar sesión</button>
      </div>
      {children(profileId)}
    </div>
  )
}
