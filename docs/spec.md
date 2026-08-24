# spec.md — Especificación técnica

**Proyecto:** Lab. Garcia's Connect
**Versión:** 2.0 — 18/08/2026
**Base:** Análisis v2.2 + Modelo de datos v1.2 + esquema `01_labgarcias_schema.sql` + **CR-01**

## CR-01 — Cambio de alcance (reunión con la clienta, 17/08/2026)

| Decisión | Cambio | Reemplaza a |
|---|---|---|
| D-17 | Se **elimina** la autenticación con Google. En su lugar: botón "Solicitar acceso" con formulario público (nombre, correo, dirección, teléfono) que notifica al admin. | §3.4 (Google), CU-18 auto-registro |
| D-18 | El **admin crea las cuentas** de odontólogo desde la gestión de usuarios, con contraseña autogenerada notificada por **correo** (y Telegram si ya está vinculado), y **cambio obligatorio en el primer login**. | CU-19 verificación por correo |
| D-19 | **Por el momento las órdenes las crea el admin** (el odontólogo la envía en papel o por teléfono). La pantalla "Nueva orden" del odontólogo se retira; el endpoint queda restringido a ADMIN. | §5.1 (rol) |
| D-20 | Cada cambio de estado notifica al odontólogo por **correo y Telegram**, además de la campana. | RN-05 (canales) |
| D-21 | **Telegram reemplaza a WhatsApp como canal de mensajería instantánea** (18/08/2026): la Bot API de Telegram es oficial y gratuita; WhatsApp requiere un proveedor pago (Meta/Twilio) que la clienta no puede asumir hoy. WhatsApp queda como **estructura** activable a futuro (P-18). | D-20 original (WhatsApp) |

**Nuevos pendientes:** P-18 (WhatsApp: canal en estructura; se activará cuando el negocio justifique pagar un proveedor — Meta Cloud API o Twilio), P-19 (¿la creación de órdenes por el odontólogo vuelve más adelante? el flujo queda documentado), P-20 (bot de Telegram: crear con @BotFather y configurar el token en las properties — tarea del desarrollador, no del agente).

Los cambios de base de datos de CR-01 van en la migración **V2** (§1.2). **V1 no se toca.**

Este documento define **qué** construir. Las reglas de cómo trabajar están en `Agente.md`; el orden en `Plan.md`.

> Todo lo no especificado aquí **no se implementa**. Ante un vacío, aplicar la sección 3.2 de `Agente.md`.

---

## 1. Stack y convenciones

| Capa | Tecnología |
|---|---|
| Frontend | React.js + CSS moderno (CSS Modules o custom properties) |
| Backend | Java 17+ / Spring Boot 3.x |
| Base de datos | PostgreSQL 15+, esquema `labgarcias` |
| Migraciones | Flyway |
| Seguridad | Spring Security + JWT |
| Estado servidor (front) | TanStack Query |
| Documentación de API | springdoc-openapi (Swagger UI) — **solo en desarrollo** |

**Convenciones REST**

- Base: `/api/v1`
- Nombres de recurso en plural y en español: `/ordenes`, `/tipos-trabajo`, `/notificaciones`
- Códigos: `200` OK, `201` creado, `400` validación, `401` no autenticado, `403` sin permiso, `404` no encontrado, `409` conflicto de estado, `422` regla de negocio incumplida, `423` licencia vencida
- Errores con cuerpo uniforme:
  ```json
  { "codigo": "TIPO_TRABAJO_INACTIVO", "mensaje": "...", "campo": "tipoTrabajoId" }
  ```
- Fechas en ISO-8601. `TIMESTAMPTZ` se serializa en UTC.
- Paginación: `?page=0&size=10&sort=campo,asc`. Respuesta:
  ```json
  { "contenido": [], "total": 45, "pagina": 0, "tamano": 10, "totalPaginas": 5 }
  ```
  `page` es **base 0**. `size` solo admite **10, 20 o 30**; otro valor → `400 TAMANO_PAGINA_INVALIDO`. Por defecto `page=0`, `size=10`.
  **Toda paginación se resuelve en el backend** con `Pageable` de Spring Data. Prohibido devolver la colección completa y paginar en el cliente.

### 1.0 Migración V2 (CR-01)

`V2__cr01_solicitud_acceso_whatsapp.sql` — cambios sobre el esquema validado:

```sql
-- D-17/D-18: teléfono y cambio obligatorio de contraseña
ALTER TABLE labgarcias.usuario ADD COLUMN telefono VARCHAR(30);
ALTER TABLE labgarcias.usuario ADD COLUMN debe_cambiar_password BOOLEAN NOT NULL DEFAULT FALSE;

-- D-17: solicitudes de acceso
CREATE TABLE labgarcias.solicitud_acceso (
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
ALTER TABLE labgarcias.notificacion_envio DROP CONSTRAINT chk_envio_canal;
ALTER TABLE labgarcias.notificacion_envio ADD CONSTRAINT chk_envio_canal
    CHECK (canal IN ('APP','CORREO','TELEGRAM','WHATSAPP'));

-- Nuevos eventos
ALTER TABLE labgarcias.notificacion DROP CONSTRAINT chk_notificacion_evento;
ALTER TABLE labgarcias.notificacion ADD CONSTRAINT chk_notificacion_evento
    CHECK (tipo_evento IN ('CUENTA_CREADA','NUEVA_ORDEN','ORDEN_URGENTE','CAMBIO_ESTADO',
                           'SOLICITUD_ACCESO','CREDENCIALES_CREADAS'));

-- P-18: canal WhatsApp configurable (estructura para el futuro; D-21)
ALTER TABLE labgarcias.configuracion_notificacion
    ADD COLUMN canal_whatsapp_activo BOOLEAN NOT NULL DEFAULT FALSE;

-- D-21: vinculación de Telegram por usuario (el bot no puede iniciar
-- conversaciones: cada usuario se vincula una vez y se captura su chat_id)
ALTER TABLE labgarcias.usuario ADD COLUMN telegram_chat_id VARCHAR(100);
ALTER TABLE labgarcias.usuario ADD COLUMN telegram_vinculado BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE labgarcias.telegram_token_vinculacion (
    id             BIGSERIAL    PRIMARY KEY,
    usuario_id     BIGINT       NOT NULL REFERENCES labgarcias.usuario(id) ON DELETE CASCADE,
    token          VARCHAR(64)  NOT NULL UNIQUE,
    fecha_emision  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_uso      TIMESTAMPTZ
);
```

`google_subject_id` y `token_verificacion` **no se eliminan** (sin migraciones destructivas), pero dejan de usarse: sin endpoints de Google ni de verificación.

**Nota sobre `configuracion_notificacion.telegram_chat_id`:** esa columna (V1) era la configuración del laboratorio. Con D-21 el destino de Telegram de **cada usuario** vive en `usuario.telegram_chat_id`, poblado por el flujo de vinculación (§6.5). La columna de V1 queda como configuración del admin.

### 1.1 Documentación de la API — Swagger

**Librería:** `springdoc-openapi-starter-webmvc-ui` (Spring Boot 3.x). **No usar springfox**, está discontinuado.

