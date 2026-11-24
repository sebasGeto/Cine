import { useState } from 'react'
import { addEpisode, addSeason, updateEpisode, updateSeason } from '../api/catalog.js'
import { durationLabel } from './format.js'

const nextNumber = (items) => Math.max(0, ...items.map((i) => i.number)) + 1

const episodeFrom = (form) => ({
  title: form.get('title'),
  synopsis: form.get('synopsis'),
  durationSec: Number(form.get('durationSec')),
})

// RF-16 y RF-17. El número de una temporada o un episodio no se edita: los identifica.
export default function SeasonsEditor({ content, run }) {
  return (
    <section>
      <h2>Temporadas y episodios</h2>
      {content.seasons.map((s) => <SeasonBlock key={s.number} id={content.id} season={s} run={run} />)}
      <SubmitForm key={content.seasons.length} title="Nueva temporada" button="Agregar temporada"
        onSubmit={(form) => run(() => addSeason(content.id, {
          number: Number(form.get('number')), description: form.get('description'),
        }))}>
        <label>Número <input name="number" type="number" min="1" defaultValue={nextNumber(content.seasons)} required /></label>
        <label>Descripción <input name="description" required /></label>
      </SubmitForm>
    </section>
  )
}

function SeasonBlock({ id, season, run }) {
  const [editing, setEditing] = useState(null) // 'season' o el número del episodio

  async function finish(action) {
    if (await run(action)) setEditing(null)
  }

  return (
    <div className="cat-season">
      {editing === 'season' ? (
        <SubmitForm title={`Temporada ${season.number}`} button="Guardar" onCancel={() => setEditing(null)}
          onSubmit={(form) => finish(() => updateSeason(id, season.number, { description: form.get('description') }))}>
          <label>Descripción <input name="description" defaultValue={season.description} required /></label>
        </SubmitForm>
      ) : (
        <h3>
          Temporada {season.number}: {season.description}{' '}
          <button type="button" onClick={() => setEditing('season')}>Editar</button>
        </h3>
      )}
      <ol className="cat-episodes">
        {season.episodes.map((e) => (
          <li key={e.episodeId} value={e.number}>
            {editing === e.number ? (
              <SubmitForm title={`Episodio ${e.number}`} button="Guardar" onCancel={() => setEditing(null)}
                onSubmit={(form) => finish(() => updateEpisode(id, season.number, e.number, episodeFrom(form)))}>
                <EpisodeFields episode={e} />
              </SubmitForm>
            ) : (
              <>
                <strong>{e.title}</strong> <span className="cat-muted">{durationLabel(e.durationSec)}</span>{' '}
                <button type="button" onClick={() => setEditing(e.number)}>Editar</button>
                <div>{e.synopsis}</div>
              </>
            )}
          </li>
        ))}
      </ol>
      <details>
        <summary>Agregar episodio</summary>
        <SubmitForm key={season.episodes.length} title="Nuevo episodio" button="Agregar episodio"
          onSubmit={(form) => run(() => addEpisode(id, season.number, {
            number: Number(form.get('number')), ...episodeFrom(form),
          }))}>
          <label>Número <input name="number" type="number" min="1" defaultValue={nextNumber(season.episodes)} required /></label>
          <EpisodeFields />
        </SubmitForm>
      </details>
    </div>
  )
}

function EpisodeFields({ episode }) {
  return (
    <>
      <label>Título <input name="title" defaultValue={episode?.title} required /></label>
      <label>Sinopsis <input name="synopsis" defaultValue={episode?.synopsis} required /></label>
      <label>Duración (segundos)
        <input name="durationSec" type="number" min="1" defaultValue={episode?.durationSec} required />
      </label>
    </>
  )
}

function SubmitForm({ title, button, onSubmit, onCancel, children }) {
  return (
    <form onSubmit={(e) => { e.preventDefault(); onSubmit(new FormData(e.target)) }}>
      <fieldset>
        <legend>{title}</legend>
        <div className="cat-row">
          {children}
          <button type="submit" className="cat-primary">{button}</button>
          {onCancel && <button type="button" onClick={onCancel}>Cancelar</button>}
        </div>
      </fieldset>
    </form>
  )
}
