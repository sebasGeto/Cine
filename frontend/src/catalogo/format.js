export const TYPES = { movie: 'Película', series: 'Serie', documentary: 'Documental' }

export const STATUSES = { draft: 'Borrador', published: 'Publicado', retired: 'Retirado' }

export const AGE_RATINGS = [0, 7, 12, 16, 18]

export const SOURCE_KINDS = { institution: 'Institución', archive: 'Archivo', publication: 'Publicación' }

export const ageLabel = (rating) => (rating === 0 ? 'Todo público' : `+${rating}`)

export function durationLabel(seconds) {
  if (seconds < 60) return `${seconds} s`
  const minutes = Math.round(seconds / 60)
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return [h > 0 && `${h} h`, m > 0 && `${m} min`].filter(Boolean).join(' ')
}
