-- =====================================================================
-- LAB. GARCIA'S CONNECT — ESQUEMA DE BASE DE DATOS
-- Fase 2 — PostgreSQL 16
-- Basado exclusivamente en: "Fase1 Analisis LabGarciasConnect v2.1"
-- Cada tabla referencia la regla de negocio (RN) o caso de uso (CU) que la origina.
-- D-16: modelo monocliente. Este esquema corresponde a UNA instalacion de UN
-- laboratorio; no contiene columnas de tenant porque el sistema no es multi-tenant.
-- NO contiene estructuras para requerimientos pendientes (P-02, P-08, P-10, P-11,
-- P-12, P-13, P-14): esos puntos quedan explicitamente fuera hasta su confirmacion.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 0. ESQUEMA Y UTILIDADES
-- ---------------------------------------------------------------------
-- CREATE SCHEMA IF NOT EXISTS labgarcias;
-- SET search_path TO labgarcias, public;

-- Trigger generico de auditoria de modificacion
CREATE OR REPLACE FUNCTION fn_set_fecha_actualizacion()
RETURNS TRIGGER AS $$
BEGIN
    NEW.fecha_actualizacion = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- =====================================================================
-- 1. SEGURIDAD Y USUARIOS
-- =====================================================================

-- ---------------------------------------------------------------------
-- rol — RN-14: SuperAdmin, Administrador, Odontologo
-- ---------------------------------------------------------------------
CREATE TABLE rol (
    id              SMALLSERIAL   PRIMARY KEY,
    codigo          VARCHAR(20)   NOT NULL UNIQUE,
    nombre          VARCHAR(50)   NOT NULL,
    descripcion     TEXT,
    CONSTRAINT chk_rol_codigo CHECK (codigo IN ('SUPERADMIN', 'ADMIN', 'ODONTOLOGO'))
);
COMMENT ON TABLE rol IS 'RN-14: roles del sistema. Lista cerrada de tres roles.';

-- ---------------------------------------------------------------------
-- usuario — RN-14, RN-15, RN-16, CU-18, CU-19
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
    id                   BIGSERIAL     PRIMARY KEY,
    rol_id               SMALLINT      NOT NULL REFERENCES rol(id),
    nombre_completo      VARCHAR(150)  NOT NULL,
    correo               VARCHAR(255)  NOT NULL,
    nombre_usuario       VARCHAR(60)   NOT NULL,
    password_hash        VARCHAR(255),
    direccion            VARCHAR(255),
    proveedor_auth       VARCHAR(10)   NOT NULL DEFAULT 'LOCAL',
    google_subject_id    VARCHAR(255)  UNIQUE,
    estado_cuenta        VARCHAR(25)   NOT NULL DEFAULT 'PENDIENTE_VERIFICACION',
    correo_verificado    BOOLEAN       NOT NULL DEFAULT FALSE,
    fecha_creacion       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    fecha_actualizacion  TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT uq_usuario_correo          UNIQUE (correo),
    CONSTRAINT uq_usuario_nombre_usuario  UNIQUE (nombre_usuario),
    CONSTRAINT chk_usuario_proveedor      CHECK (proveedor_auth IN ('LOCAL', 'GOOGLE')),
    CONSTRAINT chk_usuario_estado         CHECK (estado_cuenta IN ('PENDIENTE_VERIFICACION', 'ACTIVA', 'INACTIVA')),
    -- RN-16: si el registro es local debe existir contrasena; si es Google, el identificador del proveedor
    CONSTRAINT chk_usuario_credencial     CHECK (
        (proveedor_auth = 'LOCAL'  AND password_hash IS NOT NULL)
     OR (proveedor_auth = 'GOOGLE' AND google_subject_id IS NOT NULL)
    )
);
COMMENT ON TABLE  usuario IS 'RN-14/RN-16: usuarios de los tres roles. Datos de auto-registro del odontologo (CU-18).';
COMMENT ON COLUMN usuario.password_hash IS 'RN-15: la politica de complejidad (min 9 caracteres, mayus/minus/numeros/especiales) se valida en la capa de aplicacion; aqui solo se almacena el hash.';
COMMENT ON COLUMN usuario.estado_cuenta IS 'CU-18/CU-19: la cuenta nace PENDIENTE_VERIFICACION y pasa a ACTIVA tras confirmar el correo.';
COMMENT ON COLUMN usuario.direccion IS 'RN-16: requerido en el auto-registro del odontologo; opcional para ADMIN/SUPERADMIN.';