**Alcance: solo desarrollo.** La documentación se expone únicamente con el perfil `dev` activo. En producción queda deshabilitada:

```yaml
# application-dev.yml
springdoc:
  api-docs.enabled: true
  swagger-ui.enabled: true
  swagger-ui.path: /swagger-ui.html

# application-prod.yml
springdoc:
  api-docs.enabled: false
  swagger-ui.enabled: false
```

**Motivo:** exponer la estructura completa de la API en la instalación de un laboratorio no aporta valor y aumenta la superficie de ataque.

**Configuración obligatoria**

1. **Esquema de seguridad JWT.** Sin esto no se puede probar ningún endpoint autenticado desde la UI. Definir un `SecurityScheme` de tipo `HTTP`, esquema `bearer`, formato `JWT`, y aplicarlo globalmente.
2. **Exclusión en Spring Security.** Permitir sin autenticación: `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**`.
3. **Exclusión en el filtro de licencia** (§3.6). Sin esto, con la licencia vencida la propia documentación queda bloqueada.
4. **Metadatos:** título "Lab. Garcia's Connect API", versión, y descripción breve.

**Documentación de endpoints — obligatoria**

Todo endpoint nuevo se documenta en la misma tarea que lo crea. No se difiere.

| Anotación | Dónde | Contenido |
|---|---|---|
| `@Tag` | Controller | Nombre del módulo: "Órdenes", "Seguridad", "Catálogos" |
| `@Operation` | Método | `summary` con la acción y **el caso de uso que implementa** |
| `@ApiResponses` | Método | Los códigos de la sección correspondiente de este spec, con el `codigo` de error |
| `@Schema` | Campos de DTO | Descripción y ejemplo de cada campo |

Ejemplo del nivel esperado:

```java
@Operation(
    summary = "Crear una nueva orden (CU-09)",
    description = "El odontólogo crea una orden. El estado inicial, el recargo y la "
                + "fecha estimada los calcula el backend (RN-11, RN-18, RN-21)."
)
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "Orden creada"),
    @ApiResponse(responseCode = "422", description = "TIPO_TRABAJO_INACTIVO")
})
```

**Restricciones**

- **Prohibido** exponer entidades JPA en los esquemas: solo DTO (coherente con `Agente.md` 6.2).
- **Prohibido** incluir ejemplos con datos reales de pacientes. Usar nombres ficticios.
- Los campos de contraseña se marcan de forma que no se registren en ejemplos.

**Criterios de aceptación**

1. Con el perfil `dev`, `/swagger-ui.html` responde y lista todos los endpoints existentes.
2. Con el perfil `prod`, `/swagger-ui.html` y `/v3/api-docs` devuelven 404.
3. Se puede autenticar en la UI con el botón "Authorize" pegando un JWT y ejecutar un endpoint protegido.
4. Con la licencia vencida, Swagger sigue accesible en `dev`.
5. Ningún esquema expone una entidad JPA.

---

### 1.2 Configuración por instalación

*(Agregada el 23/08/2026 por el desarrollador, con T-27.)*

D-16 define una instalación y una base **por laboratorio**. Estas properties son las que cambian
de una instalación a otra: **las setea el desarrollador al instalar, no el agente**, que es el
mismo criterio que ya fijó P-20 para el bot de Telegram. Ninguna lleva un valor inventado en el
código, y las obligatorias **no tienen default en `prod`**: sin ellas la aplicación no arranca, a
propósito.

| Property | Variable de entorno | Obligatoria en `prod` | Para qué |
|---|---|---|---|
| `app.laboratorio.zona-horaria` | `LAB_ZONA_HORARIA` | **Sí**, sin default | Corte de la semana de "entregadas esta semana" (§5.7) |
| `telegram.bot.token` | `TELEGRAM_BOT_TOKEN` | No — sin él el canal se deshabilita | Bot API de Telegram (§6.3, P-20) |
| `telegram.bot.username` | `TELEGRAM_BOT_USERNAME` | No — sin él la vinculación se deshabilita | Enlace profundo de vinculación (§6.5, P-20) |

**`app.laboratorio.zona-horaria`** — zona IANA del laboratorio; en `dev`, `America/Montevideo` por
defecto.

- **Qué decide:** los contadores "entregadas esta semana" de los dos paneles (§5.7) cuentan el
  pasaje a `ENTREGADO` de `orden_historial_estado`, y esa columna es `TIMESTAMPTZ`. La semana va de
  lunes a domingo **en esta zona**.
- **Por qué no puede quedar en UTC:** un trabajo entregado el domingo a las 21:00 en Montevideo son
  las 00:00 del lunes en UTC, así que con el corte en UTC caería en la semana siguiente y el
  indicador mentiría justo el día de mayor actividad.
- **Por qué no se deduce del servidor:** el huso del sistema operativo no es un dato del negocio y
  cambia con la máquina; el del laboratorio, no.
- **Por qué no tiene default en `prod`:** un default sería adivinar dónde está el laboratorio. En
  `dev` sí lo tiene porque ahí el valor no afecta a nadie.

---

## 2. Constantes del dominio

Definidas una sola vez, con referencia a su regla (ver `Agente.md` 6.1):

| Constante | Valor | Regla |
|---|---|---|
| `DIAS_HABILES_MINIMOS` | 7 | RN-12 |
| `PRECIO_MINIMO_TRABAJO` | 250 | RN-21 |
| `LONGITUD_MINIMA_PASSWORD` | 9 | RN-15 |
| `TAMANO_MAXIMO_IMAGEN` | 5 MB (5242880) | RN-13 |
| `TAMANO_MAXIMO_DOCUMENTO` | 8 MB (8388608) | RN-13 |
| `HORAS_VIGENCIA_TOKEN` | 24 | D-02 |
| `FORMATOS_IMAGEN` | `image/jpeg`, `image/png` | RN-13 |
| `FORMATOS_DOCUMENTO` | `application/pdf`, `...wordprocessingml.document` | RN-13 |

El recargo por urgencia (200) y el estado inicial **no son constantes**: se leen de la tabla `tipo_orden`.

---

## 3. Módulo Seguridad

### 3.1 Solicitud de acceso — D-17 (reemplaza el auto-registro CU-18)

`POST /api/v1/auth/solicitud-acceso` · público

**Request**
```json
{
  "nombreCompleto": "Dr. Juan Pérez",
  "correo": "juan@mail.com",
  "direccion": "Av. 18 de Julio 1234",
  "telefono": "+59891234567"
}
```

**Validaciones**
- Todos los campos obligatorios. Correo con formato válido; teléfono con formato internacional.
- Si ya existe un usuario con ese correo → `409 CORREO_YA_REGISTRADO`.
- Si ya existe una solicitud PENDIENTE con ese correo → `409 SOLICITUD_YA_EXISTENTE`.

**Comportamiento**
- Crea `solicitud_acceso` en estado `PENDIENTE`.
- Publica `SolicitudAccesoEvent` → notificación al ADMIN (`SOLICITUD_ACCESO`) por sus canales activos: campana, correo y los demás configurados. **No se crea ningún usuario.**

**Respuesta** `201` — `{ "mensaje": "Solicitud enviada. El laboratorio se pondrá en contacto." }`

