import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { errorMessage, searchContents } from '../api/catalog.js'
import CatalogGate from './CatalogGate.jsx'
import Cover from './Cover.jsx'
import Pagination from './Pagination.jsx'
import { TYPES, ageLabel } from './format.js'
import ContinueWatching from '../reproduccion/ContinueWatching.jsx'
import ViewerNav from '../reproduccion/ViewerNav.jsx'

const FILTERS = ['q', 'type', 'genre', 'language', 'year']
const SIZE = 12

// RF-21: búsqueda del cliente. Los filtros viven en la URL para no perderlos al volver de un detalle.
export default function CatalogPage() {
  return <CatalogGate role="CLIENT">{(profileId) => <Search profileId={profileId} />}</CatalogGate>
}

function Search({ profileId }) {
  const [params, setParams] = useSearchParams()
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)
  const query = params.toString()

  useEffect(() => {
    let active = true
    const current = new URLSearchParams(query)
    const filters = Object.fromEntries(FILTERS.map((f) => [f, current.get(f)]))
    searchContents(profileId, { ...filters, page: current.get('page') ?? 0, size: SIZE })
      .then((data) => {
        if (!active) return
        setResult(data)
        setError(null)
      })
      .catch((e) => {
        if (!active) return
        setResult(null)
        setError(errorMessage(e))
      })
    // Si cambian los filtros antes de que llegue la respuesta, esta se ignora
    return () => { active = false }
  }, [profileId, query])

  function submit(event) {
    event.preventDefault()
    const form = new FormData(event.target)
    setParams(FILTERS.filter((f) => form.get(f).trim()).map((f) => [f, form.get(f).trim()]))
  }

  function goTo(page) {
    const next = new URLSearchParams(query)
    next.set('page', page)
    setParams(next)
  }

  return (
    <main>
      <ViewerNav />
      <ContinueWatching profileId={profileId} />
      <h1>Catálogo</h1>
      {/* key: al cambiar la URL (atrás, limpiar) los campos muestran los filtros aplicados */}
      <form key={query} className="cat-filters" onSubmit={submit}>
        <label>Título <input name="q" type="search" defaultValue={params.get('q') ?? ''} /></label>
        <label>Tipo
          <select name="type" defaultValue={params.get('type') ?? ''}>
            <option value="">Todos</option>
            {Object.entries(TYPES).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </label>
        <label>Género <input name="genre" defaultValue={params.get('genre') ?? ''} placeholder="drama" /></label>
        <label>Idioma <input name="language" defaultValue={params.get('language') ?? ''} placeholder="es" /></label>
        <label>Año <input name="year" type="number" min="1888" max="2100" defaultValue={params.get('year') ?? ''} /></label>
        <button type="submit" className="cat-primary">Buscar</button>
        {query && <button type="button" onClick={() => setParams({})}>Limpiar</button>}
      </form>

      {error && <p className="cat-error" role="alert">{error}</p>}
      {result?.total === 0 && <p>No hay contenidos con esos filtros.</p>}
      {result && result.total > 0 && (
        <>
          <ul className="cat-grid">
            {result.items.map((c) => (
              <li key={c.id}>
                <Link to={`/catalogo/${c.id}`}>
                  <Cover url={c.coverUrl} title={c.title} />
                  <strong>{c.title}</strong>
                </Link>
                <span>{TYPES[c.type]} · {c.releaseYear} · {ageLabel(c.ageRating)}</span>
                <span className="cat-muted">{c.genres.join(', ')}</span>
              </li>
            ))}
          </ul>
          <Pagination {...result} onChange={goTo} />
        </>
      )}
    </main>
  )
}
