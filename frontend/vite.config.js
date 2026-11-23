import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// En desarrollo, las peticiones a /api se redirigen al backend de Spring Boot.
// Así el navegador no hace peticiones a otro origen y no hace falta configurar CORS.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
