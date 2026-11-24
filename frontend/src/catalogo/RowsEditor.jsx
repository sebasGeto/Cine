// Lista editable de objetos (reparto, entrevistados, fuentes, episodios).
export default function RowsEditor({ title, fields, rows, onChange }) {
  const set = (index, name, value) =>
    onChange(rows.map((row, i) => (i === index ? { ...row, [name]: value } : row)))

  const add = () => onChange([...rows, Object.fromEntries(
    fields.map((f) => [f.name, f.options ? Object.keys(f.options)[0] : '']),
  )])

  return (
    <fieldset>
      <legend>{title}</legend>
      {rows.map((row, i) => (
        <div className="cat-row" key={i}>
          {fields.map((f) => (
            <label key={f.name}>{f.label}
              {f.options ? (
                <select value={row[f.name]} onChange={(e) => set(i, f.name, e.target.value)}>
                  {Object.entries(f.options).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                </select>
              ) : (
                <input type={f.type ?? 'text'} min={f.type === 'number' ? 1 : undefined} required={!f.optional}
                  value={row[f.name] ?? ''} onChange={(e) => set(i, f.name, e.target.value)} />
              )}
            </label>
          ))}
          <button type="button" onClick={() => onChange(rows.filter((_, j) => j !== i))}>Quitar</button>
        </div>
      ))}
      <button type="button" onClick={add}>Agregar</button>
    </fieldset>
  )
}
