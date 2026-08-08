# spec.md — Especificación técnica

**Proyecto:** Lab. Garcia's Connect
**Versión:** 1.0 — 08/08/2026
**Base:** Análisis v2.2 + Modelo de datos v1.2 + esquema `01_labgarcias_schema.sql`

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
- Paginación: `?page=0&size=20`, respuesta `{ "contenido": [], "total": n, "pagina": 0 }`

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

### 3.1 Registro de odontólogo — CU-18

`POST /api/v1/auth/registro` · público

**Request**
```json
{
  "nombreCompleto": "Dr. Juan Pérez",
  "correo": "juan@mail.com",
  "nombreUsuario": "jperez",
  "password": "*38Op5)l6",
  "direccion": "Av. 18 de Julio 1234"
}
```

**Validaciones**
- Todos los campos obligatorios (RN-16).
- `correo` con formato válido y no registrado → `409 CORREO_YA_REGISTRADO`.
- `nombreUsuario` no registrado → `409 USUARIO_YA_REGISTRADO`.
- `password` cumple RN-15: mínimo 9 caracteres, al menos una mayúscula, una minúscula, un número y un carácter especial → `400 PASSWORD_INVALIDA` indicando los requisitos.

**Comportamiento**
- Crea `usuario` con `rol = ODONTOLOGO`, `proveedor_auth = LOCAL`, `estado_cuenta = PENDIENTE_VERIFICACION`, `correo_verificado = false`.
- Hash con BCrypt.
- Genera token de verificación y dispara CU-19.

**Respuesta** `201` — `{ "mensaje": "Cuenta creada. Revisá tu correo para confirmarla." }`
**Nunca** devuelve el token ni datos del usuario.

**Criterios de aceptación**
1. Contraseña que no cumple RN-15 → rechazada con detalle.
2. La cuenta creada no puede iniciar sesión hasta verificarse.
3. La contraseña nunca aparece en logs ni en la respuesta.

---

### 3.2 Verificación de cuenta — CU-19 / D-02

`GET /api/v1/auth/verificar?token={token}` · público

**Comportamiento**
- Busca en `token_verificacion`. Debe existir, no estar usado (`fecha_uso IS NULL`) y no estar vencido.
- Válido → marca `fecha_uso`, pone `usuario.estado_cuenta = ACTIVA` y `correo_verificado = true`.
- Vencido o usado → `400 TOKEN_INVALIDO`.

`POST /api/v1/auth/reenviar-verificacion` · público · body `{ "correo": "..." }`
- Invalida tokens previos y emite uno nuevo. Responde `200` genérico **siempre**, exista o no el correo (no revelar cuentas).

**Frontend:** página `/verificar` que consume el enlace y muestra éxito o error con opción de reenvío.

**Criterios de aceptación**
1. El token funciona una sola vez.
2. Un token de más de 24 h es rechazado.
3. La respuesta de reenvío es idéntica exista o no la cuenta.

---

### 3.3 Login — CU-01

`POST /api/v1/auth/login` · público · body `{ "correo": "...", "password": "..." }`

**Comportamiento**
- Valida credenciales. Falla → `401 CREDENCIALES_INVALIDAS` (mensaje genérico, sin distinguir si el correo existe).
- `estado_cuenta != ACTIVA` → `403 CUENTA_NO_VERIFICADA`.
- Emite JWT con `sub` (id de usuario), `rol` y expiración.

**Respuesta**
```json
{ "token": "...", "usuario": { "id": 1, "nombreCompleto": "...", "rol": "ODONTOLOGO" } }
```

**Criterios de aceptación**
1. Correo inexistente y contraseña incorrecta devuelven el mismo mensaje.
2. Cuenta sin verificar no obtiene token.
3. El JWT contiene el rol y se valida en cada request.

---

### 3.4 Autenticación con Google — RN-16

`POST /api/v1/auth/google` · público · body `{ "idToken": "..." }`

**Comportamiento**
- Valida el token con Google y obtiene `sub`, correo y nombre.
- Si existe usuario con ese `google_subject_id` → login.
- Si no existe → crea `usuario` con `proveedor_auth = GOOGLE`, `google_subject_id`, `estado_cuenta = ACTIVA` y `correo_verificado = true` (CU-19 A2: el proveedor ya verificó el correo), `password_hash = null`.
- `direccion` queda vacía; el odontólogo la completa en su perfil.

**Criterios de aceptación**
1. Un usuario Google no requiere verificación por correo.
2. No se crea contraseña para usuarios Google.
3. Un correo ya registrado como LOCAL no se duplica → `409 CORREO_YA_REGISTRADO`.

