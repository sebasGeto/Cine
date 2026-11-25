// Sesión del navegador: el token que guarda el inicio de sesión (localStorage 'token',
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

export const currentProfileName = () => localStorage.getItem('profileName')

/** Después de iniciar sesión: guarda el token y obliga a elegir perfil de nuevo. */
export function saveSession(token) {
  localStorage.setItem('token', token)
  forgetProfile()
}

/** RF-10: el perfil elegido viaja en X-Profile-Id en las peticiones del cliente. */
export function rememberProfile(profile) {
  localStorage.setItem('profileId', profile.profileId)
  localStorage.setItem('profileName', profile.name)
}

export function forgetProfile() {
  localStorage.removeItem('profileId')
  localStorage.removeItem('profileName')
}

export function clearSession() {
  localStorage.removeItem('token')
  forgetProfile()
}
