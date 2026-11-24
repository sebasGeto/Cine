export default function Pagination({ page, size, total, onChange }) {
  const pages = Math.max(1, Math.ceil(total / size))
  return (
    <nav className="cat-pagination" aria-label="Paginación">
      <button type="button" disabled={page <= 0} onClick={() => onChange(page - 1)}>Anterior</button>
      <span>Página {page + 1} de {pages} · {total} {total === 1 ? 'resultado' : 'resultados'}</span>
      <button type="button" disabled={page + 1 >= pages} onClick={() => onChange(page + 1)}>Siguiente</button>
    </nav>
  )
}
