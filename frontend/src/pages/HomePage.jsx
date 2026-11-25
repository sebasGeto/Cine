import { Navigate } from 'react-router-dom'
import { currentProfileId, currentRole } from '../catalogo/session.js'

// Entrada de la aplicación: lleva a cada quien a su pantalla según la sesión guardada.
export default function HomePage() {
  const role = currentRole()
  if (role === 'ADMIN') return <Navigate to="/admin/catalogo" replace />
  if (role === 'CLIENT') return <Navigate to={currentProfileId() ? '/catalogo' : '/perfiles'} replace />
  return <Navigate to="/login" replace />
}
