-- Migración V2: Tablas para Onboarding de Clientes Personas Físicas

-- 1. Tabla Clientes
CREATE TABLE IF NOT EXISTS clientes (
    id                      BIGSERIAL       PRIMARY KEY,
    nombre                  VARCHAR(50)     NOT NULL,
    segundo_nombre          VARCHAR(50),
    apellido_paterno        VARCHAR(50)     NOT NULL,
    apellido_materno        VARCHAR(50)     NOT NULL,
    fecha_nacimiento        DATE            NOT NULL,
    curp                    CHAR(18)        NOT NULL,
    rfc                     VARCHAR(13)     NOT NULL,
    sexo                    VARCHAR(15)     NOT NULL,
    nacionalidad            VARCHAR(50)     NOT NULL,
    estado_civil            VARCHAR(20)     NOT NULL,
    correo                  VARCHAR(100)    NOT NULL,
    telefono_movil          VARCHAR(10)     NOT NULL,
    telefono_alternativo    VARCHAR(10),
    ocupacion               VARCHAR(80)     NOT NULL,
    empresa                 VARCHAR(100)    NOT NULL,
    ingreso_mensual         NUMERIC(12,2)   NOT NULL CHECK (ingreso_mensual > 0),
    activo                  BOOLEAN         NOT NULL DEFAULT TRUE,
    fecha_creacion          TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_actualizacion     TIMESTAMP,
    CONSTRAINT uq_clientes_curp UNIQUE (curp),
    CONSTRAINT uq_clientes_rfc UNIQUE (rfc),
    CONSTRAINT uq_clientes_correo UNIQUE (correo)
);

CREATE INDEX IF NOT EXISTS idx_clientes_nombre ON clientes (nombre, apellido_paterno, apellido_materno);
CREATE INDEX IF NOT EXISTS idx_clientes_activo ON clientes (activo);
CREATE INDEX IF NOT EXISTS idx_clientes_fecha_creacion ON clientes (fecha_creacion);

-- 2. Tabla Domicilios (1 a 1 con Clientes)
CREATE TABLE IF NOT EXISTS domicilios (
    id                      BIGSERIAL       PRIMARY KEY,
    cliente_id              BIGINT          NOT NULL,
    calle                   VARCHAR(100)    NOT NULL,
    numero_exterior         VARCHAR(20)     NOT NULL,
    numero_interior         VARCHAR(20),
    colonia                 VARCHAR(80)     NOT NULL,
    municipio               VARCHAR(80)     NOT NULL,
    estado                  VARCHAR(50)     NOT NULL,
    codigo_postal           CHAR(5)         NOT NULL,
    pais                    VARCHAR(50)     NOT NULL DEFAULT 'México',
    CONSTRAINT fk_domicilios_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE,
    CONSTRAINT uq_domicilios_cliente UNIQUE (cliente_id)
);

CREATE INDEX IF NOT EXISTS idx_domicilios_cliente_id ON domicilios (cliente_id);
CREATE INDEX IF NOT EXISTS idx_domicilios_codigo_postal ON domicilios (codigo_postal);

-- 3. Tabla Cuentas (1 a N con Clientes)
CREATE TABLE IF NOT EXISTS cuentas (
    id                      BIGSERIAL       PRIMARY KEY,
    cliente_id              BIGINT          NOT NULL,
    numero_cuenta           VARCHAR(20)     NOT NULL,
    saldo                   NUMERIC(15,2)   NOT NULL DEFAULT 0.00 CHECK (saldo >= 0),
    estatus                 VARCHAR(20)     NOT NULL DEFAULT 'ACTIVA',
    fecha_creacion          TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_actualizacion     TIMESTAMP,
    CONSTRAINT fk_cuentas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE,
    CONSTRAINT uq_cuentas_numero UNIQUE (numero_cuenta)
);

CREATE INDEX IF NOT EXISTS idx_cuentas_cliente_id ON cuentas (cliente_id);
CREATE INDEX IF NOT EXISTS idx_cuentas_estatus ON cuentas (estatus);

-- 4. Tabla Usuarios (1 a 1 con Clientes para Autenticación)
CREATE TABLE IF NOT EXISTS usuarios (
    id                      BIGSERIAL       PRIMARY KEY,
    cliente_id              BIGINT          NOT NULL,
    correo                  VARCHAR(100)    NOT NULL,
    password                VARCHAR(255)    NOT NULL,
    activo                  BOOLEAN         NOT NULL DEFAULT TRUE,
    fecha_creacion          TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_actualizacion     TIMESTAMP,
    CONSTRAINT fk_usuarios_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE,
    CONSTRAINT uq_usuarios_cliente UNIQUE (cliente_id),
    CONSTRAINT uq_usuarios_correo UNIQUE (correo)
);

CREATE INDEX IF NOT EXISTS idx_usuarios_correo ON usuarios (correo);
CREATE INDEX IF NOT EXISTS idx_usuarios_cliente_id ON usuarios (cliente_id);