---

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
| GET | `/api/v1/tipos-trabajo` | autenticado (solo activos) |
| GET | `/api/v1/tipos-trabajo/todos` | ADMIN, SUPERADMIN |
| POST | `/api/v1/tipos-trabajo` | ADMIN, SUPERADMIN |
| PUT | `/api/v1/tipos-trabajo/{id}` | ADMIN, SUPERADMIN |
| PATCH | `/api/v1/tipos-trabajo/{id}/estado` | ADMIN, SUPERADMIN |

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
3. Un odontólogo solo ve tipos activos.

---

### 4.2 Estados — CU-22 / RN-04

| Método | Ruta | Rol |
|---|---|---|
| GET | `/api/v1/estados` | autenticado |
| PUT | `/api/v1/estados/{id}` | ADMIN, SUPERADMIN |

Solo se permite editar `nombre` y `descripcion`. **Prohibido** modificar `codigo`, `orden_secuencia`, `es_terminal` o `es_productivo`, ni crear o eliminar estados: el flujo lineal de RN-04 depende de ellos.

---

### 4.3 Tipos de orden — RN-11

`GET /api/v1/tipos-orden` · autenticado · solo lectura.

Devuelve `codigo`, `nombre` y `recargoMonto`. No hay endpoints de escritura: el comportamiento diferencial es configuración del sistema.

---

## 5. Módulo Órdenes

### 5.1 Crear orden — CU-09

`POST /api/v1/ordenes` · rol `ODONTOLOGO`

**Request**
```json
{
  "pacienteNombre": "Martín Pérez",
  "fechaIngreso": "2026-08-06",
  "tipoTrabajoId": 16,
  "tipoOrdenCodigo": "NORMAL",
  "descripcion": "Disyuntor superior, tornillo de expansión 7mm"
}
```

**Validaciones**
- `pacienteNombre`, `fechaIngreso`, `tipoTrabajoId`, `tipoOrdenCodigo` obligatorios. `descripcion` opcional.
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
10. Publica evento `OrdenCreadaEvent` → notificación al laboratorio (RN-19). Si `tipo_orden.notifica_admin` es `true`, además notificación de urgencia al ADMIN (RN-11).

**Respuesta** `201` con la orden creada en formato público (sin `pacienteNombre`).

**Criterios de aceptación**
1. Una orden `URGENTE` nace en `EN_EVALUACION` con recargo 200; una `NORMAL` en `RECIBIDO` con recargo 0.
2. `precioTotal` = `precioBase` + `recargoUrgencia`, calculado por la base de datos.
3. La fecha estimada de una orden creada el viernes con 7 días hábiles cae el martes de la semana siguiente.
4. La respuesta no contiene el nombre completo del paciente.
5. Cambiar el precio del catálogo después no altera la orden creada.

---

### 5.2 Adjuntar archivos — RN-13

`POST /api/v1/ordenes/{id}/archivos` · rol `ODONTOLOGO` (propietario) · `multipart/form-data`

**Validaciones (backend, obligatorias)**
- Imagen: `image/jpeg` o `image/png`, ≤ 5 MB.
- Documento: `application/pdf` o DOCX, ≤ 8 MB.
- Fuera de rango → `422 ARCHIVO_NO_PERMITIDO` indicando formato o tamaño.
- La orden debe pertenecer al odontólogo autenticado (RN-01).

**Puerto `AlmacenamientoArchivos`** con `AlmacenamientoLocal` como única implementación (Agente.md 5.5). El binario **no** se guarda en la base: `orden_archivo` almacena ruta y metadatos.

`GET /api/v1/ordenes/{id}/archivos` — listado. `GET /api/v1/archivos/{id}` — descarga con verificación de propiedad.

**Criterios de aceptación**
1. Un JPG de 6 MB es rechazado por el backend aunque el frontend lo permita.
2. Un GIF es rechazado.
3. Un odontólogo no puede descargar el archivo de una orden ajena.

---

### 5.3 Listar mis órdenes — CU-03 / RN-01

`GET /api/v1/ordenes?estado=&page=&size=` · rol `ODONTOLOGO`

**Filtra siempre por el usuario autenticado.** El id de odontólogo **no** se acepta como parámetro.

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

| Evento | `tipo_evento` | Destinatario | Regla |
|---|---|---|---|
| Cuenta creada | `CUENTA_CREADA` | Odontólogo | RN-16, CU-19 |
| Orden creada | `NUEVA_ORDEN` | Administrador | RN-19 |
| Orden urgente creada | `ORDEN_URGENTE` | Administrador | RN-11 |
| Cambio de estado | `CAMBIO_ESTADO` | Odontólogo dueño | RN-05, CU-07 |

**Texto de `CAMBIO_ESTADO`** (formato documentado en CU-07):
> `El trabajo del paciente Código {paciente_codigo} pasó a la etapa de {estado}.`

**No inventar otros eventos.** La cancelación **no** notifica (S-08 sin resolver).

