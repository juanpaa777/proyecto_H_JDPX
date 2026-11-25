-- Migración V5: Control Centralizado de Sesiones Activas (Sesión Única) y Autenticación Multifactor (2FA)

-- 1. Tabla de Sesiones Activas respaldada en servidor
CREATE TABLE IF NOT EXISTS sesiones_activas (
    id                      BIGSERIAL       PRIMARY KEY,
    usuario_id              BIGINT          NOT NULL,
    cliente_id              BIGINT          NOT NULL,
    token_jti               VARCHAR(64)     NOT NULL,
    plataforma              VARCHAR(30)     NOT NULL,
    dispositivo_info        VARCHAR(150)    NOT NULL,
    activa                  BOOLEAN         NOT NULL DEFAULT TRUE,
    motivo_cierre           VARCHAR(100),
    fecha_inicio            TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_expiracion        TIMESTAMP       NOT NULL,
    fecha_ultimo_acceso     TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_sesiones_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT uq_sesiones_token_jti UNIQUE (token_jti)
);

CREATE INDEX IF NOT EXISTS idx_sesiones_usuario_activa ON sesiones_activas (usuario_id, activa);
CREATE INDEX IF NOT EXISTS idx_sesiones_token_jti ON sesiones_activas (token_jti);

-- 2. Tabla de Desafíos y Códigos Temporales 2FA (OTP)
CREATE TABLE IF NOT EXISTS codigos_2fa (
    id                      BIGSERIAL       PRIMARY KEY,
    usuario_id              BIGINT          NOT NULL,
    desafio_id              VARCHAR(64)     NOT NULL,
    codigo_hash             VARCHAR(255)    NOT NULL,
    plataforma              VARCHAR(30)     NOT NULL,
    dispositivo_info        VARCHAR(150)    NOT NULL,
    intentos_fallidos       INTEGER         NOT NULL DEFAULT 0,
    usado                   BOOLEAN         NOT NULL DEFAULT FALSE,
    forzar_cierre_previa    BOOLEAN         NOT NULL DEFAULT FALSE,
    fecha_creacion          TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_expiracion        TIMESTAMP       NOT NULL,
    CONSTRAINT fk_codigos2fa_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT uq_codigos2fa_desafio UNIQUE (desafio_id)
);

CREATE INDEX IF NOT EXISTS idx_codigos2fa_desafio ON codigos_2fa (desafio_id);
CREATE INDEX IF NOT EXISTS idx_codigos2fa_usuario ON codigos_2fa (usuario_id, fecha_creacion);