CREATE INDEX idx_usuario_rol    ON usuario(rol_id);
CREATE INDEX idx_usuario_correo ON usuario(lower(correo));

CREATE TRIGGER trg_usuario_actualizacion
    BEFORE UPDATE ON usuario
    FOR EACH ROW EXECUTE FUNCTION fn_set_fecha_actualizacion();

-- ---------------------------------------------------------------------
-- token_verificacion — CU-19, D-02: enlace de un solo uso con expiracion 24 h
-- ---------------------------------------------------------------------
CREATE TABLE token_verificacion (
    id                BIGSERIAL     PRIMARY KEY,
    usuario_id        BIGINT        NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    token             VARCHAR(255)  NOT NULL UNIQUE,
    fecha_emision     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    fecha_expiracion  TIMESTAMPTZ   NOT NULL,
    fecha_uso         TIMESTAMPTZ,

    CONSTRAINT chk_token_expiracion CHECK (fecha_expiracion > fecha_emision)
);
COMMENT ON TABLE  token_verificacion IS 'CU-19/D-02: token de un solo uso con expiracion (24 h) para confirmar la cuenta por correo.';
COMMENT ON COLUMN token_verificacion.fecha_uso IS 'D-02: NULL = no utilizado. Al confirmarse se sella la fecha, garantizando el uso unico.';

CREATE INDEX idx_token_usuario ON token_verificacion(usuario_id);


-- =====================================================================
-- 2. CATALOGOS ADMINISTRABLES
-- =====================================================================

-- ---------------------------------------------------------------------
-- estado — RN-04, CU-22: tabla administrable por ADMIN y SUPERADMIN
-- ---------------------------------------------------------------------
CREATE TABLE estado (
    id                   SMALLSERIAL   PRIMARY KEY,
    codigo               VARCHAR(30)   NOT NULL UNIQUE,
    nombre               VARCHAR(50)   NOT NULL,
    descripcion          TEXT,
    orden_secuencia      SMALLINT,
    es_terminal          BOOLEAN       NOT NULL DEFAULT FALSE,
    es_productivo        BOOLEAN       NOT NULL DEFAULT TRUE,
    activo               BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_creacion       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    fecha_actualizacion  TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT uq_estado_secuencia CHECK (orden_secuencia IS NULL OR orden_secuencia > 0)
);
COMMENT ON TABLE  estado IS 'RN-04/CU-22: catalogo de estados administrable.';
COMMENT ON COLUMN estado.orden_secuencia IS 'RN-04: posicion en el flujo productivo lineal. NULL para estados fuera del flujo (CANCELADO).';
COMMENT ON COLUMN estado.es_productivo IS 'FALSE para CANCELADO, que es terminal y ajeno al flujo de produccion.';

CREATE TRIGGER trg_estado_actualizacion
    BEFORE UPDATE ON estado
    FOR EACH ROW EXECUTE FUNCTION fn_set_fecha_actualizacion();

-- ---------------------------------------------------------------------
-- tipo_trabajo — RN-12, RN-18, CU-16, Anexo A
-- Estructura minima exigida por la clienta: (id, trabajo, cantidad dias estimados)
-- ---------------------------------------------------------------------
CREATE TABLE tipo_trabajo (
    id                   SERIAL         PRIMARY KEY,
    nombre               VARCHAR(150)   NOT NULL UNIQUE,
    dias_estimados       SMALLINT       NOT NULL,
    precio               NUMERIC(10,2)  NOT NULL,
    activo               BOOLEAN        NOT NULL DEFAULT TRUE,
    fecha_creacion       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    fecha_actualizacion  TIMESTAMPTZ    NOT NULL DEFAULT now(),

    -- RN-12/CU-16 A2: minimo 7 dias habiles
    CONSTRAINT chk_tipo_trabajo_dias   CHECK (dias_estimados >= 7),
    -- RN-21 (D-14): el precio del trabajo no puede ser menor a 250
    CONSTRAINT chk_tipo_trabajo_precio CHECK (precio >= 250)
);
COMMENT ON TABLE  tipo_trabajo IS 'RN-12/CU-16: catalogo administrable de tipos de trabajo. Carga inicial: 45 tipos del Anexo A.';
COMMENT ON COLUMN tipo_trabajo.dias_estimados IS 'RN-12/RN-18: dias habiles estimados de confeccion. Minimo 7 (restriccion de negocio).';
COMMENT ON COLUMN tipo_trabajo.precio IS 'RN-21 (D-14): costo del trabajo. Minimo 250. Base de calculo del recargo por urgencia y del cargo por cancelacion. Valores reales PENDIENTES de la reunion con la clienta (P-15).';
COMMENT ON COLUMN tipo_trabajo.activo IS 'CU-16 A1: la desactivacion impide seleccionarlo en nuevas ordenes; las ordenes existentes conservan la referencia.';