**Frontend:** el botón "Solicitar acceso" del login (mockup) abre el formulario. Sin captcha en esta versión.

**Criterios de aceptación**
1. La solicitud no crea usuario ni permite login.
2. El admin recibe la notificación por campana y correo.
3. Un correo con solicitud pendiente no puede duplicarla.

---

### 3.1.b Alta de odontólogo por el administrador — D-18

`POST /api/v1/odontologos` · rol `ADMIN`, `SUPERADMIN`

**Request**
```json
{
  "nombreCompleto": "Dr. Juan Pérez",
  "correo": "juan@mail.com",
  "nombreUsuario": "jperez",
  "direccion": "Av. 18 de Julio 1234",
  "telefono": "+59891234567",
  "solicitudId": 12
}
```
`solicitudId` es opcional: si viene, la solicitud pasa a `APROBADA` y sella `fecha_resolucion`.

**Comportamiento**
1. Valida correo y nombre de usuario únicos (mismos `409` de siempre).
2. **Genera la contraseña**: aleatoria, cumpliendo RN-15 (mínimo 9, mayúsculas, minúsculas, números, especiales). Se genera con `SecureRandom`. **Nunca se persiste en claro ni se loguea**: solo el hash BCrypt.
3. Crea el usuario: rol `ODONTOLOGO`, `estado_cuenta = ACTIVA`, `correo_verificado = true` (no hay verificación: el admin es la verificación), **`debe_cambiar_password = true`**.
4. Publica `CredencialesCreadasEvent` → notificación `CREDENCIALES_CREADAS` al odontólogo por **correo** (canal garantizado: en este momento el odontólogo aún no vinculó Telegram). El texto incluye nombre de usuario, la contraseña temporal y la indicación de que **deberá cambiarla al ingresar**.

**Tratamiento de la contraseña temporal — decisión del desarrollador (18/08/2026).** El outbox persiste `notificacion.mensaje`, así que enviar la contraseña por el flujo normal la dejaría en claro en la base. Se resuelve de forma híbrida:

- `notificacion.mensaje` guarda un **texto genérico sin la contraseña**: *"Se creó tu cuenta en Lab. Garcia's Connect. Revisá tu correo por las credenciales de acceso."*
- El correo con nombre de usuario y contraseña temporal **se compone y envía directamente en el listener** `@TransactionalEventListener(AFTER_COMMIT)`. La contraseña viaja **solo en memoria**, dentro del evento. Nunca se persiste ni se loguea.
- El `notificacion_envio` de canal `CORREO` se registra igual y se marca `ENVIADO` o `FALLIDO` según el resultado (criterio 3 de esta sección).

> **Limitación aceptada:** si ese envío falla, **no es reintentable desde el outbox** — la contraseña ya no existe en ningún lado. El remedio operativo es que el admin vuelva a crear las credenciales. **No implementar** ningún endpoint de "regenerar credenciales": no está en esta especificación.

**Cambio obligatorio en el primer login**
- Mientras `debe_cambiar_password = true`, el login responde `200` con `{ "debeCambiarPassword": true }` y un token **restringido**: solo habilita `POST /api/v1/auth/cambiar-password`.
- `POST /api/v1/auth/cambiar-password` · body `{ "passwordActual", "passwordNueva" }` · valida RN-15, apaga la bandera y emite el token normal.

**Criterios de aceptación**
1. La contraseña generada cumple RN-15 y no aparece en ningún log.
2. Hasta cambiar la contraseña, ningún otro endpoint es accesible con ese token.
3. El correo de credenciales llega y queda registrado en el outbox.
4. Al crear desde una solicitud, esta queda `APROBADA`.

**Gestión de solicitudes**
- `GET /api/v1/solicitudes-acceso?estado=PENDIENTE&page=&size=` · ADMIN — listado paginado (convención §8.1).
- `PATCH /api/v1/solicitudes-acceso/{id}/rechazar` · ADMIN.

### 3.2 ~~Verificación de cuenta~~ — ELIMINADO por CR-01 (D-18)

Las cuentas las crea el administrador ya activas; no hay verificación por correo. La tabla `token_verificacion` queda sin uso (se conserva para una eventual recuperación de contraseña, hoy fuera de alcance). **No implementar** los endpoints de verificación ni reenvío; si existen, se eliminan.

### 3.3 Login — CU-01

`POST /api/v1/auth/login` · público · body `{ "correo": "...", "password": "..." }`

**Comportamiento**
- Valida credenciales. Falla → `401 CREDENCIALES_INVALIDAS` (mensaje genérico, sin distinguir si el correo existe).
- `estado_cuenta != ACTIVA` → `403 CUENTA_INACTIVA`. (Renombrado desde `CUENTA_NO_VERIFICADA`: con D-18 las cuentas nacen `ACTIVA` y no hay verificación, así que el único caso real es una cuenta dada de baja por el SuperAdmin.)
- Emite JWT con `sub` (id de usuario), `rol` y expiración.

**Respuesta**
```json
{ "token": "...", "debeCambiarPassword": false,
  "usuario": { "id": 1, "nombreCompleto": "...", "rol": "ODONTOLOGO" } }
```
Si `debeCambiarPassword` es `true`, el token solo habilita el cambio de contraseña (§3.1.b).

**Criterios de aceptación**
1. Correo inexistente y contraseña incorrecta devuelven el mismo mensaje.
2. Una cuenta `INACTIVA` no obtiene token.
3. El JWT contiene el rol y se valida en cada request.

---

### 3.4 ~~Autenticación con Google~~ — ELIMINADO por CR-01 (D-17)

Se retira por pedido de la clienta: el acceso queda controlado por el flujo de solicitud + alta manual, que garantiza que quien entra es un odontólogo real. **Eliminar** el endpoint `/auth/google`, su servicio y el botón del frontend si ya existen. La columna `google_subject_id` permanece en la base sin uso.

### 3.5 Autorización — RN-14

Roles: `SUPERADMIN`, `ADMIN`, `ODONTOLOGO`.

**Todo endpoint lleva anotación de autorización.** Sin excepción. `SUPERADMIN` accede a todo lo de `ADMIN` más la gestión de usuarios y licencias (CU-17, CU-23).

`POST /api/v1/auth/logout` — CU-14 · autenticado.

---

### 3.6 Bloqueo por licencia — RN-20 / CU-23

**Patrón:** Intercepting Filter.

Filtro que se ejecuta antes de cualquier endpoint de negocio y consulta `v_licencia_vigente`.

- Sin licencia vigente → `423 LICENCIA_VENCIDA` con mensaje claro.
- **Excepciones al bloqueo:** `/api/v1/auth/login`, `/api/v1/licencias/**` (para que el SuperAdmin pueda regularizar), los endpoints de salud, y las rutas de Swagger `/swagger-ui/**` y `/v3/api-docs/**` (§1.1).
- El frontend intercepta el `423` y muestra una pantalla de bloqueo.

**Endpoints de licencia** (solo `SUPERADMIN`):
- `GET /api/v1/licencias` — listado histórico
- `GET /api/v1/licencias/vigente` — estado actual
- `POST /api/v1/licencias` — registrar período: `{ "fechaInicio", "fechaVencimiento", "observacion" }`

