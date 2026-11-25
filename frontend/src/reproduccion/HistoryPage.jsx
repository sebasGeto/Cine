import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { TYPES } from '../catalogo/format.js'
import { errorMessage } from '../api/catalog.js'
import { getHistory } from '../api/playback.js'
import CatalogGate from '../catalogo/CatalogGate.jsx'
import Pagination from '../catalogo/Pagination.jsx'
import ViewerNav from './ViewerNav.jsx'
import { clock, episodeLabel } from './time.js'
import './reproduccion.css'

const SIZE = 20
const STATUS = { open: 'En curso', closed: 'Terminada', abandoned: 'Abandonada' }

// Historial de sesiones del perfil: se conserva aunque el contenido se retire o se vuelva a ver.
export default function HistoryPage() {
  return <CatalogGate role="CLIENT">{(profileId) => <History profileId={profileId} />}</CatalogGate>
}

function History({ profileId }) {
  const [page, setPage] = useState(0)
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    let active = true
    getHistory(profileId, page, SIZE)
      .then((data) => {
        if (!active) return
        setResult(data)
        setError(null)
      })
      .catch((e) => active && setError(errorMessage(e)))
    return () => { active = false }
  }, [profileId, page])

  return (
    <main>
      <ViewerNav />
      <h1>Historial</h1>
      {error && <p className="cat-error" role="alert">{error}</p>}
      {result?.totalElements === 0 && <p>Este perfil todavía no ha reproducido nada.</p>}
      {result?.totalElements > 0 && (
        <>
          <div className="cat-table">
            <table>
              <thead>
                <tr><th>Inicio</th><th>Contenido</th><th>Tipo</th><th>Visto</th><th>Posición</th><th>Estado</th></tr>
              </thead>
              <tbody>
                {result.items.map((s) => (
                  <tr key={s.sessionId}>
                    <td>{new Date(s.startedAt).toLocaleString()}</td>
                    <td>
                      {s.title ? <Link to={`/catalogo/${s.contentId}`}>{s.title}</Link> : <span className="cat-muted">Contenido retirado</span>}
                      {s.seasonNumber && <span className="cat-muted"> · {episodeLabel(s.seasonNumber, s.episodeNumber)}</span>}
                    </td>
                    <td>{TYPES[s.contentType]}</td>
                    <td>{clock(s.watchedSec)}</td>
                    <td>{clock(s.lastPositionSec)} / {clock(s.durationSec)}</td>
                    <td>{STATUS[s.status] ?? s.status}{s.completed && ' · completada'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={result.page} size={result.size} total={result.totalElements} onChange={setPage} />
        </>
      )}
    </main>
  )
}
