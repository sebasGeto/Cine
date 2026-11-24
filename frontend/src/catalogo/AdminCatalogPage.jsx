import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { errorMessage, listContents, publishContent, retireContent } from '../api/catalog.js'
import CatalogGate from './CatalogGate.jsx'
import Pagination from './Pagination.jsx'
import { STATUSES, TYPES, ageLabel } from './format.js'

const FILTERS = ['title', 'type', 'status']
const SIZE = 12

// RF-18 a RF-20: listado del administrador, con publicar y retirar.
export default function AdminCatalogPage() {
  return <CatalogGate role="ADMIN">{() => <List />}</CatalogGate>
}

function List() {
  const [params, setParams] = useSearchParams()
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)
  const [version, setVersion] = useState(0)
  const query = params.toString()

  useEffect(() => {
    let active = true
    const current = new URLSearchParams(query)
    const filters = Object.fromEntries(FILTERS.map((f) => [f, current.get(f)]))
    listContents({ ...filters, page: current.get('page') ?? 0, size: SIZE })
      .then((data) => active && setResult(data))
      .catch((e) => active && setError(errorMessage(e)))
    return () => { active = false }
  }, [query, version])

  function submit(event) {
    event.preventDefault()
    const form = new FormData(event.target)
    setParams(FILTERS.filter((f) => form.get(f).trim()).map((f) => [f, form.get(f).trim()]))
  }

  async function change(action, id) {
    setError(null)
    try {
      await action(id)
      setVersion((v) => v + 1)
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <main>
      <h1>Administración del catálogo</h1>
      <p><Link className="cat-button cat-primary" to="/admin/catalogo/nuevo">Nuevo contenido</Link></p>
      <form key={query} className="cat-filters" onSubmit={submit}>
        <label>Título <input name="title" type="search" defaultValue={params.get('title') ?? ''} /></label>
        <label>Tipo
          <select name="type" defaultValue={params.get('type') ?? ''}>
            <option value="">Todos</option>
            {Object.entries(TYPES).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </label>
        <label>Estado
          <select name="status" defaultValue={params.get('status') ?? ''}>
            <option value="">Todos</option>
            {Object.entries(STATUSES).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </label>
        <button type="submit" className="cat-primary">Buscar</button>
        {query && <button type="button" onClick={() => setParams({})}>Limpiar</button>}
      </form>

      {error && <p className="cat-error" role="alert">{error}</p>}
      {result?.total === 0 && <p>No hay contenidos con esos filtros.</p>}
      {result && result.total > 0 && (
        <>
          <div className="cat-table">
            <table>
              <thead>
                <tr><th>Título</th><th>Tipo</th><th>Año</th><th>Clasificación</th><th>Estado</th><th>Acciones</th></tr>
              </thead>
              <tbody>
                {result.items.map((c) => (
                  <tr key={c.id}>
                    <td><Link to={`/admin/catalogo/${c.id}`}>{c.title}</Link></td>
                    <td>{TYPES[c.type]}</td>
                    <td>{c.releaseYear}</td>
                    <td>{ageLabel(c.ageRating)}</td>
                    <td><span className={`cat-status cat-${c.status}`}>{STATUSES[c.status]}</span></td>
                    <td className="cat-actions">
                      {c.status !== 'published' && <button type="button" onClick={() => change(publishContent, c.id)}>Publicar</button>}
                      {c.status !== 'retired' && <button type="button" onClick={() => change(retireContent, c.id)}>Retirar</button>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination {...result} onChange={(page) => {
            const next = new URLSearchParams(query)
            next.set('page', page)
            setParams(next)
          }} />
        </>
      )}
    </main>
  )
}