**Fuera de alcance:** planes, precios y pasarela de pago (P-11, P-12).

**Criterios de aceptación**
1. Con licencia vencida, ningún rol de negocio opera; el login sigue accesible.
2. Al registrar una licencia vigente, la operación se restablece sin pérdida de datos.
3. Un `ADMIN` no puede crear licencias.

---

## 4. Módulo Catálogos

### 4.1 Tipos de trabajo — CU-16 / RN-12 / RN-21

| Método | Ruta | Rol |
|---|---|---|
| GET | `/api/v1/tipos-trabajo/activos` | autenticado — **sin paginar** |
| GET | `/api/v1/tipos-trabajo` | ADMIN, SUPERADMIN — **paginado** |
| POST | `/api/v1/tipos-trabajo` | ADMIN, SUPERADMIN |
| PUT | `/api/v1/tipos-trabajo/{id}` | ADMIN, SUPERADMIN |
| PATCH | `/api/v1/tipos-trabajo/{id}/estado` | ADMIN, SUPERADMIN |

`GET /api/v1/tipos-trabajo?page=0&size=10&sort=nombre,asc&activo=&busqueda=`

Filtros opcionales: `activo` (true/false) y `busqueda` (coincidencia parcial del nombre, sin distinguir mayúsculas ni acentos).

> **Excepción deliberada:** la exención de paginado aplica a los endpoints que alimentan **selectores donde se necesita el catálogo completo** — hoy `/tipos-trabajo/activos` (§5.1) y `/odontologos/activos` (§5.1, D-19). *(Alcance ampliado el 23/08/2026 por el desarrollador: antes la exención nombraba solo al primero. Paginar un selector no es una limitación sino un defecto — con 31 odontólogos el administrador no podría registrar la orden del último —, y acumular páginas en el cliente lo prohíbe `Agente.md` §6.2. El fundamento es idéntico al del catálogo de trabajos.)*

**Request (POST/PUT)**
```json
{ "nombre": "PLACA ACTIVA", "diasEstimados": 7, "precio": 250.00 }
```

**Validaciones**
- `nombre` único, obligatorio → `409 TIPO_TRABAJO_DUPLICADO`
- `diasEstimados >= 7` → `422 DIAS_ESTIMADOS_INSUFICIENTES` (RN-12)
- `precio >= 250` → `422 PRECIO_INSUFICIENTE` (RN-21)

**Desactivación (CU-16 A1):** `activo = false` no borra el registro; el tipo deja de ofrecerse en nuevas órdenes pero las existentes conservan su referencia. **No se permite eliminar** tipos de trabajo.

**Criterios de aceptación**
1. Un tipo con 6 días o precio 249 es rechazado.
2. Al desactivar un tipo usado, las órdenes existentes siguen mostrándolo correctamente.
3. Un odontólogo solo ve tipos activos, y los recibe completos desde `/activos`.
4. `GET /tipos-trabajo?size=15` es rechazado con `400`.
5. Con 45 tipos y `size=10`, la respuesta trae `totalPaginas: 5` y 10 elementos.

---

### 4.2 Estados — CU-22 / RN-04

| Método | Ruta | Rol |
|---|---|---|
| GET | `/api/v1/estados` | autenticado (sin paginar: catálogo cerrado de 7) |
| PUT | `/api/v1/estados/{id}` | ADMIN, SUPERADMIN |

Solo se permite editar `nombre` y `descripcion`. **Prohibido** modificar `codigo`, `orden_secuencia`, `es_terminal` o `es_productivo`, ni crear o eliminar estados: el flujo lineal de RN-04 depende de ellos.

---

### 4.3 Tipos de orden — RN-11

`GET /api/v1/tipos-orden` · autenticado · solo lectura.

Devuelve `codigo`, `nombre` y `recargoMonto`. No hay endpoints de escritura: el comportamiento diferencial es configuración del sistema.

---

## 5. Módulo Órdenes

### 5.1 Crear orden — CU-09

`POST /api/v1/ordenes` · rol `ADMIN`, `SUPERADMIN` — **D-19: por ahora las órdenes las registra el laboratorio** (el odontólogo la envía en papel o por teléfono, como en CU-05 del análisis original)

**Request**
```json
{
  "odontologoId": 3,
  "pacienteNombre": "Martín Pérez",
  "fechaIngreso": "2026-08-06",
  "tipoTrabajoId": 16,
  "tipoOrdenCodigo": "NORMAL",
  "descripcion": "Disyuntor superior, tornillo de expansión 7mm"
}
```

**Validaciones**
- `odontologoId`, `pacienteNombre`, `fechaIngreso`, `tipoTrabajoId`, `tipoOrdenCodigo` obligatorios. `descripcion` opcional.
- `odontologoId` debe ser un usuario ODONTOLOGO activo → `422 ODONTOLOGO_INVALIDO`.
- Tipo de trabajo existente y activo → `422 TIPO_TRABAJO_INACTIVO`.
- `tipoOrdenCodigo` ∈ {`NORMAL`, `URGENTE`}.

**Comportamiento — cada paso referenciado**

1. `codigo` de orden: generado por la secuencia (`LG-XXXX`).
2. `paciente_iniciales`: **derivadas por el backend** del nombre — primera letra de cada palabra, en mayúscula, separadas por punto (`Martín Pérez` → `M.P.`). RN-22.
3. `paciente_codigo`: secuencia autoincremental. RN-22.
4. `dias_estimados_aplicados` ← `tipo_trabajo.dias_estimados` (foto del valor actual).
5. `precio_base` ← `tipo_trabajo.precio` (foto). RN-21.
6. `recargo_urgencia` ← `tipo_orden.recargo_monto`. RN-11.
7. `estado_id` ← `tipo_orden.estado_inicial_id`. **Leído de la tabla, no codificado.** RN-11.
8. `fecha_estimada_entrega` = `fecha_ingreso` + `dias_estimados_aplicados` **días hábiles**, excluyendo sábados y domingos. RN-18.
   > Feriados **no** se contemplan (S-05 sin resolver). No implementar tabla de feriados.
9. Inserta el registro inicial en `orden_historial_estado` con `usuario_id = null` (asignado por el sistema).
10. Publica evento `OrdenCreadaEvent` → notificación al **odontólogo dueño** de que su orden fue registrada (correo + Telegram, D-20/D-21). La notificación al admin por nueva orden (RN-19) pierde sentido cuando el creador es el propio admin: se emite solo si el creador no es el destinatario (previsto para cuando P-19 reabra la creación por el odontólogo).

> **D-19 / P-19:** la pantalla "Nueva orden" del odontólogo se retira de la navegación y el endpoint no acepta el rol ODONTOLOGO. El flujo documentado del CU-09 original se conserva como referencia para cuando la clienta decida reabrirlo.

**Respuesta** `201` con la orden creada en formato público (sin `pacienteNombre`).

**Criterios de aceptación**
1. Una orden `URGENTE` nace en `EN_EVALUACION` con recargo 200; una `NORMAL` en `RECIBIDO` con recargo 0.
2. `precioTotal` = `precioBase` + `recargoUrgencia`, calculado por la base de datos.
3. La fecha estimada de una orden creada el viernes con 7 días hábiles cae el martes de la semana siguiente.
4. La respuesta no contiene el nombre completo del paciente.
5. Cambiar el precio del catálogo después no altera la orden creada.

