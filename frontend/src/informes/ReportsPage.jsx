import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { downloadReport, getReport } from '../api/account.js'
import { errorMessage } from '../api/catalog.js'
import CatalogGate from '../catalogo/CatalogGate.jsx'
import { TYPES } from '../catalogo/format.js'
import { money } from '../suscripciones/format.js'

// RF-35 a RF-37: ingresos por plan (PostgreSQL, vista v_revenue_by_plan con el usuario de solo
// lectura) y consumo por plan y tipo de contenido (MongoDB + nombres de plan de PostgreSQL).
// El rango vive en la URL; las fechas son días completos en UTC, ambos incluidos.
export default function ReportsPage() {
  return <CatalogGate role="ADMIN">{() => <Reports />}</CatalogGate>
}

const iso = (d) => d.toISOString().slice(0, 10)

function defaultRange() {
  const to = new Date()
  const from = new Date(to)
  from.setUTCDate(from.getUTCDate() - 90)
  return { from: iso(from), to: iso(to) }
}

function Reports() {
  const [params, setParams] = useSearchParams()
  const range = { ...defaultRange(), ...Object.fromEntries(params) }
  const [revenue, setRevenue] = useState(null)
  const [consumption, setConsumption] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    let active = true
    Promise.all([getReport('revenue', range.from, range.to), getReport('consumption', range.from, range.to)])
      .then(([r, c]) => {
        if (!active) return
        setRevenue(r)
        setConsumption(c)
        setError(null)
      })
      .catch((e) => {
        if (!active) return
        setRevenue(null)
        setConsumption(null)
        setError(errorMessage(e))
      })
    return () => { active = false }
  }, [range.from, range.to])

  function submit(event) {
    event.preventDefault()
    const form = new FormData(event.target)
    setParams({ from: form.get('from'), to: form.get('to') })
  }

  async function download(kind) {
    setError(null)
    try {
      await downloadReport(kind, range.from, range.to)
    } catch (e) {
      // Con responseType blob el error también llega como blob: se lee para mostrar el mensaje
      const text = await e.response?.data?.text?.().catch(() => null)
      setError(text ? JSON.parse(text).message : errorMessage(e))
    }
  }

  const totalRevenue = revenue?.reduce((sum, r) => sum + Number(r.total), 0) ?? 0
  const totalSubs = revenue?.reduce((sum, r) => sum + r.subscriptions, 0) ?? 0
  const totalMinutes = consumption?.reduce((sum, r) => sum + r.minutes, 0) ?? 0

  return (
    <main>
      <h1>Informes</h1>
      <form key={`${range.from}${range.to}`} className="cat-filters" onSubmit={submit}>
        <label>Desde <input name="from" type="date" defaultValue={range.from} required /></label>
        <label>Hasta <input name="to" type="date" defaultValue={range.to} required /></label>
        <button type="submit" className="cat-primary">Consultar</button>
      </form>
      {error && <p className="cat-error" role="alert">{error}</p>}

      <section>
        <h2>Ingresos por plan</h2>
        <p className="cat-muted">Pagos registrados en el período, agrupados por plan (PostgreSQL).</p>
        {revenue?.length === 0 && <p>No hay pagos en ese período.</p>}
        {revenue?.length > 0 && (
          <div className="cat-table">
            <table>
              <thead><tr><th>Plan</th><th>Suscripciones</th><th>Ingresos</th></tr></thead>
              <tbody>
                {revenue.map((r) => (
                  <tr key={r.planId}><td>{r.planName}</td><td>{r.subscriptions}</td><td>{money(r.total)}</td></tr>
                ))}
              </tbody>
              <tfoot>
                <tr><th>Total</th><th>{totalSubs}</th><th>{money(totalRevenue)}</th></tr>
              </tfoot>
            </table>
          </div>
        )}
        <p><button type="button" onClick={() => download('revenue')} disabled={!revenue}>Descargar CSV</button></p>
      </section>

      <section>
        <h2>Consumo por plan y tipo de contenido</h2>
        <p className="cat-muted">
          Minutos vistos según las sesiones de reproducción (MongoDB), con el plan vigente al iniciar
          cada sesión.
        </p>
        {consumption?.length === 0 && <p>No hay reproducciones en ese período.</p>}
        {consumption?.length > 0 && (
          <div className="cat-table">
            <table>
              <thead><tr><th>Plan</th><th>Tipo</th><th>Minutos vistos</th><th>Perfiles</th></tr></thead>
              <tbody>
                {consumption.map((r) => (
                  <tr key={`${r.planId}-${r.contentType}`}>
                    <td>{r.planName}</td>
                    <td>{TYPES[r.contentType] ?? r.contentType}</td>
                    <td>{r.minutes.toLocaleString('es-CO')}</td>
                    <td>{r.profiles}</td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr><th colSpan={2}>Total</th><th>{totalMinutes.toLocaleString('es-CO', { maximumFractionDigits: 1 })}</th><th /></tr>
              </tfoot>
            </table>
          </div>
        )}
        <p><button type="button" onClick={() => download('consumption')} disabled={!consumption}>Descargar CSV</button></p>
      </section>
    </main>
  )
}
