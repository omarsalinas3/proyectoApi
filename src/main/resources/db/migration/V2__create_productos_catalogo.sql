CREATE TABLE IF NOT EXISTS productos_catalogo (
    id_producto             BIGINT PRIMARY KEY,
    id_servicio             INTEGER,
    id_cat_tipo_servicio    INTEGER,
    tipo_front              INTEGER,
    producto                VARCHAR(255),
    servicio                VARCHAR(255),
    precio                  NUMERIC(12, 2),
    tipo_referencia         VARCHAR(50),
    has_digito_verificador  BOOLEAN,
    show_ayuda              BOOLEAN,
    legend                  TEXT,
    fecha_actualizacion     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