---

### 5.2 Adjuntar archivos — RN-13

`POST /api/v1/ordenes/{id}/archivos` · rol `ADMIN`, `SUPERADMIN` u `ODONTOLOGO` propietario · `multipart/form-data` (D-19: hoy los adjuntos los carga el laboratorio al registrar la orden)

**Validaciones (backend, obligatorias)**
- Imagen: `image/jpeg` o `image/png`, ≤ 5 MB.
- Documento: `application/pdf` o DOCX, ≤ 8 MB.
- Fuera de rango → `422 ARCHIVO_NO_PERMITIDO` indicando formato o tamaño.
- La orden debe pertenecer al odontólogo autenticado (RN-01).

**Puerto `AlmacenamientoArchivos`** con `AlmacenamientoLocal` como única implementación (Agente.md 5.5). El binario **no** se guarda en la base: `orden_archivo` almacena ruta y metadatos.

`GET /api/v1/ordenes/{id}/archivos` — listado. `GET /api/v1/archivos/{id}` — descarga con verificación de propiedad.

**Eliminación de un adjunto — decisión del desarrollador (19/08/2026)**

`DELETE /api/v1/archivos/{id}` · rol `ADMIN`, `SUPERADMIN`

Resuelve el caso operativo de un archivo cargado por error. **El odontólogo no puede borrar adjuntos**, ni siquiera de sus propias órdenes: recibe `403`. No es un caso de RN-01 —no se le niega por ser ajena, sino por su rol— así que acá **no aplica** la regla de responder `404`.

- Borrado **definitivo**: se elimina el registro de `orden_archivo` y el binario del almacenamiento. No hay baja lógica; la tabla no tiene columna para eso y no se agrega.
- Archivo inexistente → `404 ARCHIVO_NO_ENCONTRADO`.
- Respuesta `204` sin cuerpo.

> **Fuera de alcance:** no se registra traza de quién borró ni cuándo. El modelo no tiene dónde guardarla y auditoría de borrados no está documentada.

**Criterios de aceptación**
1. Un JPG de 6 MB es rechazado por el backend aunque el frontend lo permita.
2. Un GIF es rechazado.
3. Un odontólogo no puede descargar el archivo de una orden ajena.
4. El admin borra un adjunto y desaparecen tanto el registro como el binario; un odontólogo que intenta borrar recibe `403`, incluso sobre una orden propia.

---

### 5.3 Listar mis órdenes — CU-03 / RN-01

`GET /api/v1/ordenes?estado=&historico=&page=&size=` · rol `ODONTOLOGO`

**Filtra siempre por el usuario autenticado.** El id de odontólogo **no** se acepta como parámetro.

**`historico`** *(agregado el 23/08/2026 por el desarrollador, con T-27)* — `true` deja solo las
órdenes ya cerradas, que es lo que consume el historial de CU-12 (§8). Por defecto es `false` y el
listado se comporta como siempre.

- **"Cerrada" es lo que dice `estado.es_terminal`**, que es donde RN-04 define el fin del flujo —
  hoy `ENTREGADO` y `CANCELADO`. No hay ninguna lista de estados terminales escrita en el código:
  si el catálogo marca otra etapa como terminal, el historial la incluye sola.
- **El filtro entre entregadas y canceladas es el `estado` que ya existía**, no un parámetro nuevo:
  `?historico=true&estado=CANCELADO` deja las canceladas. Los dos filtros se combinan.
- Va como filtro del mismo endpoint y no como recurso aparte porque es **el mismo listado del mismo
  dueño** con una condición más; duplicar la ruta duplicaría también RN-01.

**Respuesta por ítem** — nunca `pacienteNombre`:
```json
{
  "id": 1,
  "codigo": "LG-0001",
  "pacienteIdentificacion": "M.P. - Caso #1000",
  "tipoTrabajo": "DISYUNTOR CON TORNILLO ESTANDAR",
  "tipoOrden": "Normal",
  "estado": "En producción",
  "fechaIngreso": "2026-08-06",
  "fechaEstimadaEntrega": "2026-08-17",
  "precioTotal": 250.00
}
```

---

### 5.4 Detalle y seguimiento — CU-04

`GET /api/v1/ordenes/{id}` · `ODONTOLOGO` (propietario), `ADMIN`, `SUPERADMIN`

**RN-01:** si un odontólogo pide una orden ajena → `404`, no `403` (no revelar existencia).

Incluye los datos del listado más `descripcion`, `precioBase`, `recargoUrgencia`, archivos, y la **línea de tiempo**: lista de etapas alcanzadas desde `orden_historial_estado` con estado, fecha/hora y autor.

El `ADMIN` sí recibe `pacienteNombre` (lo necesita para operar); el `ODONTOLOGO` **no** (RN-22).

**`siguienteEstado`** *(agregado el 23/08/2026 por el desarrollador, con T-26)* — la única transición que la orden admite en ese momento, con `codigo` y `nombre`:

```json
{ "siguienteEstado": { "codigo": "CONTROL_CALIDAD", "nombre": "Control de calidad" } }
```

- **Es `null` cuando no hay transición posible**, que es el caso de los estados terminales `ENTREGADO` y `CANCELADO`.
- Lo calcula el backend con la regla de RN-04 —`orden_secuencia` inmediatamente superior—, la misma que valida §5.5. **§8 lo exige así:** *"ningún cálculo de negocio en el cliente: precios, fechas y transiciones vienen del backend"*. Con esto la pantalla de administración dibuja su botón de avance sin reimplementar RN-04.
- Viaja el **nombre** además del código porque `estado.nombre` es editable (CU-22): derivarlo del código en el cliente rompería la pantalla al renombrar una etapa.

**Criterios de aceptación**
1. La línea de tiempo muestra cada etapa con fecha y hora.
2. Un odontólogo recibe 404 al pedir una orden ajena.
3. La respuesta al odontólogo no contiene el nombre del paciente.

---

### 5.5 Cambiar estado — CU-06 / RN-04

`PATCH /api/v1/ordenes/{id}/estado` · rol `ADMIN`, `SUPERADMIN` · body `{ "estadoCodigo": "EN_PRODUCCION" }`

**Patrón:** State.

**Transiciones permitidas — únicamente hacia adelante:**

```
RECIBIDO → EN_EVALUACION → EN_PRODUCCION → CONTROL_CALIDAD → LISTO → ENTREGADO
```

- Solo se permite avanzar al estado con `orden_secuencia` inmediatamente superior.
- **Retrocesos prohibidos** (P-02 sin resolver) → `409 TRANSICION_NO_PERMITIDA`.
- Desde `ENTREGADO` o `CANCELADO` no hay transición → `409`.
- El `ADMIN` **no** puede cancelar (RN-17/CU-20: la cancelación es del odontólogo).

**En la misma transacción:** actualiza `orden.estado_id`, inserta en `orden_historial_estado` con el usuario autor, y publica `EstadoOrdenCambiadoEvent`.

