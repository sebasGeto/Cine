import { NavLink } from 'react-router-dom'
import { currentProfileId } from '../catalogo/session.js'
import './reproduccion.css'

// Enlaces entre las pantallas del cliente. Las del perfil (catálogo, Mi lista, historial) solo
// aparecen cuando hay uno elegido; las de la cuenta (perfiles, planes, mi cuenta) siempre.
export default function ViewerNav() {
  const withProfile = Boolean(currentProfileId())
  return (
    <nav className="rep-nav" aria-label="Secciones">
      {withProfile && <NavLink to="/catalogo" end>Catálogo</NavLink>}
      {withProfile && <NavLink to="/mi-lista">Mi lista</NavLink>}
      {withProfile && <NavLink to="/historial">Historial</NavLink>}
      <NavLink to="/perfiles">Perfiles</NavLink>
      <NavLink to="/planes">Planes</NavLink>
      <NavLink to="/cuenta">Mi cuenta</NavLink>
    </nav>
  )
}
