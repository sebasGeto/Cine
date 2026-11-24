import { useState } from 'react'

// Portada; si la imagen no carga se muestra el título en su lugar.
export default function Cover({ url, title }) {
  const [failedUrl, setFailedUrl] = useState(null)
  if (!url || failedUrl === url) {
    return <div className="cat-cover cat-cover-empty" aria-hidden="true">{title}</div>
  }
  return <img className="cat-cover" src={url} alt="" onError={() => setFailedUrl(url)} />
}