CREATE INDEX idx_tipo_trabajo_activo ON tipo_trabajo(activo);

CREATE TRIGGER trg_tipo_trabajo_actualizacion
    BEFORE UPDATE ON tipo_trabajo
    FOR EACH ROW EXECUTE FUNCTION fn_set_fecha_actualizacion();

-- ---------------------------------------------------------------------
-- tipo_orden — RN-11, CU-15: Normal / Urgente
-- El comportamiento diferencial se define como DATO, no como codigo:
--   - estado_inicial_id : NORMAL -> RECIBIDO ; URGENTE -> EN_EVALUACION
--   - notifica_admin    : TRUE para URGENTE
--   - recargo_monto     : monto fijo adicional (D-14). URGENTE = 200.
-- ---------------------------------------------------------------------
CREATE TABLE tipo_orden (
    id                   SMALLSERIAL    PRIMARY KEY,
    codigo               VARCHAR(20)    NOT NULL UNIQUE,
    nombre               VARCHAR(50)    NOT NULL,
    estado_inicial_id    SMALLINT       NOT NULL REFERENCES estado(id),
    notifica_admin       BOOLEAN        NOT NULL DEFAULT FALSE,
    recargo_monto        NUMERIC(10,2)  NOT NULL DEFAULT 0,
    activo               BOOLEAN        NOT NULL DEFAULT TRUE,

    CONSTRAINT chk_tipo_orden_codigo  CHECK (codigo IN ('NORMAL', 'URGENTE')),
    CONSTRAINT chk_tipo_orden_recargo CHECK (recargo_monto >= 0)
);
COMMENT ON TABLE  tipo_orden IS 'RN-11/CU-15: tipo de orden definido al crearla. Determina estado inicial, notificacion al admin y recargo.';
COMMENT ON COLUMN tipo_orden.recargo_monto IS 'RN-11 + D-14: monto fijo que se suma al precio del trabajo. URGENTE = 200. ATENCION: RN-11 documenta el recargo como PORCENTAJE; esta columna implementa un MONTO FIJO segun indicacion del desarrollador. Requiere confirmacion de la clienta (S-11).';
COMMENT ON COLUMN tipo_orden.estado_inicial_id IS 'RN-11: URGENTE nace en EN_EVALUACION; NORMAL nace en RECIBIDO.';


-- =====================================================================
-- 3. NUCLEO OPERATIVO: ORDENES
-- =====================================================================

-- Secuencia para el codigo visible de caso (formato LG-XXXX visto en los mockups)
CREATE SEQUENCE seq_codigo_orden START 1;

-- Secuencia para el codigo de paciente (RN-03 / D-15: identificacion sin nombre completo)
CREATE SEQUENCE seq_codigo_paciente START 1000;

