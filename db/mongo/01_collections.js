// =====================================================================
// 01_collections.js
// Colecciones de MongoDB con validación $jsonSchema
// (sección 4 del documento de la entrega).
//
// validationLevel "strict" + validationAction "error": todo documento que
// se inserte o actualice debe cumplir el esquema, o MongoDB lo rechaza.
// Los campos no listados se permiten, para que el catálogo pueda evolucionar.
// Tiempos de reproducción en segundos. accountId, profileId, planId y
// subscriptionId son referencias a PostgreSQL (enteros).
// =====================================================================

const appDb = db.getSiblingDB(process.env.MONGO_INITDB_DATABASE);

// ---------------------------------------------------------------------
// contents: catálogo polimórfico (película, serie, documental)
// RI-06 a RI-09, RF-14 a RF-23
// ---------------------------------------------------------------------
appDb.createCollection('contents', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['type', 'title', 'synopsis', 'releaseYear', 'genres', 'countries',
                 'audioLanguages', 'subtitleLanguages', 'ageRating', 'coverUrl',
                 'status', 'schemaVersion', 'createdAt'],
      properties: {
        type:              { enum: ['movie', 'series', 'documentary'] },
        title:             { bsonType: 'string', minLength: 1 },
        synopsis:          { bsonType: 'string', minLength: 1 },
        releaseYear:       { bsonType: 'number', minimum: 1888, maximum: 2100 },
        genres:            { bsonType: 'array', minItems: 1, uniqueItems: true, items: { bsonType: 'string' } },
        countries:         { bsonType: 'array', minItems: 1, items: { bsonType: 'string' } },
        audioLanguages:    { bsonType: 'array', minItems: 1, items: { bsonType: 'string' } },
        subtitleLanguages: { bsonType: 'array', items: { bsonType: 'string' } },
        ageRating:         { enum: [0, 7, 12, 16, 18] },
        coverUrl:          { bsonType: 'string', minLength: 1 },
        status:            { enum: ['draft', 'published', 'retired'] },
        publishedAt:       { bsonType: 'date' },
        retiredAt:         { bsonType: 'date' },
        schemaVersion:     { bsonType: 'number', minimum: 1 },
        createdAt:         { bsonType: 'date' },
        updatedAt:         { bsonType: 'date' }
      },
      // Exactamente una de estas tres variantes según el tipo
      oneOf: [
        // Película
        {
          required: ['durationSec', 'directors', 'cast'],
          properties: {
            type:        { enum: ['movie'] },
            durationSec: { bsonType: 'number', minimum: 1 },
            directors:   { bsonType: 'array', minItems: 1, items: { bsonType: 'string' } },
            cast: {
              bsonType: 'array',
              items: {
                bsonType: 'object',
                required: ['name'],
                properties: {
                  name:      { bsonType: 'string' },
                  character: { bsonType: 'string' }
                }
              }
            }
          }
        },
        // Documental
        {
          required: ['durationSec', 'directors', 'topics'],
          properties: {
            type:        { enum: ['documentary'] },
            durationSec: { bsonType: 'number', minimum: 1 },
            directors:   { bsonType: 'array', minItems: 1, items: { bsonType: 'string' } },
            topics:      { bsonType: 'array', minItems: 1, items: { bsonType: 'string' } },
            interviewees: {
              bsonType: 'array',
              items: { bsonType: 'object', required: ['name'] }
            },
            sources: {
              bsonType: 'array',
              items: {
                bsonType: 'object',
                required: ['name', 'kind'],
                properties: { kind: { enum: ['institution', 'archive', 'publication'] } }
              }
            }
          }
        },
        // Serie: temporadas y episodios embebidos
        {
          required: ['seasons'],
          properties: {
            type: { enum: ['series'] },
            seasons: {
              bsonType: 'array',
              minItems: 1,
              items: {
                bsonType: 'object',
                required: ['number', 'description', 'episodes'],
                properties: {
                  number:      { bsonType: 'number', minimum: 1 },
                  description: { bsonType: 'string' },
                  episodes: {
                    bsonType: 'array',
                    items: {
                      bsonType: 'object',
                      required: ['episodeId', 'number', 'title', 'synopsis', 'durationSec'],
                      properties: {
                        episodeId:   { bsonType: 'objectId' },
                        number:      { bsonType: 'number', minimum: 1 },
                        title:       { bsonType: 'string' },
                        synopsis:    { bsonType: 'string' },
                        durationSec: { bsonType: 'number', minimum: 1 }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      ]
    }
  },
  validationLevel: 'strict',
  validationAction: 'error'
});

// ---------------------------------------------------------------------
// myList: un documento por par perfil-contenido (RI-10, RF-24 a RF-26)
// ---------------------------------------------------------------------
appDb.createCollection('myList', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['accountId', 'profileId', 'contentId', 'addedAt'],
      properties: {
        accountId: { bsonType: ['int', 'long'] },
        profileId: { bsonType: ['int', 'long'] },
        contentId: { bsonType: 'objectId' },
        addedAt:   { bsonType: 'date' }
      }
    }
  },
  validationLevel: 'strict',
  validationAction: 'error'
});

// ---------------------------------------------------------------------
// viewingSessions: una sesión de reproducción (RI-11, RF-27 a RF-30, RF-32, RF-36)
// Historial que solo crece; datos copiados del momento de inicio.
// ---------------------------------------------------------------------
appDb.createCollection('viewingSessions', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['accountId', 'profileId', 'subscriptionId', 'planId', 'contentId',
                 'contentType', 'durationSec', 'startedAt', 'status', 'watchedSec',
                 'lastPositionSec', 'completed', 'lastActionAt'],
      properties: {
        accountId:       { bsonType: ['int', 'long'] },
        profileId:       { bsonType: ['int', 'long'] },
        subscriptionId:  { bsonType: ['int', 'long'] },
        planId:          { bsonType: ['int', 'long'] },
        contentId:       { bsonType: 'objectId' },
        contentType:     { enum: ['movie', 'series', 'documentary'] },
        episodeId:       { bsonType: 'objectId' },
        seasonNumber:    { bsonType: 'number', minimum: 1 },
        episodeNumber:   { bsonType: 'number', minimum: 1 },
        durationSec:     { bsonType: 'number', minimum: 1 },
        startedAt:       { bsonType: 'date' },
        endedAt:         { bsonType: ['date', 'null'] },
        status:          { enum: ['open', 'closed', 'abandoned'] },
        watchedSec:      { bsonType: 'number', minimum: 0 },
        lastPositionSec: { bsonType: 'number', minimum: 0 },
        completed:       { bsonType: 'bool' },
        lastActionAt:    { bsonType: 'date' },
        playState: {
          bsonType: 'object',
          properties: {
            playing:         { bsonType: 'bool' },
            segmentStartSec: { bsonType: 'number', minimum: 0 }
          }
        },
        events: {
          bsonType: 'array',
          maxItems: 200,
          items: {
            bsonType: 'object',
            required: ['type', 'at'],
            properties: {
              type: { enum: ['start', 'pause', 'resume', 'seek', 'finish'] },
              at:   { bsonType: 'date' }
            }
          }
        }
      }
    }
  },
  validationLevel: 'strict',
  validationAction: 'error'
});

