// =====================================================================
// 03_seed.js
// Datos de prueba de MongoDB.
//
// - Los IDs de contenidos y episodios son fijos para que la demo y las
//   pruebas puedan referenciarlos.
// - accountId, profileId, planId y subscriptionId corresponden a los datos
//   de db/postgres/06_seed.sql (mismas cuentas, perfiles y suscripciones).
// - Las fechas son relativas al momento de la carga, igual que en PostgreSQL,
//   y cada sesión queda dentro de la suscripción vigente de su cuenta.
// - Los enteros se guardan con NumberInt (en mongosh, un número sin más es double).
// =====================================================================

const appDb = db.getSiblingDB(process.env.MONGO_INITDB_DATABASE);

const DAY = 24 * 60 * 60 * 1000;
const NOW = Date.now();
const ago = (days) => new Date(NOW - days * DAY);
const I = (n) => NumberInt(n);

// ObjectId fijo: prefijo + número en hexadecimal (24 caracteres en total)
const oid = (prefix, n) => ObjectId(prefix + n.toString(16).padStart(24 - prefix.length, '0'));

// ---------------------------------------------------------------------
// CONTENIDOS
// ---------------------------------------------------------------------
const C = {}; // clave -> ObjectId

function base(key, n, data) {
  C[key] = oid('6740', n);
  const doc = Object.assign({
    _id: C[key],
    subtitleLanguages: ['es', 'en'],
    countries: ['CO'],
    audioLanguages: ['es'],
    schemaVersion: I(1),
    createdAt: ago(150)
  }, data);
  doc.releaseYear = I(doc.releaseYear);
  doc.ageRating = I(doc.ageRating);
  if (doc.durationSec) doc.durationSec = I(doc.durationSec);
  if (doc.status === 'published') doc.publishedAt = ago(140);
  return doc;
}

// Episodios con IDs fijos: serie s, temporada t, episodio e
function season(s, t, description, titles, durationSec) {
  return {
    number: I(t),
    description: description,
    episodes: titles.map((title, i) => ({
      episodeId: oid('6741', s * 10000 + t * 100 + (i + 1)),
      number: I(i + 1),
      title: title,
      synopsis: 'Episodio ' + (i + 1) + ' de la temporada ' + t + '.',
      durationSec: I(durationSec)
    }))
  };
}

