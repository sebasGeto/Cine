import api from './client.js'

// Peticiones de Mi lista y reproducción. El token lo agrega client.js; todas son de cliente y
// envían el perfil con el que se navega (X-Profile-Id).

const asProfile = (profileId) => ({ headers: { 'X-Profile-Id': profileId } })

// Mi lista (RF-24 a RF-26)
export const getMyList = (profileId) =>
  api.get('/my-list', asProfile(profileId)).then((r) => r.data)

export const addToMyList = (profileId, contentId) =>
  api.post('/my-list', { contentId }, asProfile(profileId)).then((r) => r.data)

export const removeFromMyList = (profileId, contentId) =>
  api.delete(`/my-list/${contentId}`, asProfile(profileId))

// Reproducción (RF-27 a RF-30)
export const startSession = (profileId, contentId, episodeId) =>
  api.post('/playback/sessions', { contentId, episodeId: episodeId || null }, asProfile(profileId))
    .then((r) => r.data)

// type: pause | resume | seek | finish. En seek, positionSec es desde dónde se salta y toSec a dónde.
export const sendEvent = (profileId, sessionId, event) =>
  api.post(`/playback/sessions/${sessionId}/events`, event, asProfile(profileId)).then((r) => r.data)

// Pausa al cerrar la pestaña: keepalive deja terminar la petición aunque la página se descargue
export function pauseOnExit(profileId, sessionId, positionSec) {
  const token = localStorage.getItem('token')
  fetch(`/api/playback/sessions/${sessionId}/events`, {
    method: 'POST',
    keepalive: true,
    headers: {
      'Content-Type': 'application/json',
      'X-Profile-Id': profileId,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ type: 'pause', positionSec }),
  }).catch(() => {})
}

// Continuar viendo (RF-31) e historial
export const getContinueWatching = (profileId) =>
  api.get('/playback/continue-watching', asProfile(profileId)).then((r) => r.data)

export const getHistory = (profileId, page, size) =>
  api.get('/playback/history', { ...asProfile(profileId), params: { page, size } }).then((r) => r.data)
