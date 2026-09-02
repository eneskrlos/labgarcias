-- =====================================================================
--  SEED DE INSTALACION - Lab Garcia's Connect
--
--  Se ejecuta UNA VEZ, a mano, DESPUES del primer arranque del backend
--  (para ese momento Flyway ya aplico V1, V2 y V3).
--
--  V1 seccion 7 ya siembra: rol, estado, tipo_orden y los 45 tipo_trabajo
--  del Anexo A. Este archivo solo agrega lo que falta para poder usar
--  el sistema:
--    1. El usuario SUPERADMIN (no hay forma de entrar sin el).
--    2. La licencia vigente (RN-20: sin ella el sistema se bloquea).
--
--  Idempotente: se puede correr dos veces sin romper nada.
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 1. USUARIO SUPERADMIN
--
--  Restricciones de V1 que hay que respetar:
--    - chk_usuario_credencial: si proveedor_auth = 'LOCAL', password_hash
--      NO puede ser NULL.
--    - chk_usuario_estado: estado_cuenta debe ser ACTIVA para poder
--      loguearse (el default PENDIENTE_VERIFICACION bloquea el acceso).
--    - uq_usuario_correo y uq_usuario_nombre_usuario son UNIQUE.
--    - direccion es opcional para SUPERADMIN/ADMIN (RN-16).
--
--  De V2: debe_cambiar_password en TRUE, para que el filtro
--  cambioPasswordObligatorioFilter exija el cambio en el primer login
--  (D-18). Asi el hash de abajo solo sirve una vez.
--
--  GENERAR EL HASH ANTES DE CORRER ESTO. Ver instrucciones al final.
--  La contrasena debe cumplir RN-15: minimo 9 caracteres, con mayuscula,
--  minuscula, numero y caracter especial.
-- ---------------------------------------------------------------------
INSERT INTO usuario (
    rol_id,
    nombre_completo,
    correo,
    nombre_usuario,
    password_hash,
    proveedor_auth,
    estado_cuenta,
    correo_verificado,
    debe_cambiar_password
)
SELECT
    r.id,
    'Administrador del Sistema',
    'REEMPLAZAR@tucorreo.com',
    'superadmin',
    '$2b$10$REEMPLAZAR_POR_EL_HASH_GENERADO',
    'LOCAL',
    'ACTIVA',
    TRUE,
    TRUE
FROM rol r
WHERE r.codigo = 'SUPERADMIN'
ON CONFLICT (correo) DO NOTHING;


-- ---------------------------------------------------------------------
-- 2. CONFIGURACION DE NOTIFICACIONES DEL SUPERADMIN
--
--  RN-19/CU-21. Telegram queda en FALSE: chk_config_telegram exige que,
--  si el canal esta activo, exista telegram_chat_id. El chat_id se captura
--  recien cuando el usuario se vincula con el bot (D-21).
-- ---------------------------------------------------------------------
INSERT INTO configuracion_notificacion (usuario_id, canal_app_activo, canal_correo_activo, canal_telegram_activo)
SELECT u.id, TRUE, TRUE, FALSE
FROM usuario u
WHERE u.nombre_usuario = 'superadmin'
ON CONFLICT (usuario_id) DO NOTHING;


-- ---------------------------------------------------------------------
-- 3. LICENCIA VIGENTE
--
--  RN-20/CU-23. Sin una fila ACTIVA cuyo rango cubra CURRENT_DATE, la
--  vista v_licencia_vigente devuelve FALSE y el sistema entero se bloquea.
--
--  chk_licencia_fechas exige fecha_vencimiento > fecha_inicio.
--  La tabla no tiene UNIQUE, asi que la idempotencia va con NOT EXISTS.
--
--  Un ano de vigencia. Ajustalo si queres otro periodo.
-- ---------------------------------------------------------------------
INSERT INTO licencia (fecha_inicio, fecha_vencimiento, estado, activada_por, observacion)
SELECT
    CURRENT_DATE,
    CURRENT_DATE + INTERVAL '1 year',
    'ACTIVA',
    u.id,
    'Licencia inicial creada por el seed de instalacion.'
FROM usuario u
WHERE u.nombre_usuario = 'superadmin'
  AND NOT EXISTS (
      SELECT 1 FROM licencia
      WHERE estado = 'ACTIVA'
        AND CURRENT_DATE BETWEEN fecha_inicio AND fecha_vencimiento
  );


COMMIT;


-- =====================================================================
--  VERIFICACION - correr despues del seed
-- =====================================================================
-- SELECT u.nombre_usuario, u.correo, u.estado_cuenta, u.debe_cambiar_password, r.codigo
--   FROM usuario u JOIN rol r ON r.id = u.rol_id;
--
-- SELECT * FROM v_licencia_vigente;        -- tiene que devolver true
-- SELECT count(*) FROM tipo_trabajo;       -- tiene que devolver 45
-- SELECT codigo, nombre FROM estado ORDER BY orden_secuencia NULLS LAST;


-- =====================================================================
--  COMO GENERAR EL HASH BCRYPT
-- =====================================================================
--  Sin instalar nada, usando la imagen de Python:
--
--    docker run --rm python:3.12-alpine sh -c \
--      "pip install -q bcrypt && python -c \"import bcrypt; \
--       print(bcrypt.hashpw(b'TuClave.Segura9', bcrypt.gensalt(10)).decode())\""
--
--  Cambia 'TuClave.Segura9' por la tuya. Debe cumplir RN-15:
--  minimo 9 caracteres, mayuscula, minuscula, numero y especial.
--
--  El hash sale con prefijo $2b$. Spring Security lo acepta:
--  BCryptPasswordEncoder soporta $2a$, $2b$ y $2y$ indistintamente.
--
--  Como debe_cambiar_password queda en TRUE, esta contrasena solo sirve
--  para el primer login. Igual, no la reutilices de otro lado.
-- =====================================================================