const contents = [
  // ---- Películas
  base('m1', 1, { type: 'movie', title: 'El último faro', synopsis: 'Un farero solitario recibe una visita que cambia su vida.',
    releaseYear: 2021, genres: ['drama'], ageRating: 16, coverUrl: 'https://img.cineconecta.test/m1.jpg', status: 'published',
    durationSec: 6600, directors: ['Lucía Mendoza'],
    cast: [{ name: 'Andrés Rivas', character: 'Tomás' }, { name: 'Marta León', character: 'Inés' }] }),
  base('m2', 2, { type: 'movie', title: 'Ruta del café', synopsis: 'Tres amigos recorren el Eje Cafetero en una camioneta vieja.',
    releaseYear: 2019, genres: ['comedia', 'aventura'], ageRating: 12, coverUrl: 'https://img.cineconecta.test/m2.jpg', status: 'published',
    durationSec: 5700, directors: ['Felipe Arango'], cast: [{ name: 'Camilo Ortiz' }, { name: 'Daniela Pardo', character: 'Sara' }] }),
  base('m3', 3, { type: 'movie', title: 'Noche en Bogotá', synopsis: 'Un taxista queda atrapado en una persecución durante una noche lluviosa.',
    releaseYear: 2023, genres: ['thriller', 'acción'], ageRating: 18, coverUrl: 'https://img.cineconecta.test/m3.jpg', status: 'published',
    audioLanguages: ['es', 'en'], durationSec: 7200, directors: ['Sergio Pineda'], cast: [{ name: 'Julián Rojas', character: 'Hernán' }] }),
  base('m4', 4, { type: 'movie', title: 'El dragón de papel', synopsis: 'Una niña construye un dragón de papel que cobra vida.',
    releaseYear: 2020, genres: ['animación', 'familiar'], ageRating: 0, coverUrl: 'https://img.cineconecta.test/m4.jpg', status: 'published',
    countries: ['CO', 'MX'], durationSec: 5400, directors: ['Ana Beltrán'], cast: [{ name: 'Valeria Soto', character: 'Mía (voz)' }] }),
  base('m5', 5, { type: 'movie', title: 'Marea alta', synopsis: 'Un grupo de pescadores enfrenta la tormenta del siglo.',
    releaseYear: 2018, genres: ['aventura'], ageRating: 7, coverUrl: 'https://img.cineconecta.test/m5.jpg', status: 'published',
    durationSec: 6000, directors: ['Ricardo Vélez'], cast: [{ name: 'Óscar Lima' }] }),
  base('m6', 6, { type: 'movie', title: 'Sombras del ayer', synopsis: 'Una mujer regresa a su pueblo natal para descubrir un secreto familiar.',
    releaseYear: 2015, genres: ['drama', 'misterio'], ageRating: 16, coverUrl: 'https://img.cineconecta.test/m6.jpg', status: 'retired',
    retiredAt: ago(10), durationSec: 6300, directors: ['Lucía Mendoza'], cast: [{ name: 'Marta León', character: 'Elvira' }] }),
  base('m7', 7, { type: 'movie', title: 'Proyecto Andrómeda', synopsis: 'Una ingeniera descubre una señal que viene de otra galaxia.',
    releaseYear: 2024, genres: ['ciencia ficción'], ageRating: 12, coverUrl: 'https://img.cineconecta.test/m7.jpg', status: 'draft',
    audioLanguages: ['es', 'en'], durationSec: 7500, directors: ['Sergio Pineda'], cast: [{ name: 'Laura Quintero', character: 'Dra. Vega' }] }),

  // ---- Series (temporadas y episodios embebidos)
  base('s1', 21, { type: 'series', title: 'Código Pacífico', synopsis: 'Una analista de ciberseguridad persigue una red de contrabando en Buenaventura.',
    releaseYear: 2022, genres: ['thriller', 'drama'], ageRating: 16, coverUrl: 'https://img.cineconecta.test/s1.jpg', status: 'published',
    audioLanguages: ['es', 'en'],
    seasons: [
      season(1, 1, 'La red', ['El puerto', 'Contraseña', 'Doble agente', 'Marea negra'], 2700),
      season(1, 2, 'La caída', ['Regreso', 'Señal débil', 'El informante', 'Fin del juego'], 2700)
    ] }),
  base('s2', 22, { type: 'series', title: 'La casa de los inventos', synopsis: 'Dos hermanos y su abuela inventan aparatos para resolver problemas del barrio.',
    releaseYear: 2021, genres: ['infantil', 'animación'], ageRating: 0, coverUrl: 'https://img.cineconecta.test/s2.jpg', status: 'published',
    seasons: [
      season(2, 1, 'Primera temporada', ['El robot regador', 'La bicicleta voladora', 'Lluvia de colores', 'El reloj perdido', 'Feria de ciencias'], 1500)
    ] }),
  base('s3', 23, { type: 'series', title: 'Detectives del Valle', synopsis: 'Una pareja de detectives resuelve casos en los pueblos del Valle del Cauca.',
    releaseYear: 2020, genres: ['policial'], ageRating: 12, coverUrl: 'https://img.cineconecta.test/s3.jpg', status: 'published',
    seasons: [
      season(3, 1, 'Casos de verano', ['La caña robada', 'El testigo', 'Último tren'], 2400),
      season(3, 2, 'Casos de invierno', ['Niebla', 'El retrato', 'La herencia'], 2400)
    ] }),

  // ---- Documentales
  base('d1', 41, { type: 'documentary', title: 'Selva viva: el Amazonas', synopsis: 'Un recorrido por la biodiversidad de la Amazonía colombiana.',
    releaseYear: 2022, genres: ['naturaleza'], ageRating: 0, coverUrl: 'https://img.cineconecta.test/d1.jpg', status: 'published',
    durationSec: 4200, directors: ['Mateo Cárdenas'], topics: ['biodiversidad', 'conservación'],
    interviewees: [{ name: 'Rosa Tikuna', description: 'Lideresa indígena' }],
    sources: [{ name: 'Instituto Sinchi', kind: 'institution', url: 'https://www.sinchi.org.co' }] }),
  base('d2', 42, { type: 'documentary', title: 'Sabor del Pacífico', synopsis: 'Cocineras tradicionales del Pacífico comparten sus recetas y su historia.',
    releaseYear: 2023, genres: ['gastronomía', 'cultura'], ageRating: 7, coverUrl: 'https://img.cineconecta.test/d2.jpg', status: 'published',
    durationSec: 3600, directors: ['Paola Mosquera'], topics: ['cocina tradicional', 'Pacífico colombiano'],
    interviewees: [{ name: 'Doña Ermelina', description: 'Cocinera tradicional' }] }),
  base('d3', 43, { type: 'documentary', title: 'Voces de la ciudad', synopsis: 'Historias de músicos callejeros en Medellín, Cali y Bogotá.',
    releaseYear: 2019, genres: ['sociedad', 'música'], ageRating: 12, coverUrl: 'https://img.cineconecta.test/d3.jpg', status: 'published',
    durationSec: 4800, directors: ['Javier Ospina'], topics: ['música urbana'],
    sources: [{ name: 'Archivo de Bogotá', kind: 'archive' }] })
];

