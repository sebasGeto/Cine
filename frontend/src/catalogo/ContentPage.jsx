import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { errorMessage, getContent, getSeasons } from '../api/catalog.js'
import CatalogGate from './CatalogGate.jsx'
import Cover from './Cover.jsx'
import { SOURCE_KINDS, TYPES, ageLabel, durationLabel } from './format.js'
import MyListButton from '../reproduccion/MyListButton.jsx'
import { playPath } from '../reproduccion/time.js'

// RF-22 y RF-23: detalle de un contenido; si es serie, sus temporadas y episodios.
export default function ContentPage() {
  const { id } = useParams()
  return <CatalogGate role="CLIENT">{(profileId) => <Detail key={id} id={id} profileId={profileId} />}</CatalogGate>
}

function Detail({ id, profileId }) {
  const navigate = useNavigate()
  const location = useLocation()
  const [content, setContent] = useState(null)
  const [seasons, setSeasons] = useState([])
  const [error, setError] = useState(null)

  useEffect(() => {
    let active = true
    async function load() {
      try {
        const data = await getContent(profileId, id)
        const list = data.type === 'series' ? await getSeasons(profileId, id) : []
        if (!active) return
        setContent(data)
        setSeasons(list)
      } catch (e) {
        if (active) setError(errorMessage(e))
      }
    }
    load()
    return () => { active = false }
  }, [id, profileId])

  // Vuelve a la búsqueda con sus filtros; si se entró directo por el enlace no hay adónde volver
  const back = () => (location.key === 'default' ? navigate('/catalogo') : navigate(-1))

  return (
    <main>
      <p><button type="button" onClick={back}>← Catálogo</button></p>
      {error && <p className="cat-error" role="alert">{error}</p>}
      {content && (
        <article className="cat-detail">
          <Cover url={content.coverUrl} title={content.title} />
          <div>
            <h1>{content.title}</h1>
            <p className="cat-muted">
              {[TYPES[content.type], content.releaseYear, ageLabel(content.ageRating),
                content.durationSec && durationLabel(content.durationSec)].filter(Boolean).join(' · ')}
            </p>
            <p>{content.synopsis}</p>
            <div className="cat-actions">
              {content.type !== 'series' && (
                <Link className="cat-button cat-primary" to={playPath(content.id)}>Reproducir</Link>
              )}
              <MyListButton profileId={profileId} contentId={content.id} />
            </div>
            <dl>
              <Field label="Géneros" values={content.genres} />
              <Field label="Países" values={content.countries} />
              <Field label="Audio" values={content.audioLanguages} />
              <Field label="Subtítulos" values={content.subtitleLanguages} />
              <Field label="Dirección" values={content.directors} />
              <Field label="Reparto" values={content.cast?.map((c) => (c.character ? `${c.name} (${c.character})` : c.name))} />
              <Field label="Temas" values={content.topics} />
              <Field label="Entrevistados" values={content.interviewees?.map((i) => (i.description ? `${i.name} (${i.description})` : i.name))} />
              <Field label="Fuentes" values={content.sources?.map((s) => `${s.name} (${SOURCE_KINDS[s.kind]})`)} />
            </dl>
            {seasons.map((s) => (
              <section key={s.number}>
                <h2>Temporada {s.number}: {s.description}</h2>
                {s.episodes.length === 0 && <p className="cat-muted">Sin episodios todavía.</p>}
                <ol className="cat-episodes">
                  {s.episodes.map((e) => (
                    <li key={e.episodeId} value={e.number}>
                      <strong>{e.title}</strong> <span className="cat-muted">{durationLabel(e.durationSec)}</span>{' '}
                      <Link to={playPath(content.id, e.episodeId)}>Reproducir</Link>
                      <div>{e.synopsis}</div>
                    </li>
                  ))}
                </ol>
              </section>
            ))}
          </div>
        </article>
      )}
    </main>
  )
}

function Field({ label, values }) {
  if (!values?.length) return null
  return <><dt>{label}</dt><dd>{values.join(', ')}</dd></>
}
