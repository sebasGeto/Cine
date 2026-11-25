// Formatos de dinero y fechas. El backend guarda y envía las fechas en UTC sin zona
// ("2026-12-19T16:32:00"); se les agrega la Z para mostrarlas en la hora local.

const COP = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })

export const money = (value) => COP.format(value)

const utc = (value) => new Date(/[zZ]|[+-]\d\d:?\d\d$/.test(value) ? value : `${value}Z`)

export const dateTime = (value) =>
  value ? utc(value).toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' }) : '—'

export const date = (value) => (value ? utc(value).toLocaleDateString('es-CO', { dateStyle: 'medium' }) : '—')
