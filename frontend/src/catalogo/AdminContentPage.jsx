import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  createContent, errorMessage, getAdminContent, publishContent, retireContent, updateContent,
} from '../api/catalog.js'
import CatalogGate from './CatalogGate.jsx'
import ContentForm from './ContentForm.jsx'
import SeasonsEditor from './SeasonsEditor.jsx'
import { STATUSES } from './format.js'

// RF-14 a RF-19: crear o editar un contenido; en las series, también temporadas y episodios.
export default function AdminContentPage() {
  const { id } = useParams()
  return <CatalogGate role="ADMIN">{() => <Editor key={id ?? 'nuevo'} id={id} />}</CatalogGate>
}

function Editor({ id }) {
  const navigate = useNavigate()
  const [content, setContent] = useState(null)
  const [error, setError] = useState(null)
  const [saved, setSaved] = useState(false)

  useEffect(() => {
    if (!id) return undefined
    let active = true
    getAdminContent(id)
      .then((data) => active && setContent(data))
      .catch((e) => active && setError(errorMessage(e)))
    return () => { active = false }
  }, [id])

  async function run(action) {
    setError(null)
    setSaved(false)
    try {
      const data = await action()
      setContent(data)
      setSaved(true)
      return data
    } catch (e) {
      setError(errorMessage(e))
      return null
    }
  }

  async function save(request) {
    const data = await run(() => (id ? updateContent(id, request) : createContent(request)))
    if (data && !id) navigate(`/admin/catalogo/${data.id}`, { replace: true })
  }

  return (
    <main>
      <p><Link to="/admin/catalogo">← Administración del catálogo</Link></p>
      <h1>{id ? content?.title ?? 'Contenido' : 'Nuevo contenido'}</h1>
      {content && id && (
        <p className="cat-actions">
          <span className={`cat-status cat-${content.status}`}>{STATUSES[content.status]}</span>
          {content.status !== 'published' && <button type="button" onClick={() => run(() => publishContent(id))}>Publicar</button>}
          {content.status !== 'retired' && <button type="button" onClick={() => run(() => retireContent(id))}>Retirar</button>}
        </p>
      )}
      {error && <p className="cat-error" role="alert">{error}</p>}
      {saved && <p className="cat-ok" role="status">Cambios guardados.</p>}
      {(!id || content) && <ContentForm content={id ? content : null} onSave={save} />}
      {id && content?.type === 'series' && <SeasonsEditor content={content} run={run} />}
    </main>
  )
}
