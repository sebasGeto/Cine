import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getContinueWatching } from '../api/playback.js'
import Cover from '../catalogo/Cover.jsx'
import { episodeLabel, playPath } from './time.js'
import './reproduccion.css'

// RF-31: lo que el perfil dejó a medias, del más reciente al más antiguo, con la posición guardada.
// No se muestra nada si no hay contenidos en progreso.
export default function ContinueWatching({ profileId }) {
  const [items, setItems] = useState([])

  useEffect(() => {
    let active = true
    getContinueWatching(profileId)
      .then((data) => active && setItems(data))
      .catch(() => active && setItems([]))
    return () => { active = false }
  }, [profileId])

  if (items.length === 0) return null

  return (
    <section aria-labelledby="continuar-viendo">
      <h2 id="continuar-viendo">Continuar viendo</h2>
      <ul className="rep-row">
        {items.map((i) => (
          <li key={i.contentId}>
            <Link to={playPath(i.contentId, i.episodeId)} title="Continuar">
              <Cover url={i.coverUrl} title={i.title} />
            </Link>
            <strong>{i.title}</strong>
            {i.seasonNumber && <span className="cat-muted">{episodeLabel(i.seasonNumber, i.episodeNumber)}</span>}
            <div className="rep-bar" aria-label={`${i.progressPercent} % visto`}>
              <span style={{ width: `${i.progressPercent}%` }} />
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}
