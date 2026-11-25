import { useEffect, useState } from 'react'
import { errorMessage } from '../api/catalog.js'
import { addToMyList, getMyList, removeFromMyList } from '../api/playback.js'

// Agregar o quitar un contenido de Mi lista desde su detalle (RF-24 y RF-25).
export default function MyListButton({ profileId, contentId }) {
  const [inList, setInList] = useState(null)   // null = todavía no se sabe
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    let active = true
    getMyList(profileId)
      .then((list) => active && setInList(list.some((i) => i.contentId === contentId)))
      .catch(() => active && setInList(false))
    return () => { active = false }
  }, [profileId, contentId])

  async function toggle() {
    setBusy(true)
    setError(null)
    try {
      if (inList) {
        await removeFromMyList(profileId, contentId)
        setInList(false)
      } else {
        await addToMyList(profileId, contentId)
        setInList(true)
      }
    } catch (e) {
      const code = e.response?.data?.code
      if (code === 'ALREADY_IN_LIST') setInList(true)
      else if (code === 'NOT_IN_MY_LIST') setInList(false)
      else setError(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <button type="button" onClick={toggle} disabled={busy || inList === null}>
        {inList ? '✓ En Mi lista' : '+ Mi lista'}
      </button>
      {error && <span className="cat-error" role="alert">{error}</span>}
    </>
  )
}