-- ---------------------------------------------------------------------
-- orden — RN-01, RN-04, RN-11, RN-17, RN-18, CU-09, CU-20, D-04
-- ---------------------------------------------------------------------
CREATE TABLE orden (
    id                        BIGSERIAL     PRIMARY KEY,
    codigo                    VARCHAR(20)   NOT NULL UNIQUE
                                            DEFAULT 'LG-' || to_char(nextval('seq_codigo_orden'), 'FM0000'),
    odontologo_id             BIGINT         NOT NULL REFERENCES usuario(id),
    paciente_nombre           VARCHAR(150)   NOT NULL,
    paciente_iniciales        VARCHAR(10)    NOT NULL,
    paciente_codigo           INTEGER        NOT NULL UNIQUE
                                             DEFAULT nextval('seq_codigo_paciente'),
    tipo_trabajo_id           INTEGER        NOT NULL REFERENCES tipo_trabajo(id),
    tipo_orden_id             SMALLINT       NOT NULL REFERENCES tipo_orden(id),
    estado_id                 SMALLINT       NOT NULL REFERENCES estado(id),
    descripcion               TEXT,
    fecha_ingreso             DATE           NOT NULL,
    fecha_estimada_entrega    DATE           NOT NULL,
    dias_estimados_aplicados  SMALLINT       NOT NULL,
    precio_base               NUMERIC(10,2)  NOT NULL,
    recargo_urgencia          NUMERIC(10,2)  NOT NULL DEFAULT 0,
    precio_total              NUMERIC(10,2)  GENERATED ALWAYS AS (precio_base + recargo_urgencia) STORED,
    cargo_cancelacion         NUMERIC(10,2),
    fecha_creacion            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    fecha_actualizacion       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    fecha_cancelacion         TIMESTAMPTZ,

    CONSTRAINT chk_orden_fechas    CHECK (fecha_estimada_entrega >= fecha_ingreso),
    CONSTRAINT chk_orden_dias      CHECK (dias_estimados_aplicados >= 7),
    CONSTRAINT chk_orden_precio    CHECK (precio_base >= 250),
    CONSTRAINT chk_orden_recargo   CHECK (recargo_urgencia >= 0),
    CONSTRAINT chk_orden_cargo_can CHECK (cargo_cancelacion IS NULL OR cargo_cancelacion >= 0)
);
COMMENT ON TABLE  orden IS 'CU-09: orden creada por el odontologo. RN-17: inmutable tras su creacion, solo cancelable.';
COMMENT ON COLUMN orden.codigo IS 'Codigo visible del caso (formato LG-XXXX segun mockups del PDF original).';
COMMENT ON COLUMN orden.odontologo_id IS 'RN-01: aislamiento de datos. Cada odontologo solo accede a las ordenes donde figura como propietario.';
COMMENT ON COLUMN orden.paciente_nombre IS 'D-04: dato que ingresa el odontologo al crear la orden. USO INTERNO: no debe exponerse en listados (RN-03).';
COMMENT ON COLUMN orden.paciente_iniciales IS 'D-15 (resuelve S-02): iniciales derivadas del nombre. Junto al codigo es la identificacion visible del paciente (ej.: M.P. - Caso #2458), preservando RN-03.';
COMMENT ON COLUMN orden.paciente_codigo IS 'D-15: codigo autoincremental del paciente. Ver S-12: sin entidad paciente, un mismo paciente recibe un codigo distinto por cada orden.';
COMMENT ON COLUMN orden.precio_base IS 'D-14: foto del precio del tipo de trabajo al momento de crear la orden. Cambios posteriores del catalogo no alteran ordenes ya emitidas.';
COMMENT ON COLUMN orden.recargo_urgencia IS 'RN-11 + D-14: foto de tipo_orden.recargo_monto (200 para URGENTE, 0 para NORMAL).';
COMMENT ON COLUMN orden.precio_total IS 'Columna generada: precio_base + recargo_urgencia. Calculada por el motor, no puede quedar desincronizada.';
COMMENT ON COLUMN orden.cargo_cancelacion IS 'P-14 PENDIENTE de confirmacion con la clienta: 10% de precio_base si la cancelacion ocurre en EN_PRODUCCION. La columna existe pero NO debe aplicarse logica hasta la confirmacion.';
COMMENT ON COLUMN orden.fecha_estimada_entrega IS 'RN-18: calculada como fecha_ingreso + dias habiles del tipo de trabajo.';
COMMENT ON COLUMN orden.dias_estimados_aplicados IS 'Foto del valor de dias_estimados vigente al crear la orden (CU-16 A1: cambios posteriores del catalogo no alteran ordenes ya emitidas).';

CREATE INDEX idx_orden_odontologo  ON orden(odontologo_id);
CREATE INDEX idx_orden_estado      ON orden(estado_id);
CREATE INDEX idx_orden_tipo_orden  ON orden(tipo_orden_id);
CREATE INDEX idx_orden_entrega     ON orden(fecha_estimada_entrega);

CREATE TRIGGER trg_orden_actualizacion
    BEFORE UPDATE ON orden
    FOR EACH ROW EXECUTE FUNCTION fn_set_fecha_actualizacion();

-- ---------------------------------------------------------------------
-- orden_historial_estado — CU-04, CU-06: linea de tiempo con fecha y hora
-- ---------------------------------------------------------------------
CREATE TABLE orden_historial_estado (
    id            BIGSERIAL     PRIMARY KEY,
    orden_id      BIGINT        NOT NULL REFERENCES orden(id) ON DELETE CASCADE,
    estado_id     SMALLINT      NOT NULL REFERENCES estado(id),
    usuario_id    BIGINT        REFERENCES usuario(id),
    fecha_hora    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    observacion   TEXT
);
COMMENT ON TABLE  orden_historial_estado IS 'CU-04: alimenta la linea de tiempo del seguimiento (etapa + fecha/hora). CU-06: registra cada transicion.';
COMMENT ON COLUMN orden_historial_estado.usuario_id IS 'Autor del cambio. NULL cuando lo asigna el sistema (ej.: estado inicial de una orden urgente, RN-11).';