appDb.contents.insertMany(contents);

const byKey = {};
contents.forEach((c) => { byKey[c._id.toHexString()] = c; });

// ---------------------------------------------------------------------
// SUSCRIPCIONES DE POSTGRESQL (copiadas de 06_seed.sql)
// [subscriptionId, accountId, planId, inicio hace N días]; duran 30 días
// ---------------------------------------------------------------------
const SUBS = [
  [1, 1, 1, 95], [2, 1, 2, 65], [3, 1, 2, 35], [4, 1, 2, 5],
  [5, 2, 1, 70], [6, 2, 1, 40],
  [7, 4, 2, 42], [8, 4, 3, 12],
  [9, 5, 1, 62], [10, 5, 1, 32], [11, 5, 1, 2]
];
function subAt(accountId, daysAgo) {
  const s = SUBS.find((x) => x[1] === accountId && x[3] >= daysAgo && daysAgo > x[3] - 30);
  if (!s) throw new Error('Sin suscripción vigente: cuenta ' + accountId + ', hace ' + daysAgo + ' días');
  return { subscriptionId: I(s[0]), planId: I(s[2]) };
}

// Perfil -> cuenta (06_seed.sql)
const PROFILE_ACCOUNT = { 1: 1, 2: 1, 3: 2, 4: 4, 5: 4, 6: 4, 7: 4, 8: 5 };

// ---------------------------------------------------------------------
// SESIONES DE VISUALIZACIÓN
// [perfil, contenido, [temporada, episodio] o null, hace N días, fracción vista, salto]
// fracción >= 0.9 -> completada. salto = segundos avanzados con el control "avanzar".
// ---------------------------------------------------------------------
const PLAN = [
  // Ana
  [1, 'm1', null, 80, 1.0, 0], [1, 's1', [1, 1], 70, 1.0, 0], [1, 's1', [1, 2], 50, 0.95, 0],
  [1, 'd1', null, 30, 1.0, 0], [1, 'm6', null, 15, 0.3, 0], [1, 's1', [1, 3], 3, 0.45, 0],
  [1, 'm3', null, 1, 0.4, 0],
  // Tomás (perfil infantil)
  [2, 's2', [1, 1], 60, 1.0, 0], [2, 'm4', null, 25, 1.0, 0], [2, 's2', [1, 2], 20, 1.0, 0],
  [2, 'd1', null, 10, 0.6, 0], [2, 's2', [1, 3], 2, 0.5, 0],
  // Bruno (sesiones de cuando su suscripción estaba vigente)
  [3, 'm2', null, 65, 1.0, 0], [3, 's3', [1, 1], 45, 1.0, 0], [3, 's3', [1, 2], 20, 0.7, 0],
  // Diego
  [4, 'm3', null, 40, 1.0, 0], [4, 's1', [1, 1], 35, 1.0, 0], [4, 's1', [1, 2], 30, 1.0, 0],
  [4, 's1', [1, 3], 8, 1.0, 600], [4, 's1', [1, 4], 6, 1.0, 0], [4, 'd3', null, 5, 1.0, 0],
  [4, 's1', [2, 1], 1, 0.3, 0],
  // Laura
  [5, 'm1', null, 9, 0.55, 0], [5, 's3', [1, 1], 7, 1.0, 0], [5, 's3', [1, 2], 3, 0.2, 0],
  // Sofía
  [6, 'm2', null, 11, 1.0, 0], [6, 's2', [1, 1], 4, 1.0, 0],
  // Elena
  [8, 'd2', null, 55, 1.0, 0], [8, 'm5', null, 28, 1.0, 0], [8, 's3', [1, 1], 15, 1.0, 0],
  [8, 's3', [1, 2], 1, 0.65, 0], [8, 'd1', null, 0.2, 0.25, 0]
];

function findEpisode(content, t, e) {
  const s = content.seasons.find((x) => x.number == t);
  return s.episodes.find((x) => x.number == e);
}