**Criterios de aceptación**
1. Saltar de `RECIBIDO` a `LISTO` es rechazado.
2. Retroceder de `CONTROL_CALIDAD` a `EN_PRODUCCION` es rechazado.
3. Cada transición deja su registro fechado en el historial.
4. Si la transacción falla, no se envía notificación.

---

### 5.6 Cancelar orden — CU-20 / RN-17

`PATCH /api/v1/ordenes/{id}/cancelar` · rol `ODONTOLOGO` (propietario)

**Comportamiento**
- Verifica propiedad (RN-01).
- Si el estado es terminal (`ENTREGADO` o `CANCELADO`) → `409 ORDEN_NO_CANCELABLE`.
- Pasa a `CANCELADO`, sella `fecha_cancelacion`, registra en el historial.

**Fuera de alcance (P-14):** **no** calcular ni asignar `cargo_cancelacion`, y **no** restringir la cancelación según la etapa. La columna queda en `null`.

**RN-17:** **no existe** endpoint de edición de orden para el odontólogo. No crear `PUT /ordenes/{id}`.

**Criterios de aceptación**
1. No existe ningún endpoint que permita al odontólogo modificar una orden creada.
2. Una orden entregada no puede cancelarse.
3. `cargo_cancelacion` permanece nulo.

---

### 5.7 Administración de órdenes — CU-10

`GET /api/v1/admin/ordenes?estado=&tipoOrden=&odontologoId=&page=&size=` · `ADMIN`, `SUPERADMIN`

`GET /api/v1/admin/dashboard` devuelve:
- Contadores: en curso, listas para retirar, entregadas esta semana, urgentes activas.
- Distribución por estado (vista `v_ordenes_por_estado`).
- Próximas a entregar, ordenadas por `fecha_estimada_entrega`.
- Órdenes recientes.

**Órdenes urgentes:** vista `v_ordenes_urgentes`.

**RN-22 en el bloque de urgentes:** `v_ordenes_urgentes` **incluye `paciente_nombre`** y la
respuesta **no lo expone**, igual que ningún otro listado (`Agente.md` §8.2). La consulta ni
siquiera lo selecciona. El laboratorio ve ese dato solo en el detalle de la orden (§5.4).

#### Panel del odontólogo — CU-02

*(Agregado el 23/08/2026 por el desarrollador, con T-27.)*

`GET /api/v1/dashboard` · rol `ODONTOLOGO` devuelve:
- Contadores: en curso, listas para retirar, entregadas esta semana.
- Órdenes recientes.

**RN-01:** el dueño sale del token y el endpoint **no acepta ningún parámetro**, así que no hay
forma de pedir el panel de otro. **RN-22:** las órdenes recientes identifican al paciente por
iniciales y código.

**No devuelve el contador de "mensajes nuevos"** que enumera CU-02: D-11 pospuso la mensajería.

Existe porque §8 prohíbe que el cliente calcule nada y sin él "el panel muestra sus contadores" no
sería verificable — el mismo criterio con el que T-32b creó `GET /perfil`.

#### Definiciones de los contadores

Valen para los dos paneles. Ninguna estaba en la spec y las fijó el desarrollador el 23/08/2026:

- **En curso:** órdenes con `estado.es_terminal = false` **y estado distinto de `LISTO`**. Se
  excluye `LISTO` para que "en curso" y "listas para retirar" no cuenten dos veces el mismo
  trabajo: si un trabajo listo apareciera en los dos, quien lee "3 en curso, 1 listo" no sabría si
  tiene 3 o 4 trabajos abiertos.
- **Listas para retirar:** órdenes en la etapa `LISTO`.
- **Entregadas esta semana:** órdenes cuyo **pasaje a `ENTREGADO` en `orden_historial_estado`**
  cayó en la semana en curso. La tabla `orden` no tiene fecha de entrega real, así que el dato solo
  existe en el historial. La semana es la calendario, **de lunes a domingo**, y el rango es
  semiabierto `[lunes 00:00, lunes siguiente 00:00)`.
  - **El corte se calcula en la zona horaria del laboratorio, no en UTC.** `fecha_hora` es
    `TIMESTAMPTZ`: con el corte en UTC, un trabajo entregado el domingo a las 21:00 en Montevideo
    caería en la semana siguiente. La zona vive en **`app.laboratorio.zona-horaria`**, que es
    configuración de instalación y se documenta en **§1.2**.
- **Urgentes activas** *(solo el dashboard del laboratorio)*: las filas de `v_ordenes_urgentes`,
  que ya define urgente activa como tipo `URGENTE` con estado no terminal. **Se solapa a propósito
  con "en curso"**: es el mismo conjunto mirado por otro corte.

Los cuatro **no forman una partición** y no se pretende que sumen un total.

#### Bloques de resumen

"Próximas a entregar", "órdenes recientes" y "urgentes" traen **hasta 5 filas** cada uno. **No son
listados paginados de §8.1**: el panel es un vistazo, y quien quiera la lista completa va a "Mis
trabajos" (§5.3) o a "Órdenes" (§5.7). "Recientes" ordena por `fecha_ingreso` descendente.

**Sin reportes ni estadísticas** más allá de estos contadores: CU-13 es Fase 4.

---

## 6. Módulo Notificaciones

### 6.1 Arquitectura

**Patrones:** Observer (eventos de Spring), Strategy + Adapter (canales), Transactional Outbox (`notificacion` + `notificacion_envio`).

**Flujo**

1. Un módulo de negocio publica un evento (`OrdenCreadaEvent`, `EstadoOrdenCambiadoEvent`, `CuentaCreadaEvent`).
2. El listener corre con `@TransactionalEventListener(phase = AFTER_COMMIT)`.
3. Crea la `notificacion` y un `notificacion_envio` con estado `PENDIENTE` por cada canal activo.
4. Un proceso programado (`@Scheduled`) toma los pendientes y los despacha por su `CanalNotificacion`.
5. Éxito → `ENVIADO` con fecha. Fallo → `FALLIDO` con `detalle_error`, reintentable.

### 6.2 Eventos y destinatarios

| Evento | `tipo_evento` | Destinatario | Canales | Regla |
|---|---|---|---|---|
| Solicitud de acceso | `SOLICITUD_ACCESO` | Administrador | app + correo + Telegram | D-17 |
| Credenciales creadas | `CREDENCIALES_CREADAS` | Odontólogo | correo | D-18 |
| Orden registrada | `NUEVA_ORDEN` | Odontólogo dueño (ver §5.1 paso 10) | correo + Telegram | D-19 |
| Orden urgente creada | `ORDEN_URGENTE` | Administrador | app + correo + Telegram | RN-11 |
| Cambio de estado | `CAMBIO_ESTADO` | Odontólogo dueño | app + **correo + Telegram** | RN-05, CU-07, **D-20/D-21** |

`CUENTA_CREADA` queda sin uso (era del auto-registro).

**Texto de `CAMBIO_ESTADO`** (formato documentado en CU-07):
> `El trabajo del paciente Código {paciente_codigo} pasó a la etapa de {estado}.`

**No inventar otros eventos.** La cancelación **no** notifica (S-08 sin resolver).

### 6.3 Canales

Puerto `CanalNotificacion` con `soporta(canal)` y `enviar(notificacion)`.

