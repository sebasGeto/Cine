import { useEffect, useState } from 'react'
import api from '../api/client.js'

// Página temporal: verifica que el backend responde y que ve las dos bases.
export default function HomePage() {
  const [health, setHealth] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    api.get('/health')
      .then((res) => setHealth(res.data))
      .catch(() => setError('No se pudo contactar el backend'))
  }, [])

  return (
    <main>
      <h1>CineConecta</h1>
      {error && <p>{error}</p>}
      {health && (
        <ul>
          <li>PostgreSQL: {health.postgres}</li>
          <li>MongoDB: {health.mongo}</li>
        </ul>
      )}
    </main>
  )
}
