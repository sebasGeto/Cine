// Sesión vista desde el catálogo: el token que guarda el inicio de sesión (localStorage 'token',
// el mismo que lee api/client.js) y el perfil seleccionado (localStorage 'profileId').

// El rol viene dentro del token. Solo decide qué mostrar: los permisos los verifica el servidor.
export function currentRole() {
  try {
    const payload = localStorage.getItem('token').split('.')[1]
    const { role, exp } = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
    return exp * 1000 > Date.now() ? role : null
  } catch {
    return null
  }
}

export const currentProfileId = () => localStorage.getItem('profileId')