### 6.3 Canales

Puerto `CanalNotificacion` con `soporta(canal)` y `enviar(notificacion)`.

| Adaptador | Estado |
|---|---|
| `CanalApp` | Implementado — solo marca `ENVIADO` (la notificación ya está en base para la campana) |
| `CanalCorreo` | Implementado — SMTP configurable por properties |
| `CanalTelegram` | **Estructura únicamente.** Sin credenciales, marca `FALLIDO` con "canal no configurado". No implementar la integración. |

Los canales activos salen de `configuracion_notificacion` del destinatario (RN-19). Sin configuración, el valor por defecto es app + correo.

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

**Criterios de aceptación**
1. Un cambio de estado genera exactamente una notificación con envío por cada canal activo.
2. Si el correo falla, el envío queda `FALLIDO` y la notificación sigue visible en la app.
3. Un usuario solo ve sus propias notificaciones.
4. Activar Telegram sin `chatId` es rechazado.

---

## 7. Módulo Usuarios (administración)

| Método | Ruta | Rol | Caso de uso |
|---|---|---|---|
| GET | `/api/v1/odontologos` | ADMIN, SUPERADMIN | CU-11 |
| GET | `/api/v1/usuarios` | SUPERADMIN | CU-17 |
| PATCH | `/api/v1/usuarios/{id}/estado` | SUPERADMIN | CU-17 |
| GET | `/api/v1/perfil` | autenticado | — |
| PUT | `/api/v1/perfil` | autenticado | — |

En `/perfil` el usuario edita `nombreCompleto` y `direccion`. **No** puede cambiar su rol ni su correo.

---

## 8. Frontend — pantallas

| Pantalla | Ruta | Rol | Caso de uso |
|---|---|---|---|
| Login | `/login` | público | CU-01 |
| Registro | `/registro` | público | CU-18 |
| Verificación | `/verificar` | público | CU-19 |
| Panel odontólogo | `/inicio` | ODONTOLOGO | CU-02 |
| Mis órdenes | `/ordenes` | ODONTOLOGO | CU-03 |
| Detalle y seguimiento | `/ordenes/:id` | ODONTOLOGO | CU-04 |
| Nueva orden | `/ordenes/nueva` | ODONTOLOGO | CU-09 |
| Historial | `/historial` | ODONTOLOGO | CU-12 |
| Perfil | `/perfil` | autenticado | — |
| Dashboard admin | `/admin` | ADMIN | CU-10 |
| Órdenes (admin) | `/admin/ordenes` | ADMIN | CU-06 |
| Odontólogos | `/admin/odontologos` | ADMIN | CU-11 |
| Tipos de trabajo | `/admin/tipos-trabajo` | ADMIN | CU-16 |
| Configuración | `/admin/configuracion` | ADMIN | CU-21 |
| Licencias | `/admin/licencias` | SUPERADMIN | CU-23 |
| Licencia vencida | `/bloqueado` | cualquiera | RN-20 |

**Referencia visual:** los mockups del PDF original (login, panel odontólogo, seguimiento, dashboard admin). Respetar la estructura de navegación; el detalle visual queda a criterio del desarrollador.

**Menú odontólogo:** Inicio · Mis trabajos · Nueva orden · Historial · Perfil.
**El ítem "Mensajes" NO se incluye** (D-11, pospuesto).

**Menú admin:** Dashboard · Trabajos · Odontólogos · Tipos de trabajo · Configuración.
**No incluir** Pacientes (S-03 sin resolver), Calendario, Mensajes, Reportes ni Facturación (P-08).

**Reglas transversales del frontend**
- Responsive: usable en celular, tablet y computadora, sin instalación.
- Ninguna pantalla muestra el nombre completo del paciente al odontólogo (RN-22).
- Ningún cálculo de negocio en el cliente: precios, fechas y transiciones vienen del backend.
- La línea de tiempo del seguimiento se arma con el historial que devuelve la API.

---

## 9. Trazabilidad — regla de negocio → implementación

| Regla | Dónde se implementa |
|---|---|
| RN-01 | Filtro por usuario autenticado en todas las consultas de órdenes; 404 ante orden ajena |
| RN-03 / RN-22 | `pacienteIdentificacion` en toda respuesta al odontólogo |
| RN-04 | Transiciones lineales en el servicio de órdenes (State) |
| RN-05 | Evento + outbox + canales app y correo |
| RN-11 | `tipo_orden`: estado inicial, `notifica_admin`, `recargo_monto` |
| RN-12 | Validación `diasEstimados >= 7` |
| RN-13 | Validación de formato y tamaño en el backend |
| RN-14 | `@PreAuthorize` en cada endpoint |
| RN-15 | Validador de contraseña en el registro |
| RN-16 | Registro local + Google + verificación por correo |
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
