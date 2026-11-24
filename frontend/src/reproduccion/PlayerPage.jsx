import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { errorMessage, getContent, getSeasons } from '../api/catalog.js'
import { pauseOnExit, sendEvent, startSession } from '../api/playback.js'
import CatalogGate from '../catalogo/CatalogGate.jsx'
import ViewerNav from './ViewerNav.jsx'
import { clock, episodeLabel, playPath } from './time.js'
import './reproduccion.css'

// Velocidad de la simulación: segundos de contenido por segundo real (para la demo)
const SPEEDS = [1, 10, 60, 300]
// Saltos del control "avanzar" (RF-29), en segundos
const SKIPS = [-30, 30, 300]

// RF-27 a RF-30: reproducción simulada. /reproducir/:contentId (las series con ?episodio=<id>).
export default function PlayerPage() {
  const { contentId } = useParams()
  const [params] = useSearchParams()
  const episodeId = params.get('episodio')
  return (
    <CatalogGate role="CLIENT">
      {(profileId) => (
        <Player key={`${contentId}-${episodeId}`} profileId={profileId} contentId={contentId} episodeId={episodeId} />
      )}
    </CatalogGate>
  )
}

function Player({ profileId, contentId, episodeId }) {
  const [session, setSession] = useState(null)      // última respuesta del backend
  const [position, setPosition] = useState(0)       // posición simulada del reproductor
  const [speed, setSpeed] = useState(10)
  const [content, setContent] = useState(null)
  const [nextEpisode, setNextEpisode] = useState(null)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const started = useRef(false)
  const busyRef = useRef(false)
  // Última sesión y posición, para los manejadores que no se vuelven a crear en cada render
  const live = useRef({ session: null, position: 0 })
  useEffect(() => {
    live.current = { session, position }
  }, [session, position])

  const open = session?.status === 'open'
  const playing = open && session.playing
  const duration = session?.durationSec ?? 0

  // Inicia la sesión una sola vez (en desarrollo, StrictMode ejecuta los efectos dos veces)
  useEffect(() => {
    if (started.current) return
    started.current = true
    startSession(profileId, contentId, episodeId)
      .then((s) => {
        setSession(s)
        setPosition(s.positionSec)
      })
      .catch((e) => setError(errorMessage(e)))
    getContent(profileId, contentId).then(setContent).catch(() => {})
  }, [profileId, contentId, episodeId])

  // Si se sale de la página o se cierra la pestaña mientras suena, se registra la pausa
  useEffect(() => {
    const exit = () => {
      const { session: s, position: p } = live.current
      if (s?.status === 'open' && s.playing) pauseOnExit(profileId, s.sessionId, Math.round(p))
    }
    window.addEventListener('pagehide', exit)
    return () => {
      window.removeEventListener('pagehide', exit)
      exit()
    }
  }, [profileId])

  const send = useCallback(async (type, extra = {}) => {
    const s = live.current.session
    if (!s || busyRef.current) return
    busyRef.current = true
    setBusy(true)
    try {
      const updated = await sendEvent(profileId, s.sessionId,
        { type, positionSec: Math.round(live.current.position), ...extra })
      setSession(updated)
      setPosition(updated.positionSec)
      setError(null)
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      busyRef.current = false
      setBusy(false)
    }
  }, [profileId])

  // Avance simulado del tiempo mientras suena
  useEffect(() => {
    if (!playing) return
    const timer = setInterval(() => setPosition((p) => Math.min(p + speed, duration)), 1000)
    return () => clearInterval(timer)
  }, [playing, speed, duration])

  // Al llegar al final, la sesión se finaliza sola
  useEffect(() => {
    if (playing && duration > 0 && position >= duration) send('finish')
  }, [playing, position, duration, send])

  // Siguiente episodio, para ofrecerlo al terminar uno
  useEffect(() => {
    if (!session?.episodeId || content?.type !== 'series') return
    getSeasons(profileId, contentId)
      .then((seasons) => {
        const all = seasons.flatMap((s) => s.episodes)
        const index = all.findIndex((e) => e.episodeId === session.episodeId)
        setNextEpisode(index >= 0 ? all[index + 1] ?? null : null)
      })
      .catch(() => {})
  }, [profileId, contentId, content?.type, session?.episodeId])

  const seek = (delta) => send('seek', { toSec: Math.max(0, Math.min(Math.round(position + delta), duration)) })
  const percent = duration ? Math.min(100, (100 * position) / duration) : 0
  const label = episodeLabel(session?.seasonNumber, session?.episodeNumber)

  return (
    <main>
      <ViewerNav />
      <p><Link to={`/catalogo/${contentId}`}>← Volver al detalle</Link></p>
      <h1>{content?.title ?? 'Reproducción'}{label && <span className="cat-muted"> · {label}</span>}</h1>

      {error && <p className="cat-error" role="alert">{error}</p>}

      {session && (
        <section className="rep-player" aria-label="Reproductor simulado">
          <div className="rep-screen">
            <div>
              <strong>{clock(position)}</strong>
              <div>{open ? (playing ? '▶ Reproduciendo' : '❚❚ En pausa') : 'Reproducción finalizada'}</div>
            </div>
          </div>

          <div>
            <div className="rep-bar"><span style={{ width: `${percent}%` }} /></div>
            <div className="rep-times"><span>{clock(position)}</span><span>{clock(duration)}</span></div>
          </div>

          {open && (
            <div className="rep-controls">
              {playing
                ? <button type="button" className="cat-primary" onClick={() => send('pause')} disabled={busy}>Pausar</button>
                : <button type="button" className="cat-primary" onClick={() => send('resume')} disabled={busy}>Reanudar</button>}
              {SKIPS.map((d) => (
                <button key={d} type="button" onClick={() => seek(d)} disabled={busy}>
                  {d < 0 ? `⟲ ${-d} s` : d >= 60 ? `⟳ ${d / 60} min` : `⟳ ${d} s`}
                </button>
              ))}
              <button type="button" onClick={() => send('finish')} disabled={busy}>Finalizar</button>
              <label>Velocidad
                <select value={speed} onChange={(e) => setSpeed(Number(e.target.value))}>
                  {SPEEDS.map((s) => <option key={s} value={s}>x{s}</option>)}
                </select>
              </label>
            </div>
          )}

          {/* Valores confirmados por el servidor en el último evento */}
          <dl className="rep-stats">
            <div><dt>Tiempo visto (sin saltos)</dt><dd>{clock(session.watchedSec)}</dd></div>
            <div><dt>Última posición guardada</dt><dd>{clock(session.positionSec)}</dd></div>
            <div><dt>Estado</dt><dd>{session.completed ? 'Completada' : open ? 'En curso' : 'Terminada'}</dd></div>
          </dl>

          {!open && (
            <div className="cat-actions">
              {nextEpisode && (
                <Link className="cat-button cat-primary" to={playPath(contentId, nextEpisode.episodeId)}>
                  Siguiente episodio: {nextEpisode.title}
                </Link>
              )}
              <Link to="/catalogo">Ir al catálogo</Link>
            </div>
          )}
        </section>
      )}
    </main>
  )
}
