import { NavLink } from 'react-router-dom'
import './reproduccion.css'

// Enlaces entre las pantallas del cliente: catálogo, Mi lista e historial.
export default function ViewerNav() {
  return (
    <nav className="rep-nav" aria-label="Secciones">
      <NavLink to="/catalogo" end>Catálogo</NavLink>
      <NavLink to="/mi-lista">Mi lista</NavLink>
      <NavLink to="/historial">Historial</NavLink>
    </nav>
  )
}
