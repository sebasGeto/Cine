import { useEffect, useState } from 'react'
import { getAccountHistory } from '../api/account.js'
import { errorMessage } from '../api/catalog.js'
import CatalogGate from '../catalogo/CatalogGate.jsx'
import ViewerNav from '../reproduccion/ViewerNav.jsx'
import { date, dateTime, money } from '../suscripciones/format.js'
import '../suscripciones/suscripciones.css'

// RF-04: historial de suscripciones y pagos de la cuenta (vista v_account_history).
export default function AccountPage() {
  return <CatalogGate role="CLIENT" needsProfile={false}>{() => <History />}</CatalogGate>
}

function History() {
  const [rows, setRows] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    let active = true
    getAccountHistory()
      .then((data) => active && setRows(data))
      .catch((e) => active && setError(errorMessage(e)))
    return () => { active = false }
  }, [])

  return (
    <main>
      <ViewerNav />
      <h1>Mi cuenta</h1>
      <h2>Suscripciones y pagos</h2>
      {error && <p className="cat-error" role="alert">{error}</p>}
      {rows?.length === 0 && <p>Todavía no has contratado ningún plan.</p>}
      {rows?.length > 0 && (
        <div className="cat-table">
          <table>
            <thead>
              <tr><th>Plan</th><th>Desde</th><th>Hasta</th><th>Pago</th><th>Valor</th><th>Referencia</th><th>Estado</th></tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.subscriptionId} className={r.isCurrent ? 'sub-current-row' : undefined}>
                  <td>{r.planName}</td>
                  <td>{date(r.startsAt)}</td>
                  <td>{date(r.endsAt)}</td>
                  <td>{dateTime(r.paidAt)}</td>
                  <td>{r.amount != null ? money(r.amount) : '—'}</td>
                  <td className="cat-muted sub-ref" title={r.reference ?? undefined}>{r.reference ?? '—'}</td>
                  <td>{r.isCurrent
                    ? <span className="cat-status cat-published">Vigente</span>
                    : <span className="cat-status">Terminada</span>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </main>
  )
}