CREATE INDEX idx_historial_orden ON orden_historial_estado(orden_id, fecha_hora);

-- ---------------------------------------------------------------------
-- orden_archivo — RN-13, CU-09: imagenes y documentos adjuntos
-- Los limites de formato y tamano se imponen tambien a nivel de base de datos.
-- ---------------------------------------------------------------------
CREATE TABLE orden_archivo (
    id                   BIGSERIAL     PRIMARY KEY,
    orden_id             BIGINT        NOT NULL REFERENCES orden(id) ON DELETE CASCADE,
    categoria            VARCHAR(10)   NOT NULL,
    nombre_original      VARCHAR(255)  NOT NULL,
    ruta_almacenamiento  VARCHAR(500)  NOT NULL,
    tipo_mime            VARCHAR(100)  NOT NULL,
    tamano_bytes         BIGINT        NOT NULL,
    subido_por           BIGINT        REFERENCES usuario(id),
    fecha_carga          TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT chk_archivo_categoria CHECK (categoria IN ('IMAGEN', 'DOCUMENTO')),
    CONSTRAINT chk_archivo_tamano    CHECK (tamano_bytes > 0),
    -- RN-13: fotos JPG/PNG hasta 5 MB; documentos PDF/DOCX hasta 8 MB
    CONSTRAINT chk_archivo_formato_tamano CHECK (
        (categoria = 'IMAGEN'
            AND tipo_mime IN ('image/jpeg', 'image/png')
            AND tamano_bytes <= 5242880)
     OR (categoria = 'DOCUMENTO'
            AND tipo_mime IN ('application/pdf',
                              'application/vnd.openxmlformats-officedocument.wordprocessingml.document')
            AND tamano_bytes <= 8388608)
    )
);
COMMENT ON TABLE orden_archivo IS 'RN-13/CU-09: adjuntos de la orden. Imagenes JPG/PNG <= 5 MB; documentos PDF/DOCX <= 8 MB.';
COMMENT ON CONSTRAINT chk_archivo_formato_tamano ON orden_archivo IS 'RN-13: ultima linea de defensa. La validacion primaria ocurre en el backend antes de persistir el archivo.';

CREATE INDEX idx_archivo_orden ON orden_archivo(orden_id);


-- =====================================================================
-- 4. NOTIFICACIONES
-- =====================================================================

-- ---------------------------------------------------------------------
-- notificacion — RN-05, RN-11, RN-19, CU-07
-- ---------------------------------------------------------------------
CREATE TABLE notificacion (
    id               BIGSERIAL     PRIMARY KEY,
    destinatario_id  BIGINT        NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    orden_id         BIGINT        REFERENCES orden(id) ON DELETE CASCADE,
    tipo_evento      VARCHAR(30)   NOT NULL,
    mensaje          TEXT          NOT NULL,
    leida            BOOLEAN       NOT NULL DEFAULT FALSE,
    fecha_creacion   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    fecha_lectura    TIMESTAMPTZ,

    CONSTRAINT chk_notificacion_evento CHECK (tipo_evento IN (
        'CUENTA_CREADA',    -- RN-16 / CU-19
        'NUEVA_ORDEN',      -- RN-19 / CU-09 (destinatario: laboratorio)
        'ORDEN_URGENTE',    -- RN-11 / CU-15 (destinatario: administrador)
        'CAMBIO_ESTADO'     -- RN-05  / CU-07 (destinatario: odontologo)
    ))
);
COMMENT ON TABLE  notificacion IS 'RN-05/RN-11/RN-19: notificacion logica. Alimenta el contador de la campana en la aplicacion.';
COMMENT ON COLUMN notificacion.tipo_evento IS 'Lista acotada a los eventos documentados. Ver sugerencia S-09 (notificacion de cancelacion no documentada).';

CREATE INDEX idx_notificacion_destinatario ON notificacion(destinatario_id, leida);
CREATE INDEX idx_notificacion_orden        ON notificacion(orden_id);