// ---------------------------------------------------------------------
// viewingProgress: estado actual por perfil y contenido (RI-12, RF-30, RF-31)
// ---------------------------------------------------------------------
appDb.createCollection('viewingProgress', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['accountId', 'profileId', 'contentId', 'contentType', 'current',
                 'completed', 'lastActivityAt'],
      properties: {
        accountId:   { bsonType: ['int', 'long'] },
        profileId:   { bsonType: ['int', 'long'] },
        contentId:   { bsonType: 'objectId' },
        contentType: { enum: ['movie', 'series', 'documentary'] },
        current: {
          bsonType: 'object',
          required: ['positionSec', 'durationSec'],
          properties: {
            positionSec:   { bsonType: 'number', minimum: 0 },
            durationSec:   { bsonType: 'number', minimum: 1 },
            episodeId:     { bsonType: 'objectId' },
            seasonNumber:  { bsonType: 'number', minimum: 1 },
            episodeNumber: { bsonType: 'number', minimum: 1 }
          }
        },
        completed:      { bsonType: 'bool' },
        lastActivityAt: { bsonType: 'date' },
        episodes: {
          bsonType: 'array',
          items: {
            bsonType: 'object',
            required: ['episodeId', 'positionSec', 'completed', 'updatedAt'],
            properties: {
              episodeId:   { bsonType: 'objectId' },
              positionSec: { bsonType: 'number', minimum: 0 },
              completed:   { bsonType: 'bool' },
              updatedAt:   { bsonType: 'date' }
            }
          }
        }
      }
    }
  },
  validationLevel: 'strict',
  validationAction: 'error'
});

// ---------------------------------------------------------------------
// adminUsers: administradores, independientes de los clientes (RI-13, RF-33, RF-34)
// ---------------------------------------------------------------------
appDb.createCollection('adminUsers', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['username', 'email', 'fullName', 'passwordHash', 'role', 'isActive', 'createdAt'],
      properties: {
        username:     { bsonType: 'string', minLength: 3 },
        email:        { bsonType: 'string', pattern: '^[^@\\s]+@[^@\\s]+$' },
        fullName:     { bsonType: 'string', minLength: 1 },
        passwordHash: { bsonType: 'string', minLength: 20 },
        role:         { enum: ['admin'] },
        isActive:     { bsonType: 'bool' },
        createdAt:    { bsonType: 'date' },
        lastLoginAt:  { bsonType: 'date' }
      }
    }
  },
  validationLevel: 'strict',
  validationAction: 'error'
});

print('Colecciones creadas: ' + appDb.getCollectionNames().join(', '));
