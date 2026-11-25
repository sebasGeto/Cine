import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  activateProfile, createProfile, deactivateProfile, getCurrentSubscription, getProfiles,
  selectProfile, updateProfile,
} from '../api/account.js'
import { errorMessage } from '../api/catalog.js'
import CatalogGate from '../catalogo/CatalogGate.jsx'
import { AGE_RATINGS, ageLabel } from '../catalogo/format.js'
import { currentProfileId, forgetProfile, rememberProfile } from '../catalogo/session.js'
import ViewerNav from '../reproduccion/ViewerNav.jsx'
import './perfiles.css'

// RF-05 a RF-10: elegir con qué perfil navegar y administrar los perfiles de la cuenta.
// El cupo del plan, la suscripción vigente y el nombre repetido los valida PostgreSQL (trigger
// e índice único); aquí solo se muestran sus mensajes.
export default function ProfilesPage() {
  return <CatalogGate role="CLIENT" needsProfile={false}>{() => <Profiles />}</CatalogGate>
}

function Profiles() {
  const navigate = useNavigate()
  const [profiles, setProfiles] = useState(null)
  const [subscription, setSubscription] = useState(undefined)
  const [error, setError] = useState(null)
  const [editing, setEditing] = useState(null) // profileId en edición, 'new' o null

  const load = useCallback(() => Promise.all([getProfiles(), getCurrentSubscription()])
    .then(([list, current]) => {
      setProfiles(list)
      setSubscription(current)
    })
    .catch((e) => setError(errorMessage(e))), [])

  useEffect(() => { load() }, [load])

  // Ejecuta una acción del backend, muestra su error si falla y recarga la lista
  async function run(action) {
    setError(null)
    try {
      await action()
      setEditing(null)
      await load()
      return true
    } catch (e) {
      setError(errorMessage(e))
      return false
    }
  }

  async function choose(profile) {
    setError(null)
    try {
      const verified = await selectProfile(profile.profileId)
      rememberProfile(verified)
      navigate('/catalogo')
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  function deactivate(profile) {
    return run(async () => {
      await deactivateProfile(profile.profileId)
      if (String(profile.profileId) === currentProfileId()) forgetProfile()
    })
  }

  if (!profiles) return <main>{error ? <p className="cat-error" role="alert">{error}</p> : <p>Cargando…</p>}</main>

  const active = profiles.filter((p) => p.isActive)
  const inactive = profiles.filter((p) => !p.isActive)

  return (
    <main>
      <ViewerNav />
      <h1>¿Quién está viendo?</h1>

      {subscription === null && (
        <p className="cat-error">
          Tu cuenta no tiene una suscripción vigente: no puedes ver contenido ni crear o activar
          perfiles. <Link to="/planes">Ver planes</Link>
        </p>
      )}
      {error && <p className="cat-error" role="alert">{error}</p>}

      {active.length > 0 && (
        <ul className="pf-pick">
          {active.map((p) => (
            <li key={p.profileId}>
              <button type="button" onClick={() => choose(p)} disabled={!subscription}>
                <span className="pf-avatar" aria-hidden="true">{p.name.charAt(0).toUpperCase()}</span>
                <strong>{p.name}</strong>
                <span className="cat-muted">{ageLabel(p.maxAgeRating)}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
      {active.length === 0 && subscription && <p>Aún no tienes perfiles activos. Crea el primero.</p>}

      <h2>Administrar perfiles</h2>
      {subscription && (
        <p className="cat-muted">
          Plan {subscription.planName}: {active.length} de {subscription.maxProfiles} perfiles activos.
        </p>
      )}

      <div className="cat-table">
        <table>
          <thead>
            <tr><th>Nombre</th><th>Clasificación máxima</th><th>Estado</th><th>Acciones</th></tr>
          </thead>
          <tbody>
            {[...active, ...inactive].map((p) => editing === p.profileId
              ? (
                <tr key={p.profileId}>
                  <td colSpan={4}>
                    <ProfileForm profile={p} onCancel={() => setEditing(null)}
                      onSave={(data) => run(() => updateProfile(p.profileId, data))} />
                  </td>
                </tr>
              )
              : (
                <tr key={p.profileId} className={p.isActive ? undefined : 'rep-dim'}>
                  <td>{p.name}</td>
                  <td>{ageLabel(p.maxAgeRating)}</td>
                  <td><span className={`cat-status ${p.isActive ? 'cat-published' : 'cat-retired'}`}>
                    {p.isActive ? 'Activo' : 'Inactivo'}
                  </span></td>
                  <td className="cat-actions">
                    <button type="button" onClick={() => setEditing(p.profileId)}>Editar</button>
                    {p.isActive
                      ? <button type="button" onClick={() => deactivate(p)}>Desactivar</button>
                      : <button type="button" onClick={() => run(() => activateProfile(p.profileId))}>Activar</button>}
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {editing === 'new'
        ? <ProfileForm onCancel={() => setEditing(null)} onSave={(data) => run(() => createProfile(data))} />
        : <p><button type="button" className="cat-primary" onClick={() => setEditing('new')}>Nuevo perfil</button></p>}
    </main>
  )
}

// Crear o editar. La clasificación máxima limita lo que el perfil ve (perfiles infantiles).
function ProfileForm({ profile, onSave, onCancel }) {
  const [sending, setSending] = useState(false)

  async function submit(event) {
    event.preventDefault()
    const form = new FormData(event.target)
    setSending(true)
    const ok = await onSave({ name: form.get('name').trim(), maxAgeRating: Number(form.get('maxAgeRating')) })
    if (!ok) setSending(false)
  }

  return (
    <form className="cat-row pf-form" onSubmit={submit}>
      <label>Nombre
        <input name="name" defaultValue={profile?.name ?? ''} maxLength={60} required autoFocus />
      </label>
      <label>Clasificación máxima
        <select name="maxAgeRating" defaultValue={profile?.maxAgeRating ?? 18}>
          {AGE_RATINGS.map((r) => <option key={r} value={r}>{ageLabel(r)}</option>)}
        </select>
      </label>
      <div className="cat-actions">
        <button type="submit" className="cat-primary" disabled={sending}>{profile ? 'Guardar' : 'Crear perfil'}</button>
        <button type="button" onClick={onCancel}>Cancelar</button>
      </div>
    </form>
  )
}