-- ---------------------------------------------------------------------
-- notificacion_envio — RN-05, RN-19: un registro por canal
-- Soporta APP + CORREO hoy y TELEGRAM sin cambios de modelo (fase futura).
-- ---------------------------------------------------------------------
CREATE TABLE notificacion_envio (
    id                BIGSERIAL     PRIMARY KEY,
    notificacion_id   BIGINT        NOT NULL REFERENCES notificacion(id) ON DELETE CASCADE,
    canal             VARCHAR(10)   NOT NULL,
    estado_envio      VARCHAR(10)   NOT NULL DEFAULT 'PENDIENTE',
    fecha_envio       TIMESTAMPTZ,
    detalle_error     TEXT,

    CONSTRAINT chk_envio_canal  CHECK (canal IN ('APP', 'CORREO', 'TELEGRAM')),
    CONSTRAINT chk_envio_estado CHECK (estado_envio IN ('PENDIENTE', 'ENVIADO', 'FALLIDO')),
    CONSTRAINT uq_envio_canal   UNIQUE (notificacion_id, canal)
);
COMMENT ON TABLE  notificacion_envio IS 'RN-05/RN-19: trazabilidad del envio por cada canal.';
COMMENT ON COLUMN notificacion_envio.canal IS 'TELEGRAM esta previsto en el modelo: RN-19 (laboratorio, alcance actual) y fase futura para el odontologo.';

-- ---------------------------------------------------------------------
-- configuracion_notificacion — RN-19, CU-21
-- ---------------------------------------------------------------------
CREATE TABLE configuracion_notificacion (
    id                    SMALLSERIAL   PRIMARY KEY,
    usuario_id            BIGINT        NOT NULL UNIQUE REFERENCES usuario(id) ON DELETE CASCADE,
    canal_app_activo      BOOLEAN       NOT NULL DEFAULT TRUE,
    canal_correo_activo   BOOLEAN       NOT NULL DEFAULT TRUE,
    canal_telegram_activo BOOLEAN       NOT NULL DEFAULT FALSE,
    telegram_chat_id      VARCHAR(100),
    fecha_actualizacion   TIMESTAMPTZ   NOT NULL DEFAULT now(),

    -- RN-19: si el canal Telegram esta activo debe existir el destino
    CONSTRAINT chk_config_telegram CHECK (
        canal_telegram_activo = FALSE OR telegram_chat_id IS NOT NULL
    )
);
COMMENT ON TABLE configuracion_notificacion IS 'RN-19/CU-21: canales por los que el Administrador recibe las notificaciones de nuevas ordenes.';

CREATE TRIGGER trg_config_notif_actualizacion
    BEFORE UPDATE ON configuracion_notificacion
    FOR EACH ROW EXECUTE FUNCTION fn_set_fecha_actualizacion();


-- =====================================================================
-- 5. LICENCIAMIENTO — RN-20, CU-23
-- D-16: el sistema NO es multi-tenant. Existe UNA instalacion y UNA base de datos
-- por laboratorio, por lo que la licencia aplica a esta instalacion y su vencimiento
-- no afecta a ningun otro laboratorio. Por eso la tabla no lleva laboratorio_id.
-- Se modela unicamente la vigencia y activacion manual por el SuperAdmin.
-- Planes y pasarela de pago quedan FUERA (P-11, P-12) hasta su definicion.
-- =====================================================================
CREATE TABLE licencia (
    id                  BIGSERIAL     PRIMARY KEY,
    fecha_inicio        DATE          NOT NULL,
    fecha_vencimiento   DATE          NOT NULL,
    estado              VARCHAR(10)   NOT NULL DEFAULT 'ACTIVA',
    activada_por        BIGINT        REFERENCES usuario(id),
    fecha_registro      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    observacion         TEXT,

    CONSTRAINT chk_licencia_estado CHECK (estado IN ('ACTIVA', 'VENCIDA')),
    CONSTRAINT chk_licencia_fechas CHECK (fecha_vencimiento > fecha_inicio)
);
COMMENT ON TABLE  licencia IS 'RN-20/CU-23/D-16: periodos de licencia de ESTA instalacion (una por laboratorio, sin multi-tenant). Sin licencia vigente el sistema se bloquea.';
COMMENT ON COLUMN licencia.activada_por IS 'CU-23: SuperAdmin que registra la activacion o renovacion (activacion manual, sin pasarela de pago: P-11/P-12 pendientes).';

CREATE INDEX idx_licencia_vencimiento ON licencia(fecha_vencimiento DESC);


