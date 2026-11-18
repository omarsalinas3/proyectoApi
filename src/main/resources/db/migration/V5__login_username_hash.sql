-- ===================================================================
-- MIGRACIÓN V5: búsqueda de usuario por hash (cifrado con IV aleatorio)
-- ===================================================================
-- El usuario pasa a cifrarse con AES-GCM e IV aleatorio, así que el texto cifrado ya no sirve para buscar ni para
-- garantizar que no se repita. Esa función la toma username_hash (HMAC-SHA256 del usuario).
--
-- La columna admite NULL solo mientras MigracionCifradoLogin llena los registros existentes al arrancar, porque el
-- cálculo necesita la llave de la aplicación y no se puede hacer en SQL.
ALTER TABLE login ADD COLUMN IF NOT EXISTS username_hash VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS uq_login_username_hash ON login(username_hash);
