-- =====================================================================
-- CR-01 — Cambio de alcance (reuniones del 17 y 18/08/2026)
-- Decisiones D-17 a D-21. SQL definido en spec.md v2.0 §1.0.
-- V1 NO se modifica: estos objetos se agregan sobre el esquema validado.
-- Sin migraciones destructivas: google_subject_id y token_verificacion
-- quedan sin uso pero no se eliminan.
--
-- NOTA sobre el esquema: spec.md §1.0 califica cada objeto como
-- "labgarcias.x", pero V1 (01_labgarcias_schema.sql) trae comentadas sus
-- líneas CREATE SCHEMA y SET search_path, así que crea todo en el esquema
-- por defecto de la conexión (public). Calificar acá rompería la migración
-- con "schema labgarcias does not exist". Las sentencias van sin calificar,
-- igual que V1, para que ambas caigan siempre en el mismo esquema.
-- =====================================================================

-- D-17/D-18: teléfono y cambio obligatorio de contraseña
ALTER TABLE usuario ADD COLUMN telefono VARCHAR(30);
ALTER TABLE usuario ADD COLUMN debe_cambiar_password BOOLEAN NOT NULL DEFAULT FALSE;

-- D-17: solicitudes de acceso
CREATE TABLE solicitud_acceso (
    id              BIGSERIAL    PRIMARY KEY,
    nombre_completo VARCHAR(150) NOT NULL,
    correo          VARCHAR(255) NOT NULL,
    direccion       VARCHAR(255) NOT NULL,
    telefono        VARCHAR(30)  NOT NULL,
    estado          VARCHAR(15)  NOT NULL DEFAULT 'PENDIENTE',
    fecha_creacion  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_resolucion TIMESTAMPTZ,
    CONSTRAINT chk_solicitud_estado CHECK (estado IN ('PENDIENTE','APROBADA','RECHAZADA'))
);

-- D-20: WhatsApp como canal
ALTER TABLE notificacion_envio DROP CONSTRAINT chk_envio_canal;
ALTER TABLE notificacion_envio ADD CONSTRAINT chk_envio_canal
    CHECK (canal IN ('APP','CORREO','TELEGRAM','WHATSAPP'));

-- Nuevos eventos
ALTER TABLE notificacion DROP CONSTRAINT chk_notificacion_evento;
ALTER TABLE notificacion ADD CONSTRAINT chk_notificacion_evento
    CHECK (tipo_evento IN ('CUENTA_CREADA','NUEVA_ORDEN','ORDEN_URGENTE','CAMBIO_ESTADO',
                           'SOLICITUD_ACCESO','CREDENCIALES_CREADAS'));

-- P-18: canal WhatsApp configurable (estructura para el futuro; D-21)
ALTER TABLE configuracion_notificacion
    ADD COLUMN canal_whatsapp_activo BOOLEAN NOT NULL DEFAULT FALSE;

-- D-21: vinculación de Telegram por usuario (el bot no puede iniciar
-- conversaciones: cada usuario se vincula una vez y se captura su chat_id)
ALTER TABLE usuario ADD COLUMN telegram_chat_id VARCHAR(100);
ALTER TABLE usuario ADD COLUMN telegram_vinculado BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE telegram_token_vinculacion (
    id             BIGSERIAL    PRIMARY KEY,
    usuario_id     BIGINT       NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    token          VARCHAR(64)  NOT NULL UNIQUE,
    fecha_emision  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_uso      TIMESTAMPTZ
);