-- =====================================================================
-- 6. MODULO DE MENSAJES — D-11: POSPUESTO A PROXIMA VERSION
-- La estructura se deja PREVISTA (no se implementa funcionalidad) para que
-- su incorporacion futura no exija migrar el modelo. Ver FT-03 / CU-08.
-- =====================================================================
CREATE TABLE mensaje (
    id             BIGSERIAL     PRIMARY KEY,
    orden_id       BIGINT        NOT NULL REFERENCES orden(id) ON DELETE CASCADE,
    emisor_id      BIGINT        NOT NULL REFERENCES usuario(id),
    contenido      TEXT          NOT NULL,
    leido          BOOLEAN       NOT NULL DEFAULT FALSE,
    fecha_envio    TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT chk_mensaje_contenido CHECK (length(trim(contenido)) > 0)
);
COMMENT ON TABLE mensaje IS 'D-11: modulo POSPUESTO. Tabla prevista (RN-07: un hilo por orden). NO implementar funcionalidad en esta version.';

CREATE INDEX idx_mensaje_orden ON mensaje(orden_id, fecha_envio);


-- =====================================================================
-- 7. DATOS INICIALES (SEED)
-- =====================================================================

-- --- Roles (RN-14) ---------------------------------------------------
INSERT INTO rol (codigo, nombre, descripcion) VALUES
('SUPERADMIN',  'SuperAdmin',    'Desarrollador. Todos los privilegios, para mantenibilidad del sistema.'),
('ADMIN',       'Administrador', 'Laboratorio. Gestiona ordenes, estados, catalogos y configuracion.'),
('ODONTOLOGO',  'Odontologo',    'Cliente. Crea y consulta unicamente sus propias ordenes.');

-- --- Estados (RN-04) -------------------------------------------------
INSERT INTO estado (codigo, nombre, descripcion, orden_secuencia, es_terminal, es_productivo) VALUES
('RECIBIDO',           'Recibido',           'El laboratorio recibio correctamente el caso.',            1, FALSE, TRUE),
('EN_EVALUACION',      'En evaluacion',      'Se esta revisando la informacion y los registros.',        2, FALSE, TRUE),
('EN_PRODUCCION',      'En produccion',      'El trabajo esta siendo confeccionado.',                    3, FALSE, TRUE),
('CONTROL_CALIDAD',    'Control de calidad', 'Se esta revisando la terminacion y adaptacion.',           4, FALSE, TRUE),
('LISTO',              'Listo',              'El trabajo esta terminado y disponible para retirar.',     5, FALSE, TRUE),
('ENTREGADO',          'Entregado',          'El caso fue entregado al odontologo.',                     6, TRUE,  TRUE),
('CANCELADO',          'Cancelado',          'La orden fue cancelada por el odontologo tras su creacion.', NULL, TRUE, FALSE);

-- --- Tipos de orden (RN-11, D-14) ------------------------------------
INSERT INTO tipo_orden (codigo, nombre, estado_inicial_id, notifica_admin, recargo_monto)
SELECT 'NORMAL', 'Normal', e.id, FALSE, 0 FROM estado e WHERE e.codigo = 'RECIBIDO';

INSERT INTO tipo_orden (codigo, nombre, estado_inicial_id, notifica_admin, recargo_monto)
SELECT 'URGENTE', 'Urgente', e.id, TRUE, 200 FROM estado e WHERE e.codigo = 'EN_EVALUACION';