const sessions = PLAN.map(([profileId, key, ep, daysAgo, fraction, skip]) => {
  const content = byKey[C[key].toHexString()];
  const accountId = PROFILE_ACCOUNT[profileId];
  const episode = ep ? findEpisode(content, ep[0], ep[1]) : null;
  const durationSec = episode ? episode.durationSec.valueOf() : content.durationSec.valueOf();
  const lastPositionSec = Math.round(durationSec * fraction);
  const watchedSec = lastPositionSec - skip;            // el salto no cuenta como tiempo visto
  const completed = lastPositionSec >= 0.9 * durationSec;
  const startedAt = ago(daysAgo);
  const endedAt = new Date(startedAt.getTime() + (watchedSec + 120) * 1000);

  const events = [{ type: 'start', positionSec: I(0), at: startedAt }];
  if (skip > 0) {
    const from = Math.round(lastPositionSec / 3);
    events.push({ type: 'seek', fromSec: I(from), toSec: I(from + skip), at: new Date(startedAt.getTime() + from * 1000) });
  }
  events.push({ type: completed ? 'finish' : 'pause', positionSec: I(lastPositionSec), at: endedAt });

  const doc = Object.assign({
    accountId: I(accountId),
    profileId: I(profileId),
    contentId: content._id,
    contentType: content.type,
    durationSec: I(durationSec),
    startedAt: startedAt,
    endedAt: endedAt,
    status: 'closed',
    watchedSec: I(watchedSec),
    lastPositionSec: I(lastPositionSec),
    completed: completed,
    lastActionAt: endedAt,
    events: events
  }, subAt(accountId, daysAgo));
  if (episode) {
    doc.episodeId = episode.episodeId;
    doc.seasonNumber = I(ep[0]);
    doc.episodeNumber = I(ep[1]);
  }
  return doc;
});

appDb.viewingSessions.insertMany(sessions);

// ---------------------------------------------------------------------
// PROGRESO ("Continuar viendo"), calculado a partir de las sesiones
// para que ambas colecciones sean coherentes.
// ---------------------------------------------------------------------
const groups = {};
sessions
  .slice()
  .sort((a, b) => a.startedAt - b.startedAt)
  .forEach((s) => {
    const k = s.profileId + '|' + s.contentId.toHexString();
    (groups[k] = groups[k] || []).push(s);
  });

const progress = Object.values(groups).map((list) => {
  const last = list[list.length - 1];
  const content = byKey[last.contentId.toHexString()];
  const doc = {
    accountId: last.accountId,
    profileId: last.profileId,
    contentId: last.contentId,
    contentType: last.contentType,
    lastActivityAt: last.endedAt
  };

  if (content.type !== 'series') {
    doc.current = { positionSec: last.lastPositionSec, durationSec: last.durationSec };
    doc.completed = last.completed;
    return doc;
  }

  // Series: progreso por episodio y episodio en curso
  const perEpisode = {};
  list.forEach((s) => {
    perEpisode[s.episodeId.toHexString()] = {
      episodeId: s.episodeId, positionSec: s.lastPositionSec, completed: s.completed, updatedAt: s.endedAt
    };
  });
  doc.episodes = Object.values(perEpisode);

  const all = [];
  content.seasons.forEach((t) => t.episodes.forEach((e) => all.push({ t: t.number, e: e })));
  let idx = all.findIndex((x) => x.e.episodeId.equals(last.episodeId));
  let position = last.lastPositionSec;
  doc.completed = false;
  if (last.completed) {
    if (idx === all.length - 1) {
      doc.completed = true;              // terminó el último episodio
    } else {
      idx += 1;                          // pasa al siguiente episodio, desde el inicio
      position = I(0);
    }
  }
  const cur = all[idx];
  doc.current = {
    positionSec: position,
    durationSec: cur.e.durationSec,
    episodeId: cur.e.episodeId,
    seasonNumber: cur.t,
    episodeNumber: cur.e.number
  };
  return doc;
});

appDb.viewingProgress.insertMany(progress);

// ---------------------------------------------------------------------
// MI LISTA (incluye un contenido retirado para mostrar cómo se marca)
// ---------------------------------------------------------------------
const myList = [
  [1, 'm6', 20], [1, 's1', 72], [1, 'd2', 4],
  [2, 's2', 61], [2, 'd1', 12],
  [4, 'm1', 9], [4, 'd3', 6],
  [8, 's1', 3]
].map(([profileId, key, daysAgo]) => ({
  accountId: I(PROFILE_ACCOUNT[profileId]),
  profileId: I(profileId),
  contentId: C[key],
  addedAt: ago(daysAgo)
}));

appDb.myList.insertMany(myList);

// ---------------------------------------------------------------------
// ADMINISTRADOR INICIAL (contraseña de prueba en el README; aquí solo el hash BCrypt)
// ---------------------------------------------------------------------
appDb.adminUsers.insertOne({
  username: 'admin',
  email: 'admin@cineconecta.com',
  fullName: 'Administrador CineConecta',
  passwordHash: '$2a$10$biLFCJcnxBFbhJLxUFWb6OYxz93JzqiBqlrc8eT9rACfhLNcDUnve',
  role: 'admin',
  isActive: true,
  createdAt: ago(150)
});

print('Carga completa: ' + contents.length + ' contenidos, ' + sessions.length + ' sesiones, ' +
      progress.length + ' progresos, ' + myList.length + ' elementos en Mi lista, 1 administrador');
