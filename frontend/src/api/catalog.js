import api from './client.js'

// Peticiones del catálogo. El token lo agrega client.js; las rutas de cliente envían además
// el perfil con el que se navega (X-Profile-Id).

const asProfile = (profileId) => ({ headers: { 'X-Profile-Id': profileId } })

// Quita los filtros vacíos para que no viajen en la URL
const clean = (params) =>
  Object.fromEntries(Object.entries(params).filter(([, v]) => v !== '' && v != null))

// Cliente
export const searchContents = (profileId, filters) =>
  api.get('/contents', { ...asProfile(profileId), params: clean(filters) }).then((r) => r.data)

export const getContent = (profileId, id) =>
  api.get(`/contents/${id}`, asProfile(profileId)).then((r) => r.data)

export const getSeasons = (profileId, id) =>
  api.get(`/contents/${id}/seasons`, asProfile(profileId)).then((r) => r.data)

// Administración
const ADMIN = '/admin/contents'

export const listContents = (filters) =>
  api.get(ADMIN, { params: clean(filters) }).then((r) => r.data)

export const getAdminContent = (id) => api.get(`${ADMIN}/${id}`).then((r) => r.data)

export const createContent = (content) => api.post(ADMIN, content).then((r) => r.data)

export const updateContent = (id, content) => api.put(`${ADMIN}/${id}`, content).then((r) => r.data)

export const publishContent = (id) => api.patch(`${ADMIN}/${id}/publish`).then((r) => r.data)

export const retireContent = (id) => api.patch(`${ADMIN}/${id}/retire`).then((r) => r.data)

export const addSeason = (id, season) =>
  api.post(`${ADMIN}/${id}/seasons`, season).then((r) => r.data)

export const updateSeason = (id, number, changes) =>
  api.put(`${ADMIN}/${id}/seasons/${number}`, changes).then((r) => r.data)

export const addEpisode = (id, season, episode) =>
  api.post(`${ADMIN}/${id}/seasons/${season}/episodes`, episode).then((r) => r.data)

export const updateEpisode = (id, season, number, changes) =>
  api.put(`${ADMIN}/${id}/seasons/${season}/episodes/${number}`, changes).then((r) => r.data)

// Mensaje del backend ({code, message}) o uno genérico si no hubo respuesta. Los mensajes de
// las funciones y triggers de PostgreSQL llegan en minúscula: se pone la primera en mayúscula.
export function errorMessage(error) {
  const message = error.response?.data?.message ?? 'No se pudo contactar el backend'
  return message.charAt(0).toUpperCase() + message.slice(1)
}
