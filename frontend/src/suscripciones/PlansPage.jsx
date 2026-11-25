import { useCallback, useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { getCurrentSubscription, getPlans, purchase } from '../api/account.js'
import { errorMessage } from '../api/catalog.js'
import CatalogGate from '../catalogo/CatalogGate.jsx'
import ViewerNav from '../reproduccion/ViewerNav.jsx'
import { dateTime, money } from './format.js'
import './suscripciones.css'

// RF-11 a RF-13: planes, suscripción vigente y contratación con pago simulado.
// Toda la regla vive en sp_purchase_subscription (una sola transacción: bloquea la cuenta, valida
// y registra suscripción y pago). La pantalla solo envía la petición y muestra el resultado.
export default function PlansPage() {
  return <CatalogGate role="CLIENT" needsProfile={false}>{() => <Plans />}</CatalogGate>
}

function Plans() {
  const location = useLocation()
  const [plans, setPlans] = useState(null)
  const [current, setCurrent] = useState(undefined)
  const [chosen, setChosen] = useState(null)
  const [error, setError] = useState(null)
  const [done, setDone] = useState(null)

  const load = useCallback(() => Promise.all([getPlans(), getCurrentSubscription()])
    .then(([list, subscription]) => {
      setPlans(list)
      setCurrent(subscription)
    })
    .catch((e) => setError(errorMessage(e))), [])

  useEffect(() => { load() }, [load])

  async function onPurchased(plan) {
    setChosen(null)
    setDone(`Listo: quedaste suscrito al plan ${plan.name}. Ya puedes crear tus perfiles.`)
    await load()
  }

  return (
    <main>
      <ViewerNav />
      <h1>Planes</h1>
      {location.state?.welcome && !done && current === null && (
        <p className="cat-ok">¡Bienvenido! Elige un plan para empezar a ver contenido.</p>
      )}
      {done && <p className="cat-ok" role="status">{done} <Link to="/perfiles">Ir a perfiles</Link></p>}
      {error && <p className="cat-error" role="alert">{error}</p>}

      {current && (
        <section className="sub-current">
          <h2>Tu suscripción</h2>
          <p>
            Plan <strong>{current.planName}</strong> ({current.maxProfiles}{' '}
            {current.maxProfiles === 1 ? 'perfil' : 'perfiles'})
          </p>
          <p>Vigente hasta: <strong>{dateTime(current.endsAt)}</strong></p>
          <p className="cat-muted">Podrás contratar de nuevo cuando termine este período.</p>
        </section>
      )}
      {current === null && <p>No tienes una suscripción vigente.</p>}

      {plans && (
        <ul className="sub-plans">
          {plans.map((p) => (
            <li key={p.planId} className={current?.planId === p.planId ? 'sub-mine' : undefined}>
              <h3>{p.name}</h3>
              <p className="sub-price">{money(p.price)} <span className="cat-muted">/ 30 días</span></p>
              <p>Hasta {p.maxProfiles} {p.maxProfiles === 1 ? 'perfil activo' : 'perfiles activos'}</p>
              {current?.planId === p.planId && <span className="cat-status cat-published">Tu plan actual</span>}
              {!current && (
                <button type="button" className="cat-primary" onClick={() => { setChosen(p); setDone(null) }}>
                  Contratar
                </button>
              )}
            </li>
          ))}
        </ul>
      )}

      {chosen && <Checkout plan={chosen} onCancel={() => setChosen(null)} onDone={() => onPurchased(chosen)} />}
    </main>
  )
}

// Pago simulado. "Simular fallo" provoca un error después de insertar la suscripción y antes del
// pago: sirve para mostrar en la demo que la transacción se deshace completa (RF-12).
function Checkout({ plan, onCancel, onDone }) {
  const [error, setError] = useState(null)
  const [sending, setSending] = useState(false)

  async function submit(event) {
    event.preventDefault()
    const form = new FormData(event.target)
    setError(null)
    setSending(true)
    try {
      await purchase(plan.planId, form.get('result') === 'approved', form.get('simulateFailure') === 'on')
      onDone()
    } catch (e) {
      setError(errorMessage(e))
      setSending(false)
    }
  }

  return (
    <section className="sub-checkout">
      <h2>Contratar plan {plan.name}</h2>
      <p>Total a pagar: <strong>{money(plan.price)}</strong> por 30 días.</p>
      <form className="cat-form" onSubmit={submit}>
        <fieldset>
          <legend>Resultado del pago (simulado)</legend>
          <label className="sub-radio"><input type="radio" name="result" value="approved" defaultChecked /> Aprobado</label>
          <label className="sub-radio"><input type="radio" name="result" value="rejected" /> Rechazado</label>
        </fieldset>
        <label className="sub-radio">
          <input type="checkbox" name="simulateFailure" /> Simular un fallo antes de registrar el pago (demo)
        </label>
        {error && <p className="cat-error" role="alert">{error}</p>}
        <div className="cat-actions">
          <button type="submit" className="cat-primary" disabled={sending}>{sending ? 'Procesando…' : 'Pagar'}</button>
          <button type="button" onClick={onCancel} disabled={sending}>Cancelar</button>
        </div>
      </form>
    </section>
  )
}
