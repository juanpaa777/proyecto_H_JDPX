-- Migración V4: Tabla de Pagos de Servicios e Integración con GestoPago (sendTx / confirmTx)

CREATE TABLE IF NOT EXISTS pagos_servicios (
    id                      BIGSERIAL       PRIMARY KEY,
    cliente_id              BIGINT          NOT NULL,
    cuenta_id               BIGINT          NOT NULL,
    numero_cuenta           VARCHAR(20)     NOT NULL,
    upc                     VARCHAR(15)     NOT NULL,
    id_servicio             INTEGER         NOT NULL,
    id_producto             INTEGER         NOT NULL,
    nombre_servicio         VARCHAR(150),
    nombre_producto         VARCHAR(150),
    tipo_front              INTEGER,
    bandera                 VARCHAR(25)     NOT NULL,
    telefono                VARCHAR(20)     NOT NULL,
    referencia              VARCHAR(100),
    monto_pago              NUMERIC(15,2)   NOT NULL CHECK (monto_pago > 0),
    comision                NUMERIC(15,2)   DEFAULT 0.00,
    saldo_anterior          NUMERIC(15,2)   NOT NULL,
    saldo_posterior         NUMERIC(15,2)   NOT NULL,
    hora_local              VARCHAR(15)     NOT NULL,
    unidad                  VARCHAR(25)     NOT NULL,
    random_param            VARCHAR(50)     NOT NULL,
    payload_signed          TEXT            NOT NULL,
    estatus                 VARCHAR(30)     NOT NULL,
    codigo_respuesta        VARCHAR(10),
    mensaje_respuesta       VARCHAR(500),
    id_tx_gestopago         VARCHAR(50),
    num_autorizacion        VARCHAR(60),
    pin                     VARCHAR(250),
    leyenda                 TEXT,
    xml_response            TEXT,
    fecha_creacion          TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_actualizacion     TIMESTAMP,
    CONSTRAINT fk_pagos_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE RESTRICT,
    CONSTRAINT fk_pagos_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas(id) ON DELETE RESTRICT,
    CONSTRAINT uq_pagos_upc UNIQUE (upc)
);

CREATE INDEX IF NOT EXISTS idx_pagos_cuenta_id ON pagos_servicios (cuenta_id);
CREATE INDEX IF NOT EXISTS idx_pagos_cliente_id ON pagos_servicios (cliente_id);
CREATE INDEX IF NOT EXISTS idx_pagos_estatus ON pagos_servicios (estatus);
CREATE INDEX IF NOT EXISTS idx_pagos_duplicados_servicio ON pagos_servicios (id_servicio, referencia, monto_pago, fecha_creacion);
CREATE INDEX IF NOT EXISTS idx_pagos_duplicados_recarga ON pagos_servicios (id_servicio, telefono, fecha_creacion);

