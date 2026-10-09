-- Migración V3: Catálogo SEPOMEX para persistencia y consulta de direcciones descargadas de la API

CREATE TABLE IF NOT EXISTS catalogo_sepomex (
    id                  BIGSERIAL       PRIMARY KEY,
    codigo_postal       CHAR(5)         NOT NULL,
    asentamiento        VARCHAR(150)    NOT NULL,
    tipo_asentamiento   VARCHAR(50),
    municipio           VARCHAR(100)    NOT NULL,
    estado              VARCHAR(100)    NOT NULL,
    ciudad              VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_sepomex_cp ON catalogo_sepomex (codigo_postal);
CREATE INDEX IF NOT EXISTS idx_sepomex_estado_mun ON catalogo_sepomex (estado, municipio);