import { useState } from 'react'
import RowsEditor from './RowsEditor.jsx'
import { AGE_RATINGS, SOURCE_KINDS, TYPES, ageLabel } from './format.js'

const CAST = [{ name: 'name', label: 'Nombre' }, { name: 'character', label: 'Personaje (si se conoce)', optional: true }]
const INTERVIEWEES = [{ name: 'name', label: 'Nombre' }, { name: 'description', label: 'Descripción', optional: true }]
const SOURCES = [
  { name: 'name', label: 'Nombre' },
  { name: 'kind', label: 'Clase', options: SOURCE_KINDS },
  { name: 'url', label: 'Dirección web', optional: true },
]
const EPISODES = [
  { name: 'number', label: 'Número', type: 'number' },
  { name: 'title', label: 'Título' },
  { name: 'synopsis', label: 'Sinopsis' },
  { name: 'durationSec', label: 'Duración (s)', type: 'number' },
]

const toList = (text) => text.split(',').map((t) => t.trim()).filter(Boolean)
const fromList = (list) => (list ?? []).join(', ')

// Formulario de los tres tipos. Solo envía los campos que corresponden al tipo elegido.
// Las validaciones de negocio las responde el servidor y se muestran tal cual.
export default function ContentForm({ content, onSave }) {
  const [type, setType] = useState(content?.type ?? 'movie')
  const [cast, setCast] = useState(content?.cast ?? [])
  const [interviewees, setInterviewees] = useState(content?.interviewees ?? [])
  const [sources, setSources] = useState(content?.sources ?? [])
  const [episodes, setEpisodes] = useState([])
  const [saving, setSaving] = useState(false)

  async function submit(event) {
    event.preventDefault()
    const form = new FormData(event.target)
    const list = (name) => toList(form.get(name))
    const number = (name) => Number(form.get(name))
    const request = {
      type,
      title: form.get('title'),
      synopsis: form.get('synopsis'),
      releaseYear: number('releaseYear'),
      genres: list('genres'),
      countries: list('countries'),
      audioLanguages: list('audioLanguages'),
      subtitleLanguages: list('subtitleLanguages'),
      ageRating: number('ageRating'),
      coverUrl: form.get('coverUrl'),
    }
    if (type !== 'series') {
      request.durationSec = number('durationSec')
      request.directors = list('directors')
    }
    if (type === 'movie') request.cast = cast
    if (type === 'documentary') Object.assign(request, { topics: list('topics'), interviewees, sources })
    if (type === 'series' && !content) {
      request.seasons = [{
        number: number('seasonNumber'),
        description: form.get('seasonDescription'),
        episodes: episodes.map((e) => ({ ...e, number: Number(e.number), durationSec: Number(e.durationSec) })),
      }]
    }
    setSaving(true)
    await onSave(request)
    setSaving(false)
  }

  return (
    <form className="cat-form" onSubmit={submit}>
      <label>Tipo
        <select value={type} disabled={!!content} onChange={(e) => setType(e.target.value)}>
          {Object.entries(TYPES).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select>
      </label>
      <label>Título <input name="title" defaultValue={content?.title} required /></label>
      <label>Sinopsis <textarea name="synopsis" rows="3" defaultValue={content?.synopsis} required /></label>
      <div className="cat-columns">
        <label>Año de estreno
          <input name="releaseYear" type="number" min="1888" max="2100" defaultValue={content?.releaseYear} required />
        </label>
        <label>Clasificación por edad
          <select name="ageRating" defaultValue={content?.ageRating ?? 0}>
            {AGE_RATINGS.map((r) => <option key={r} value={r}>{ageLabel(r)}</option>)}
          </select>
        </label>
        {type !== 'series' && (
          <label>Duración (segundos)
            <input name="durationSec" type="number" min="1" defaultValue={content?.durationSec} required />
          </label>
        )}
      </div>
      <p className="cat-muted">Separe con comas los campos que admiten varios valores.</p>
      <div className="cat-columns">
        <label>Géneros <input name="genres" defaultValue={fromList(content?.genres)} placeholder="drama, misterio" required /></label>
        <label>Países <input name="countries" defaultValue={fromList(content?.countries)} placeholder="CO, MX" required /></label>
        <label>Idiomas de audio <input name="audioLanguages" defaultValue={fromList(content?.audioLanguages)} placeholder="es, en" required /></label>
        <label>Subtítulos <input name="subtitleLanguages" defaultValue={fromList(content?.subtitleLanguages)} placeholder="es, en" /></label>
        {type !== 'series' && <label>Dirección <input name="directors" defaultValue={fromList(content?.directors)} required /></label>}
        {type === 'documentary' && <label>Temas <input name="topics" defaultValue={fromList(content?.topics)} required /></label>}
      </div>
      <label>Dirección de la portada
        <input name="coverUrl" type="url" defaultValue={content?.coverUrl} placeholder="https://" required />
      </label>
      {type === 'movie' && <RowsEditor title="Reparto" fields={CAST} rows={cast} onChange={setCast} />}
      {type === 'documentary' && (
        <>
          <RowsEditor title="Personas entrevistadas" fields={INTERVIEWEES} rows={interviewees} onChange={setInterviewees} />
          <RowsEditor title="Fuentes e instituciones" fields={SOURCES} rows={sources} onChange={setSources} />
        </>
      )}
      {type === 'series' && !content && (
        <fieldset>
          <legend>Primera temporada</legend>
          <div className="cat-columns">
            <label>Número <input name="seasonNumber" type="number" min="1" defaultValue="1" required /></label>
            <label>Descripción <input name="seasonDescription" required /></label>
          </div>
          <RowsEditor title="Episodios" fields={EPISODES} rows={episodes} onChange={setEpisodes} />
        </fieldset>
      )}
      <button type="submit" className="cat-primary" disabled={saving}>{content ? 'Guardar cambios' : 'Crear en borrador'}</button>
    </form>
  )
}
