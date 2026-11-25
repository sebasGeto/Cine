import axios from 'axios'

// Cliente HTTP único para hablar con el backend.
// Si hay un token guardado, se envía en cada petición.
const api = axios.create({ baseURL: '/api' })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Token vencido o inválido: se borra la sesión y se vuelve al inicio de sesión que corresponde.
// Las rutas /auth/ se excluyen: ahí un 401 es "usuario o contraseña incorrectos".
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const url = error.config?.url ?? ''
    if (error.response?.status === 401 && !url.startsWith('/auth/')) {
      localStorage.removeItem('token')
      localStorage.removeItem('profileId')
      localStorage.removeItem('profileName')
      window.location.assign(window.location.pathname.startsWith('/admin') ? '/admin/login' : '/login')
    }
    return Promise.reject(error)
  },
)

export default api
