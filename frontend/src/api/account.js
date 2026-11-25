import api from './client.js'

// Peticiones de cuentas, perfiles, suscripciones e informes. El token lo agrega client.js.
// Ninguna lleva X-Profile-Id: dependen solo de la cuenta del token.

// Cuentas (RF-01 a RF-04) y administradores (RF-33, RF-34)
export const register = (account) => api.post('/auth/register', account).then((r) => r.data)

export const login = (email, password) =>
  api.post('/auth/login', { email, password }).then((r) => r.data)

export const adminLogin = (username, password) =>
  api.post('/auth/admin/login', { username, password }).then((r) => r.data)

// El token es JWT sin estado: el cierre real es borrarlo del navegador (session.js)
export const logout = () => api.post('/auth/logout')

export const getAccountHistory = () => api.get('/account/history').then((r) => r.data)

// Perfiles (RF-05 a RF-10)
export const getProfiles = () => api.get('/profiles').then((r) => r.data)

export const createProfile = (profile) => api.post('/profiles', profile).then((r) => r.data)

export const updateProfile = (id, profile) => api.put(`/profiles/${id}`, profile).then((r) => r.data)

export const activateProfile = (id) => api.patch(`/profiles/${id}/activate`).then((r) => r.data)

export const deactivateProfile = (id) => api.patch(`/profiles/${id}/deactivate`).then((r) => r.data)

export const selectProfile = (id) => api.post(`/profiles/${id}/select`).then((r) => r.data)

// Planes y suscripciones (RF-11 a RF-13)
export const getPlans = () => api.get('/plans').then((r) => r.data)

// null si la cuenta no tiene suscripción vigente (el backend responde 404)
export const getCurrentSubscription = () =>
  api.get('/subscriptions/current')
    .then((r) => r.data)
    .catch((e) => {
      if (e.response?.status === 404) return null
      throw e
    })

export const purchase = (planId, paymentApproved, simulateFailure) =>
  api.post('/subscriptions', { planId, paymentApproved, simulateFailure }).then((r) => r.data)

// Informes del administrador (RF-35 a RF-37). kind: revenue | consumption
export const getReport = (kind, from, to) =>
  api.get(`/admin/reports/${kind}`, { params: { from, to } }).then((r) => r.data)

/** RF-37: el CSV necesita el token, así que se pide con axios y se descarga desde memoria. */
export async function downloadReport(kind, from, to) {
  const response = await api.get(`/admin/reports/${kind}`, {
    params: { from, to, format: 'csv' },
    responseType: 'blob',
  })
  const name = response.headers['content-disposition']?.match(/filename="?([^";]+)"?/)?.[1]
    ?? `${kind}_${from}_${to}.csv`
  const url = URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = url
  link.download = name
  link.click()
  URL.revokeObjectURL(url)
}