| Adaptador | Estado |
|---|---|
| `CanalApp` | Implementado — solo marca `ENVIADO` (la notificación ya está en base para la campana) |
| `CanalCorreo` | Implementado — SMTP configurable por properties |
| `CanalTelegram` | **IMPLEMENTADO (D-21).** Bot API oficial de Telegram: `POST https://api.telegram.org/bot{token}/sendMessage` con `chat_id` y `text`. El token del bot va en properties (`telegram.bot.token`), nunca en el código. Si el destinatario no tiene `telegram_chat_id` (no se vinculó), el envío se marca `FALLIDO` con "Telegram no vinculado" — no es un error del sistema. |
| `CanalWhatsApp` | **Estructura únicamente** (P-18, D-21): implementa el puerto, valida `telefono`, marca `FALLIDO` con "canal no configurado". Activable a futuro con Meta Cloud API o Twilio sin tocar el resto. |

Los canales activos salen de `configuracion_notificacion` del destinatario (RN-19). Sin configuración: **app + correo + Telegram** para todos (D-20/D-21). El envío Telegram solo se intenta si el usuario está vinculado; si no, se registra `FALLIDO` con "Telegram no vinculado" (visible para diagnóstico, sin reintentos automáticos hasta la vinculación).

### 6.4 Endpoints

| Método | Ruta | Rol |
|---|---|---|
| GET | `/api/v1/notificaciones?leidas=false&page=&size=` | autenticado (propias) |
| GET | `/api/v1/notificaciones/contador` | autenticado |
| PATCH | `/api/v1/notificaciones/{id}/leer` | autenticado (propia) |
| PATCH | `/api/v1/notificaciones/leer-todas` | autenticado |
| GET | `/api/v1/configuracion-notificaciones` | ADMIN, SUPERADMIN |
| PUT | `/api/v1/configuracion-notificaciones` | ADMIN, SUPERADMIN |

**Campana (frontend):** `useQuery` con `refetchInterval` de 60 s sobre `/contador`. **No implementar WebSocket ni SSE** en esta versión.

**Validación (CU-21):** si `canalTelegramActivo = true`, `telegramChatId` es obligatorio → `422 TELEGRAM_SIN_DESTINO`.

**Criterios de aceptación de §6** (los criterios 1 y 2 los verifica T-21; los criterios 3 y 4, T-22)
1. Un cambio de estado genera exactamente una notificación con envío por cada canal activo.
2. Si el correo falla, el envío queda `FALLIDO` y la notificación sigue visible en la app.
3. Un usuario solo ve sus propias notificaciones.
4. Activar Telegram sin `chatId` es rechazado.

### 6.5 Vinculación de Telegram — D-21

Un bot de Telegram **no puede iniciar** una conversación: el usuario debe escribirle primero. El flujo de vinculación, de un solo paso para el usuario:

1. En su perfil, el usuario ve el estado ("Telegram: no vinculado") y un botón **"Conectar Telegram"**.
2. `POST /api/v1/telegram/vinculacion` (autenticado) genera un token corto de un solo uso (`telegram_token_vinculacion`) y devuelve el enlace profundo: `https://t.me/{nombre_del_bot}?start={token}`.
3. El usuario abre el enlace y toca **Iniciar**: Telegram envía al bot `/start {token}`.
4. El backend recibe la actualización (**polling con `getUpdates` mediante `@Scheduled`** — no usar webhook: exige HTTPS público y complica la instalación por laboratorio, D-16), busca el token, y si es válido y no usado: guarda el `chat_id` en `usuario.telegram_chat_id`, marca `telegram_vinculado = true`, sella `fecha_uso` y responde por el bot "✅ Cuenta vinculada. Vas a recibir las notificaciones del laboratorio por acá."
5. El perfil pasa a mostrar "Telegram: vinculado ✅" con opción de desvincular (`DELETE /api/v1/telegram/vinculacion` → limpia `chat_id` y bandera).

**Configuración requerida (P-20, tarea del desarrollador):** crear el bot con @BotFather y setear `telegram.bot.token` y `telegram.bot.username` en las properties del ambiente. **El agente no inventa tokens**: sin configuración, el canal y la vinculación quedan deshabilitados con mensaje claro.

**Criterios de aceptación**
1. Un usuario vinculado recibe por Telegram cada notificación que le corresponda, en segundos.
2. Un token de vinculación usado o inexistente no vincula y el bot responde el error.
3. Desvincular detiene los envíos (quedan `FALLIDO` con "Telegram no vinculado") sin afectar correo ni campana.
4. El token del bot no aparece en el código ni en los logs.

---

## 7. Módulo Usuarios (administración)

| Método | Ruta | Rol | Caso de uso |
|---|---|---|---|
| GET | `/api/v1/odontologos` | ADMIN, SUPERADMIN — **paginado** | CU-11 |
| GET | `/api/v1/odontologos/activos` | ADMIN, SUPERADMIN — **sin paginar** | §5.1 (D-19) |
| GET | `/api/v1/usuarios` | SUPERADMIN | CU-17 |
| PATCH | `/api/v1/usuarios/{id}/estado` | SUPERADMIN | CU-17 |
| GET | `/api/v1/perfil` | autenticado | — |
| PUT | `/api/v1/perfil` | autenticado | — |
| GET | `/api/v1/dashboard` | ODONTOLOGO — **sin parámetros** (RN-01) | CU-02 |
| GET | `/api/v1/admin/dashboard` | ADMIN, SUPERADMIN | CU-10 |

En `/perfil` el usuario edita `nombreCompleto` y `direccion`. **No** puede cambiar su rol ni su correo.

> **Los dos paneles se especifican en §5.7**, que es donde vive el contenido de CU-10 y donde se
> agregó el de CU-02. Figuran acá porque esta tabla es el índice de endpoints del sistema.

> **Los dos listados de odontólogos son distintos y conviven.** `/odontologos` es la tabla administrable de CU-11: paginada (§8.1 Regla 2), con los datos de cada cuenta y acciones sobre ella. `/odontologos/activos` alimenta el **selector** de odontólogo al registrar una orden (§5.1, D-19): sin paginar —por la exención de §4—, solo cuentas `ACTIVA`, y devuelve únicamente `id` y `nombreCompleto`, que es lo único que un selector necesita.

---

## 8. Frontend — pantallas

| Pantalla | Ruta | Rol | Caso de uso |
|---|---|---|---|
| Login | `/login` | público | CU-01 |
| Solicitud de acceso | `/solicitar-acceso` | público | D-17 |
| Cambio de contraseña obligatorio | `/cambiar-password` | autenticado (bandera) | D-18 |
| Panel odontólogo | `/inicio` | ODONTOLOGO | CU-02 |
| Mis órdenes | `/ordenes` | ODONTOLOGO | CU-03 |
| Detalle y seguimiento | `/ordenes/:id` | ODONTOLOGO | CU-04 |
| Historial | `/historial` | ODONTOLOGO | CU-12 |
| Perfil | `/perfil` | autenticado | — |
| Dashboard admin | `/admin` | ADMIN | CU-10 |
| Órdenes (admin) | `/admin/ordenes` | ADMIN | CU-06 |
| Nueva orden (admin) | `/admin/ordenes/nueva` | ADMIN | CU-05 / D-19 |
| Solicitudes de acceso | `/admin/solicitudes` | ADMIN | D-17 |
| Odontólogos | `/admin/odontologos` | ADMIN | CU-11 |
| Tipos de trabajo | `/admin/tipos-trabajo` | ADMIN | CU-16 |
| Configuración | `/admin/configuracion` | ADMIN | CU-21 |
| Licencias | `/admin/licencias` | SUPERADMIN | CU-23 |
| Licencia vencida | `/bloqueado` | cualquiera | RN-20 |

