// =====================================================================
// 02_indexes.js
// Índices de MongoDB (sección 4 del documento de la entrega).
// Cada índice responde a una consulta concreta de la aplicación.
// =====================================================================

const appDb = db.getSiblingDB(process.env.MONGO_INITDB_DATABASE);

// ---------------------------------------------------------------------
// contents
// ---------------------------------------------------------------------
// Búsqueda paginada del cliente con filtros por tipo y año (RF-21).
// Orden de campos: igualdad (status, type), orden (releaseYear), rango (ageRating).
appDb.contents.createIndex(
  { status: 1, type: 1, releaseYear: -1, ageRating: 1 },
  { name: 'ix_contents_browse' }
);

// Filtro por género. genres es un arreglo: el índice es multikey
// (una entrada por cada género del documento).
appDb.contents.createIndex(
  { status: 1, genres: 1, releaseYear: -1 },
  { name: 'ix_contents_genres' }
);

// Filtro por idioma de audio (también multikey)
appDb.contents.createIndex(
  { status: 1, audioLanguages: 1 },
  { name: 'ix_contents_audio' }
);

// Búsqueda por palabras del título. default_language "none" porque hay
// títulos en varios idiomas (no se eliminan palabras vacías ni se reducen raíces).
appDb.contents.createIndex(
  { title: 'text' },
  { name: 'tx_contents_title', default_language: 'none' }
);

// Ningún episodeId se repite entre series, y la serie de un episodio se
// encuentra directamente al iniciar su reproducción (RF-27).
// Parcial: solo indexa documentos de tipo serie.
appDb.contents.createIndex(
  { 'seasons.episodes.episodeId': 1 },
  {
    name: 'ux_contents_episode_id',
    unique: true,
    partialFilterExpression: { type: 'series' }
  }
);

// ---------------------------------------------------------------------
// myList
// ---------------------------------------------------------------------
// Impide guardar dos veces el mismo contenido en un perfil (RF-24)
appDb.myList.createIndex(
  { profileId: 1, contentId: 1 },
  { name: 'ux_mylist_profile_content', unique: true }
);

// Mostrar la lista del perfil ordenada por fecha (RF-26)
appDb.myList.createIndex(
  { profileId: 1, addedAt: -1 },
  { name: 'ix_mylist_profile_added' }
);

// ---------------------------------------------------------------------
// viewingSessions
// ---------------------------------------------------------------------
// Historial de sesiones de un perfil
appDb.viewingSessions.createIndex(
  { profileId: 1, startedAt: -1 },
  { name: 'ix_sessions_profile_started' }
);

// Informe de consumo: rango de fechas y agrupación por plan y tipo (RF-36)
appDb.viewingSessions.createIndex(
  { startedAt: 1, planId: 1, contentType: 1 },
  { name: 'ix_sessions_report' }
);

// Cerrar sesiones abandonadas (RF-32). Parcial: solo las sesiones abiertas.
appDb.viewingSessions.createIndex(
  { lastActionAt: 1 },
  { name: 'ix_sessions_open', partialFilterExpression: { status: 'open' } }
);

// ---------------------------------------------------------------------
// viewingProgress
// ---------------------------------------------------------------------
// Un solo documento de progreso por perfil y contenido
appDb.viewingProgress.createIndex(
  { profileId: 1, contentId: 1 },
  { name: 'ux_progress_profile_content', unique: true }
);

// "Continuar viendo": no completados del perfil, del más reciente al más antiguo (RF-31)
appDb.viewingProgress.createIndex(
  { profileId: 1, completed: 1, lastActivityAt: -1 },
  { name: 'ix_progress_continue' }
);

// ---------------------------------------------------------------------
// adminUsers
// ---------------------------------------------------------------------
// Únicos sin distinguir mayúsculas: collation español con strength 2
// (compara letras y acentos, ignora mayúsculas).
appDb.adminUsers.createIndex(
  { username: 1 },
  { name: 'ux_admin_username', unique: true, collation: { locale: 'es', strength: 2 } }
);
appDb.adminUsers.createIndex(
  { email: 1 },
  { name: 'ux_admin_email', unique: true, collation: { locale: 'es', strength: 2 } }
);

print('Índices creados en ' + appDb.getName());
