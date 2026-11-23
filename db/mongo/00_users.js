// Crea el usuario de la aplicación con permisos solo sobre la base del proyecto.
// Usuario y contraseña vienen de variables de entorno (archivo .env).
const appDb = db.getSiblingDB(process.env.MONGO_INITDB_DATABASE);
appDb.createUser({
  user: process.env.MONGO_APP_USER,
  pwd: process.env.MONGO_APP_PASSWORD,
  roles: [{ role: 'readWrite', db: process.env.MONGO_INITDB_DATABASE }],
});
