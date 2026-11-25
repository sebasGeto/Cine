// 75 → "1:15"; 3725 → "1:02:05"
export function clock(seconds) {
  const s = Math.max(0, Math.round(seconds ?? 0))
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = String(s % 60).padStart(2, '0')
  return h > 0 ? `${h}:${String(m).padStart(2, '0')}:${sec}` : `${m}:${sec}`
}

export const episodeLabel = (season, episode) => (season ? `T${season} · E${episode}` : null)

export const playPath = (contentId, episodeId) =>
  `/reproducir/${contentId}${episodeId ? `?episodio=${episodeId}` : ''}`