**Referencia visual:** los mockups del PDF original (login, panel odontólogo, seguimiento, dashboard admin). Respetar la estructura de navegación; el detalle visual queda a criterio del desarrollador.

**Menú odontólogo:** Inicio · Mis trabajos · Historial · Perfil. ("Nueva orden" retirada por D-19; ver P-19.)
**El ítem "Mensajes" NO se incluye** (D-11, pospuesto).

**Menú admin:** Dashboard · Trabajos · Odontólogos · Solicitudes · Tipos de trabajo · Configuración.
**No incluir** Pacientes (S-03 sin resolver), Calendario, Mensajes, Reportes ni Facturación (P-08).

**Reglas transversales del frontend**
- Responsive: usable en celular, tablet y computadora, sin instalación.
- Ninguna pantalla muestra el nombre completo del paciente al odontólogo (RN-22).
- Ningún cálculo de negocio en el cliente: precios, fechas y transiciones vienen del backend.
- La línea de tiempo del seguimiento se arma con el historial que devuelve la API.

### 8.1 Convención obligatoria para toda vista CRUD

**Aplica a TODAS las pantallas de administración con alta, edición y listado**, sin excepción: tipos de trabajo, estados, odontólogos, usuarios, licencias y cualquier CRUD futuro. El objetivo es que todas se vean y se operen igual.

#### Regla 1 — Listado y formulario en vistas separadas

**Prohibido el formulario embebido sobre la tabla.** Cada recurso tiene tres rutas:

| Ruta | Propósito |
|---|---|
| `/admin/{recurso}` | Listado paginado. **Sin formulario.** |
| `/admin/{recurso}/nuevo` | Formulario de alta |
| `/admin/{recurso}/{id}/editar` | Formulario de edición |

- En el listado, un botón **"Nuevo"** arriba a la derecha navega a `/nuevo`.
- La acción **"Editar"** de cada fila navega a `/{id}/editar`.
- Alta y edición usan **el mismo componente de formulario**, parametrizado por modo. No duplicar.
- Al guardar con éxito: volver al listado y mostrar confirmación. Al cancelar: volver sin guardar.
- El formulario muestra las reglas aplicables como texto de ayuda bajo cada campo (ej.: "RN-12: mínimo 7 días hábiles").

#### Regla 2 — Paginación controlada por el backend

- Controles **debajo de la tabla**: página actual y total, anterior/siguiente, y selector de cantidad con **10, 20 y 30**.
- El estado de paginación vive en la **URL** (`?page=0&size=10`), para que la vista sea compartible y sobreviva a un refresco.
- Cambiar el tamaño de página vuelve a `page=0`.
- **Prohibido** traer toda la colección y paginar con `slice` en el cliente.
- Se consume con TanStack Query, manteniendo los datos previos visibles mientras carga la página siguiente (sin parpadeo).

#### Regla 3 — Estados de la tabla

Toda tabla contempla explícitamente: **cargando** (esqueleto o indicador, sin salto de layout), **vacío** (mensaje claro y acceso a "Nuevo"), y **error** (mensaje con opción de reintentar). Un listado que solo contempla el caso feliz está incompleto.

#### Regla 4 — Componentes compartidos

Para que la uniformidad no dependa de la disciplina, estos componentes viven en `shared/` y se reutilizan:

| Componente | Responsabilidad |
|---|---|
| `TablaPaginada` | Tabla + controles de paginación + estados de carga, vacío y error |
| `ControlesPaginacion` | Navegación y selector de tamaño (10/20/30) |
| `LayoutFormulario` | Encabezado, cuerpo, botones Guardar y Cancelar |
| `CampoFormulario` | Etiqueta, control, texto de ayuda y mensaje de error |
| `usePaginacion` | Hook que sincroniza `page` y `size` con la URL |

Una pantalla CRUD nueva se arma componiendo estos elementos. **Si una vista necesita algo que estos componentes no cubren, se extiende el componente compartido — no se resuelve con una solución propia de esa pantalla.**

#### Regla 5 — Uniformidad visual

Misma disposición en todas: título, botón "Nuevo" a la derecha, filtros si corresponde, tabla, paginación. Columna de acciones siempre al final. Mismos textos: "Nuevo", "Editar", "Desactivar", "Guardar", "Cancelar".

#### Criterios de aceptación

1. Ninguna vista de listado contiene un formulario de alta o edición.
2. El listado muestra 10 registros por defecto y permite cambiar a 20 o 30.
3. Recargar el navegador en `?page=2&size=20` mantiene la misma página.
4. La cantidad de registros que llega por la red coincide con el tamaño de página (verificable en la pestaña Red).
5. Las tablas de dos recursos distintos son visualmente indistinguibles en estructura.

---

## 9. Trazabilidad — regla de negocio → implementación

| Regla | Dónde se implementa |
|---|---|
| RN-01 | Filtro por usuario autenticado en todas las consultas de órdenes; 404 ante orden ajena |
| RN-03 / RN-22 | `pacienteIdentificacion` en toda respuesta al odontólogo |
| RN-04 | Transiciones lineales en el servicio de órdenes (State) |
| RN-05 | Evento + outbox + canales app, correo y Telegram (D-20/D-21) |
| RN-11 | `tipo_orden`: estado inicial, `notifica_admin`, `recargo_monto` |
| RN-12 | Validación `diasEstimados >= 7` |
| RN-13 | Validación de formato y tamaño en el backend |
| RN-14 | `@PreAuthorize` en cada endpoint |
| RN-15 | Validador de contraseña en el alta por el admin y en el cambio obligatorio (§3.1.b) |
| RN-16 | **Reemplazada por D-17/D-18:** solicitud de acceso pública (§3.1) + alta por el administrador con contraseña autogenerada y cambio obligatorio (§3.1.b). Sin auto-registro, sin Google, sin verificación por correo |
| RN-17 | Ausencia de endpoint de edición |
| RN-18 | Cálculo de días hábiles al crear la orden |
| RN-19 | `configuracion_notificacion` + selección de canales |
| RN-20 | Filtro de licencia |
| RN-21 | Validación `precio >= 250`; congelado en `precio_base` |

---

## 10. Definición de terminado

Una tarea está terminada cuando:

1. Cumple todos sus criterios de aceptación.
2. No introduce nada fuera de este documento.
3. Los patrones aplicados están declarados según `Agente.md` 7.1.
4. Los endpoints nuevos tienen autorización por rol.
5. Los endpoints nuevos están documentados en Swagger según §1.1 y **se verificaron desde la UI**.
6. Si toca órdenes: RN-01 verificado y el nombre del paciente no se expone.
7. Se reportó con el formato de `Agente.md` 4.1 y **se recibió confirmación**.
