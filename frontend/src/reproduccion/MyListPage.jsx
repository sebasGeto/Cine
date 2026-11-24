import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { TYPES, ageLabel } from '../catalogo/format.js'
import { errorMessage } from '../api/catalog.js'
import { getMyList, removeFromMyList } from '../api/playback.js'
import CatalogGate from '../catalogo/CatalogGate.jsx'
import Cover from '../catalogo/Cover.jsx'
import ViewerNav from './ViewerNav.jsx'
import { playPath } from './time.js'
import './reproduccion.css'

// RF-26: Mi lista del perfil. Los contenidos retirados del catálogo se ven marcados y no se pueden
// reproducir, pero se pueden quitar (RF-25).
export default function MyListPage() {
  return <CatalogGate role="CLIENT">{(profileId) => <MyList profileId={profileId} />}</CatalogGate>
}

function MyList({ profileId }) {
  const [items, setItems] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    let active = true
    getMyList(profileId)
      .then((data) => active && setItems(data))
      .catch((e) => active && setError(errorMessage(e)))
    return () => { active = false }
  }, [profileId])

  async function remove(contentId) {
    setError(null)
    try {
      await removeFromMyList(profileId, contentId)
      setItems((list) => list.filter((i) => i.contentId !== contentId))
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <main>
      <ViewerNav />
      <h1>Mi lista</h1>
      {error && <p className="cat-error" role="alert">{error}</p>}
      {items?.length === 0 && <p>Todavía no has guardado contenidos. Agrégalos desde su detalle en el catálogo.</p>}
      {items?.length > 0 && (
        <ul className="rep-list">
          {items.map((i) => (
            <li key={i.contentId} className={i.available ? '' : 'rep-dim'}>
              <Cover url={i.coverUrl} title={i.title ?? 'Contenido retirado'} />
              <div>
                <strong>{i.title ?? 'Contenido no disponible'}</strong>{' '}
                {i.retired && <span className="cat-status cat-retired">Retirado</span>}
                {!i.retired && !i.allowedForProfile && <span className="cat-status">No permitido para este perfil</span>}
                <p className="cat-muted">
                  {[TYPES[i.type], i.releaseYear, i.ageRating != null && ageLabel(i.ageRating),
                    `agregado el ${new Date(i.addedAt).toLocaleDateString()}`].filter(Boolean).join(' · ')}
                </p>
                <div className="cat-actions">
                  {i.available && i.type !== 'series' && (
                    <Link className="cat-button cat-primary" to={playPath(i.contentId)}>Reproducir</Link>
                  )}
                  {i.available && <Link to={`/catalogo/${i.contentId}`}>Ver detalle</Link>}
                  <button type="button" onClick={() => remove(i.contentId)}>Quitar</button>
                </div>
              </div>
            </li>
          ))}
        </ul>
      )}
    </main>
  )
}