-- --- Tipos de trabajo (RN-12, RN-21, Anexo A: 45 tipos) ---------------
-- dias_estimados = 7 (minimo definido). Los valores especificos por tipo estan
-- PENDIENTES de la clienta; no se inventan valores distintos.
-- precio = 250 (minimo definido). Los precios reales por tipo estan PENDIENTES
-- de la reunion con la clienta (P-15); no se inventan valores distintos.
-- Los nombres respetan la lista original de la clienta (incluye 'N' con virgulilla).
-- Requiere que la base este en UTF8 (valor por defecto de la imagen oficial de Postgres).
INSERT INTO tipo_trabajo (nombre, dias_estimados, precio) VALUES
('AAK', 7, 250),
('ADAPTACION TORNILLO ARAÑA', 7, 250),
('ARCO LINGUAL', 7, 250),
('BASE DE REPARACION SIMPLE', 7, 250),
('BIMLER B DECKBIS', 7, 250),
('BIMLER A ESTANDAR', 7, 250),
('BIMLER C PROGENIE', 7, 250),
('BIONATOR', 7, 250),
('BOTON DE NANCE', 7, 250),
('BOTON DE NANCE + BARRA PALATINA', 7, 250),
('BLOQUES GEMELOS', 7, 250),
('CAMBIO DE BASE', 7, 250),
('CONTENCION FIJA', 7, 250),
('CONTENCION HAWLEY', 7, 250),
('MC NAMARA + TORNILLO ESTANDAR + GANCHOS PARA MASCARA', 7, 250),
('DISYUNTOR CON TORNILLO ESTANDAR', 7, 250),
('DONAT', 7, 250),
('APARATO LEVANTE DE MORDIDA', 7, 250),
('TORNILLO ARAÑA + MASCARA', 7, 250),
('LAMINA ESTAMPADA', 7, 250),
('MANTENEDOR DE ESPACIO FIJO', 7, 250),
('MASCARA D''LAIRE', 7, 250),
('MC NAMARA + TORNILLO ARAÑA', 7, 250),
('BOTON DE NANCE C/LEVANTE MORDIDA', 7, 250),
('ORTHORAMA', 7, 250),
('PEQUEÑO GIGANTE', 7, 250),
('PENDULO', 7, 250),
('PISTAS PLANAS', 7, 250),
('PLACA NEUROMIORELAJANTE', 7, 250),
('PLACA ACTIVA', 7, 250),
('PLANO INCLINADO', 7, 250),
('PROTECTOR BUCAL BAJO IMPACTO', 7, 250),
('PROTECTOR BUCAL CARACTERIZADO (ALTO IMPACTO)', 7, 250),
('QUADHELIX FIJO', 7, 250),
('QUADHELIX REMOVIBLE', 7, 250),
('REJILLA LINGUAL/PALATINA FIJA', 7, 250),
('REPARACIONES', 7, 250),
('RFA', 7, 250),
('DISPOSITIVO DE ADELANTAMIENTO MANDIBULAR (RONCADOR)', 7, 250),
('TRANSPALATINO', 7, 250),
('TRANSPALATINO + GANCHO SOLDADO', 7, 250),
('TORNILLO EXTRA', 7, 250),
('TORNILLO ARAÑA', 7, 250),
('SPLIN QUIRURGICO', 7, 250),
('DISYUNTOR BONDEADO C/TORNILLO ARAÑA', 7, 250);


-- =====================================================================
-- 8. VISTAS DE APOYO (solo lectura, derivadas de reglas documentadas)
-- =====================================================================

-- CU-10: indicadores del dashboard del laboratorio por estado
CREATE VIEW v_ordenes_por_estado AS
SELECT e.codigo   AS estado_codigo,
       e.nombre   AS estado_nombre,
       COUNT(o.id) AS cantidad
FROM estado e
LEFT JOIN orden o ON o.estado_id = e.id
GROUP BY e.id, e.codigo, e.nombre, e.orden_secuencia
ORDER BY e.orden_secuencia NULLS LAST;

-- CU-10/CU-15: ordenes urgentes activas (no terminales)
CREATE VIEW v_ordenes_urgentes AS
SELECT o.id, o.codigo, o.paciente_nombre, o.fecha_estimada_entrega,
       u.nombre_completo AS odontologo,
       e.nombre          AS estado
FROM orden o
JOIN tipo_orden t ON t.id = o.tipo_orden_id
JOIN usuario    u ON u.id = o.odontologo_id
JOIN estado     e ON e.id = o.estado_id
WHERE t.codigo = 'URGENTE'
  AND e.es_terminal = FALSE;

-- RN-03 / D-15: identificacion publica de la orden SIN el nombre completo del
-- paciente. Es la fuente que deben consumir los listados y el seguimiento.
CREATE VIEW v_orden_publica AS
SELECT o.id,
       o.codigo,
       o.paciente_iniciales || ' - Caso #' || o.paciente_codigo AS paciente_identificacion,
       tt.nombre  AS tipo_trabajo,
       tor.nombre AS tipo_orden,
       e.nombre   AS estado,
       o.fecha_ingreso,
       o.fecha_estimada_entrega,
       o.precio_total,
       o.odontologo_id
FROM orden o
JOIN tipo_trabajo tt  ON tt.id  = o.tipo_trabajo_id
JOIN tipo_orden   tor ON tor.id = o.tipo_orden_id
JOIN estado       e   ON e.id   = o.estado_id;

-- CU-23: vigencia de la licencia
CREATE VIEW v_licencia_vigente AS
SELECT EXISTS (
    SELECT 1 FROM licencia
    WHERE estado = 'ACTIVA'
      AND CURRENT_DATE BETWEEN fecha_inicio AND fecha_vencimiento
) AS licencia_activa;

-- =====================================================================
-- FIN DEL ESQUEMA
-- =====================================================================
