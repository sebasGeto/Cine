import axios from 'axios'

// Cliente HTTP único para hablar con el backend.
// Si hay un token guardado, se envía en cada petición.
const api = axios.create({ baseURL: '/api' })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export default api
