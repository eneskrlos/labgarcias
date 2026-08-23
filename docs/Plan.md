# Plan.md — Plan de ejecución

**Proyecto:** Lab. Garcia's Connect
**Versión:** 2.0 — 19/08/2026 (incorpora CR-01 y el orden de ejecución definitivo)

Orden de implementación. Cada tarea se ejecuta **de forma individual**, se reporta según `Agente.md` 4.1 y **espera confirmación** antes de continuar.

**33 tareas.** T-01 a T-17 están terminadas y confirmadas. T-24 fue eliminada por D-19 (ver abajo).

---

## Orden de ejecución definitivo (19/08/2026)

Las tareas **no** se ejecutan en orden numérico. El bloque CR-01 no puede correr completo de inmediato: T-30, T-31, T-32 y T-32b dependen del núcleo de notificaciones (T-21/T-22), que todavía no existe. El orden obligatorio es:

```
T-29 → T-18 → T-19 → T-20 → [T-33a] → T-21 → T-22 → T-23
     → T-30 → T-31 → T-32 → T-32b → T-33b
     → T-25 → T-26 → T-27 → T-28
```

> **T-33a adelantada (19/08/2026).** T-33 quedó partida: su etapa de backend solo dependía de T-29 y T-20, ya terminadas, y hasta hacerla `POST /ordenes` seguía aceptando al odontólogo, en contra de D-19. La etapa de pantalla (T-33b) conserva su lugar después de T-32b, porque el selector necesita las cuentas de T-31.

**Por qué este orden:** T-29 primero, para que el código muerto de Google y verificación no se arrastre por todo el bloque 3. Después se completa el ciclo de órdenes (T-18 a T-20) y el núcleo de notificaciones (T-21 a T-23), que es la base sobre la que se apoyan las tareas de CR-01. Recién entonces entran solicitud de acceso, alta de credenciales, Telegram y órdenes por el admin. Las pantallas restantes cierran al final.

> Este orden reemplaza tanto la numeración de los bloques 0-6 como la nota "se ejecutan después de la tarea en curso" del bloque CR-01.

---

## Cómo leer este plan

| Campo | Significado |
|---|---|
| **Objetivo** | Qué debe quedar funcionando |
| **Depende de** | Tareas que deben estar confirmadas antes |
| **Spec** | Sección de `spec.md` que la define |
| **Reglas** | RN/CU que cubre |
| **Terminado cuando** | Verificación concreta |

> Regla que no se negocia: **al terminar cada tarea, reportar y esperar.** Aunque la siguiente sea trivial o dependiente.

> **Documentación Swagger:** desde T-03, cada tarea que agregue endpoints los documenta en la misma tarea (`spec.md` §1.1). El reporte debe indicar la ruta de Swagger donde verificarlos.

---

## Bloque 0 — Cimientos

### T-01 · Estructura del proyecto backend
**Objetivo:** proyecto Spring Boot arrancando, con la estructura de módulos de `Agente.md` 5.3.
**Depende de:** —
**Spec:** §1
**Terminado cuando:**
- El proyecto compila y arranca.
- Existen los paquetes `seguridad`, `ordenes`, `catalogos`, `notificaciones`, `licencia`, `shared` (vacíos, con su subestructura `controller/service/repository/domain/dto`).
- Dependencias: Web, Data JPA, Security, Validation, Flyway, PostgreSQL, Mail y `springdoc-openapi-starter-webmvc-ui`.
- Perfiles `dev` y `prod` separados, con Swagger habilitado **solo en `dev`** (`spec.md` §1.1).
- Sin lógica de negocio.

### T-02 · Base de datos y Flyway
**Objetivo:** el esquema validado se aplica automáticamente al arrancar.
**Depende de:** T-01
**Spec:** §1
**Terminado cuando:**
- `01_labgarcias_schema.sql` es `V1__esquema_inicial.sql` **sin modificaciones**.
- Conexión configurada a `labgarcias_db`, esquema `labgarcias`, puerto 5436.
- `ddl-auto: validate`. **Prohibido `update`.**
- Al arrancar contra una base vacía se crean las 14 tablas y el seed (45 tipos, 7 estados, 3 roles, 2 tipos de orden).

### T-03 · Manejo de errores, Swagger y configuración transversal
**Objetivo:** respuestas de error uniformes, excepciones del dominio y documentación de la API.
**Depende de:** T-01
**Spec:** §1, §1.1
**Terminado cuando:**
- `@RestControllerAdvice` devuelve `{ codigo, mensaje, campo }`.
- Excepciones base del dominio en `shared`.
- Constantes de `spec.md` §2 definidas con su referencia a la regla.
- CORS configurado para el frontend.
- Swagger configurado con metadatos y **esquema de seguridad JWT** (botón "Authorize" operativo).
- Rutas de Swagger permitidas en Spring Security.
- Se cumplen los criterios 1, 2 y 5 de `spec.md` §1.1: la UI responde en `dev`, devuelve 404 en `prod` y no expone entidades JPA.
> El criterio 3 (autenticar con JWT en la UI) se verifica en T-08, cuando exista el login.

### T-04 · Estructura del proyecto frontend
**Objetivo:** React arrancando, con la estructura de `Agente.md` 5.7.
**Depende de:** —
**Spec:** §8
**Terminado cuando:**
- Proyecto React con carpetas `features/`, `shared/`, `styles/`.
- React Router y TanStack Query configurados.
- Cliente HTTP con interceptor de token e interceptor de `423` (licencia).
- Variables CSS base y layout responsive. **Sin Tailwind ni librerías de componentes.**

---

## Bloque 1 — Seguridad y acceso

### T-05 · Entidades y repositorios de seguridad
**Objetivo:** mapeo JPA de `rol`, `usuario`, `token_verificacion`.
**Depende de:** T-02, T-03
**Spec:** §3
**Reglas:** RN-14, RN-16
**Terminado cuando:** las entidades reflejan exactamente el esquema, sin campos agregados, y los repositorios exponen las búsquedas necesarias.

### T-06 · Registro de odontólogo
**Objetivo:** CU-18 funcionando.
**Depende de:** T-05
**Spec:** §3.1
**Reglas:** RN-15, RN-16, CU-18
**Terminado cuando:** se cumplen los 3 criterios de aceptación de §3.1.

### T-07 · Verificación de cuenta por correo
**Objetivo:** CU-19 con enlace de un solo uso.
**Depende de:** T-06
**Spec:** §3.2
**Reglas:** D-02, CU-19
**Terminado cuando:** se cumplen los 3 criterios de §3.2.
> Requiere un envío de correo mínimo. El adaptador completo llega en T-21; acá alcanza con un envío directo o registrado en log, y se declara en el reporte.

### T-08 · Login con JWT y autorización por rol
**Objetivo:** CU-01 y CU-14.
**Depende de:** T-06
**Spec:** §3.3, §3.5
**Reglas:** RN-02, RN-14, CU-01, CU-14
**Terminado cuando:** se cumplen los 3 criterios de §3.3, todo endpoint existente tiene autorización declarada, y se cumple el criterio 3 de §1.1: se puede pegar un JWT en "Authorize" de Swagger y ejecutar un endpoint protegido.

### T-09 · Autenticación con Google
**Objetivo:** registro y login con Google.
**Depende de:** T-08
**Spec:** §3.4
**Reglas:** RN-16
**Terminado cuando:** se cumplen los 3 criterios de §3.4.

### T-10 · Módulo de licencia y filtro de bloqueo
**Objetivo:** RN-20 y CU-23.
**Depende de:** T-08
**Spec:** §3.6
**Reglas:** RN-20, CU-23, D-16
**Patrón esperado:** Intercepting Filter.
**Terminado cuando:** se cumplen los 3 criterios de §3.6 y el criterio 4 de §1.1: con la licencia vencida, Swagger sigue accesible en `dev`.
> **Fuera de alcance:** planes, precios y pasarela de pago (P-11, P-12).

### T-11 · Pantallas de login, registro y verificación
**Objetivo:** front de CU-01, CU-18, CU-19 y pantalla de bloqueo.
**Depende de:** T-04, T-08, T-09, T-10
**Spec:** §8
**Terminado cuando:**
- `/login`, `/registro`, `/verificar` y `/bloqueado` funcionan contra la API.
- El registro muestra los requisitos de contraseña de RN-15.
- Botón de Google operativo.
- Rutas protegidas por rol; sesión persistida.

---

## Bloque 2 — Catálogos

### T-12 · Entidades y consulta de catálogos
**Objetivo:** `tipo_trabajo`, `estado`, `tipo_orden` mapeados y consultables.
**Depende de:** T-08
**Spec:** §4
**Reglas:** RN-04, RN-11, RN-12, RN-21
**Terminado cuando:** los tres endpoints GET devuelven el seed correctamente; el odontólogo solo ve tipos activos.

### T-13 · Administración de tipos de trabajo
**Objetivo:** CU-16 completo.
**Depende de:** T-12
**Spec:** §4.1
**Reglas:** RN-12, RN-21, CU-16
**Terminado cuando:** se cumplen los 3 criterios de §4.1.
> **No implementar eliminación.** Solo desactivación.

### T-14 · Administración de estados
**Objetivo:** CU-22 acotado.
**Depende de:** T-12
**Spec:** §4.2
**Reglas:** RN-04, CU-22
**Terminado cuando:** solo se pueden editar `nombre` y `descripcion`; cualquier intento de alterar `codigo`, `orden_secuencia`, `es_terminal` o `es_productivo`, o de crear/eliminar estados, es rechazado.

### T-15 · Pantalla de tipos de trabajo
**Objetivo:** front de CU-16.
**Depende de:** T-11, T-13
**Spec:** §8
**Terminado cuando:** el admin lista, crea, edita y desactiva tipos, con los mensajes de validación de RN-12 y RN-21.

---

## Bloque 3 — Órdenes

### T-16 · Entidades y repositorios de órdenes
**Objetivo:** mapeo de `orden`, `orden_historial_estado`, `orden_archivo`.
**Depende de:** T-12
**Spec:** §5
**Terminado cuando:** las entidades reflejan el esquema; `precio_total` se mapea como **columna generada de solo lectura**.

### T-17 · Creación de orden
**Objetivo:** CU-09 completo.
**Depende de:** T-16
**Spec:** §5.1
**Reglas:** RN-11, RN-12, RN-18, RN-21, RN-22, CU-09
**Patrón esperado:** Factory Method.
**Terminado cuando:** se cumplen los 5 criterios de §5.1.
> El estado inicial y el recargo se **leen de `tipo_orden`**. Prohibido `if (tipo == URGENTE)`.
> El evento se publica; el envío efectivo llega en el bloque 4.

### T-18 · Adjuntos de la orden
**Objetivo:** RN-13 con el puerto de almacenamiento.
**Depende de:** T-17
**Spec:** §5.2
**Reglas:** RN-01, RN-13
**Patrón esperado:** Adapter (`AlmacenamientoArchivos` → `AlmacenamientoLocal`).
**Terminado cuando:** se cumplen los 3 criterios de §5.2.

### T-19 · Listado, detalle y seguimiento
**Objetivo:** CU-03 y CU-04 con aislamiento y privacidad.
**Depende de:** T-17
**Spec:** §5.3, §5.4
**Reglas:** RN-01, RN-22, CU-03, CU-04
**Terminado cuando:** se cumplen los 3 criterios de §5.4 y el listado nunca acepta un id de odontólogo por parámetro.

### T-20 · Cambio de estado y cancelación
**Objetivo:** CU-06 y CU-20.
**Depende de:** T-19
**Spec:** §5.5, §5.6
**Reglas:** RN-04, RN-17, CU-06, CU-20
**Patrón esperado:** State.
**Terminado cuando:** se cumplen los 4 criterios de §5.5 y los 3 de §5.6.
> **Sin retrocesos** (P-02). **Sin cargo por cancelación** (P-14). **Sin endpoint de edición** (RN-17).

---

## Bloque 4 — Notificaciones

### T-21 · Núcleo de notificaciones y canal de correo — ✅ TERMINADA (20/08/2026)
**Objetivo:** eventos, outbox y envío por correo.
**Depende de:** T-20
**Spec:** §6.1, §6.2, §6.3
**Reglas:** RN-05, RN-11, RN-19, CU-07
**Patrones esperados:** Observer, Strategy, Adapter, Transactional Outbox.
**Terminado cuando:** se cumplen los criterios 1 y 2 de §6.
> `CanalTelegram`: **solo la estructura**, marca `FALLIDO` con "canal no configurado". No integrar.
> **La integración real de Telegram llega en T-32 (D-21).** Esto no es una contradicción: es implementación en dos etapas — T-21 crea el puerto, el outbox y el adaptador vacío; T-32 lo convierte en canal real vía Bot API.
> No inventar eventos: la cancelación **no** notifica (S-08).

### T-22 · Endpoints de notificaciones y configuración de canales — ✅ TERMINADA (20/08/2026)
**Objetivo:** campana, contador y CU-21.
**Depende de:** T-21
**Spec:** §6.4
**Reglas:** RN-19, CU-21
**Terminado cuando:** se cumplen los criterios 3 y 4 de §6.

### T-23 · Campana en el frontend — ✅ TERMINADA (20/08/2026)
**Objetivo:** contador y listado de notificaciones.
**Depende de:** T-11, T-22
**Spec:** §6.4, §8
**Terminado cuando:** el contador se actualiza por polling cada 60 s y se pueden marcar como leídas.
> **No implementar WebSocket ni SSE.**

---

## Bloque 5 — Pantallas de órdenes

### ~~T-24 · Nueva orden (frontend)~~ — ELIMINADA por CR-01 (D-19)

Sus criterios eran el front de CU-09 para el **odontólogo**, que D-19 retira de la navegación. La pantalla de alta de orden pasa a ser `/admin/ordenes/nueva` y la cubre **T-33b**; el endpoint ya lo cubrió **T-33a**. Si P-19 reabre la creación por el odontólogo, esta tarea vuelve al plan.

### T-25 · Mis órdenes y seguimiento (frontend)
**Objetivo:** front de CU-03, CU-04 y CU-20.
**Depende de:** T-19, T-33b *(el menú del odontólogo pierde "Nueva orden" en T-33b)*
**Spec:** §5.3, §5.4, §5.6, §8
**Terminado cuando:**
- Listado con identificación por iniciales + código, **nunca** el nombre completo.
- Detalle con línea de tiempo fechada, datos, archivos y botón de cancelación.
- **Sin sección de mensajes** (D-11).

### T-26 · Gestión de órdenes (admin)
**Objetivo:** CU-10 completo (backend + pantalla) y front de CU-06.
**Depende de:** T-20, T-25
**Spec:** §5.5, §5.7, §8
**Terminado cuando:**
- `GET /api/v1/admin/ordenes` con los filtros de §5.7 (`estado`, `tipoOrden`, `odontologoId`) y paginación de backend.
- El admin lista y filtra órdenes, ve el detalle y avanza el estado con un botón que solo ofrece la transición siguiente válida.

> **Precisión (19/08/2026):** el objetivo decía "front de CU-10" pero el criterio exigía que el admin listara y filtrara órdenes, cosa imposible sin el endpoint. **El backend de §5.7 —salvo el dashboard— es de esta tarea**, no de otra. Es el único endpoint de §5.7 que no tenía tarea asignada.

---

## Bloque 6 — Paneles y cierre

### T-27 · Dashboards
**Objetivo:** CU-02, CU-10 y CU-12.
**Depende de:** T-26
**Spec:** §5.7, §8
**Reglas:** CU-02, CU-10, CU-12
**Terminado cuando:**
- `GET /api/v1/admin/dashboard` con lo que enumera §5.7: contadores, distribución por estado (`v_ordenes_por_estado`), próximas a entregar y órdenes recientes. **El backend del dashboard es de esta tarea.**
- Panel del odontólogo con sus contadores y órdenes recientes.
- Dashboard admin con contadores, distribución por estado, próximas a entregar y urgentes.
- Historial del odontólogo.
> **Sin reportes ni estadísticas más allá de estos contadores** (CU-13 es Fase 4).

### T-28 · Perfil, odontólogos y repaso final
**Objetivo:** CU-11, CU-17 y verificación integral.
**Depende de:** T-27
**Spec:** §7, §9
**Terminado cuando:**
- Perfil editable (nombre y dirección; **no** rol ni correo). *La pantalla `/perfil` y `GET /api/v1/perfil` ya existen desde T-32b: T-28 agrega `PUT` y el formulario, no vuelve a crearla.*
- Listado de odontólogos para el admin; gestión de usuarios para el SuperAdmin.
- Repaso de la tabla de trazabilidad de `spec.md` §9: cada regla tiene su implementación verificable.
- Repaso de que ningún punto fuera de alcance (`Agente.md` 3.3) quedó implementado.

---

## Resumen

| Bloque | Tareas | Entrega | Estado |
|---|---|---|---|
| 0 · Cimientos | T-01 a T-04 | Proyectos arrancando con base de datos migrada | Terminado |
| 1 · Seguridad | T-05 a T-11 | Login, licencia. *(Registro, verificación y Google se revierten en T-29 por CR-01.)* | Terminado |
| 2 · Catálogos | T-12 a T-15 | Tipos de trabajo y estados administrables | Terminado |
| 3 · Órdenes | T-16 a T-20 | Ciclo completo: creación, adjuntos, seguimiento, estados | Terminado |
| 4 · Notificaciones | T-21 a T-23 | Outbox, correo, campana | Terminado |
| 5 · Pantallas de órdenes | T-25, T-26 | Front de odontólogo y administración *(T-24 eliminada por D-19)* | Pendiente |
| 6 · Paneles y cierre | T-27 a T-28 | Dashboards y verificación integral | Pendiente |
| CR-01 · Cambio de alcance | T-29 a T-33b | Solicitud de acceso, alta por admin, Telegram, órdenes por admin | T-29, T-30 y T-33a terminadas |

> El orden de ejecución **no** sigue esta tabla: rige la secuencia del principio del documento.

---

## Puntos de decisión pendientes

Estas tareas **cambiarán** cuando la clienta confirme los pendientes. No adelantar trabajo sobre ellas:

| Pendiente | Tareas afectadas |
|---|---|
| P-02 · Retrocesos de estado | T-20 |
| P-08 · Facturación | Ninguna (fuera de alcance) |
| P-10 · Porcentaje vs monto fijo | T-17 |
| P-14 · Cargo por cancelación | T-20 |
| P-15 / P-16 · Precios y días reales | T-13 (solo datos, no código) |
| D-11 · Mensajería | T-25 (agregaría sección al detalle) |
| S-03 · Entidad paciente | T-17, T-19 |
| S-05 · Feriados en días hábiles | T-17 |
| S-08 · Notificar cancelación | T-21 |


---

## Bloque CR-01 — Cambio de alcance (reunión 17/08/2026)

Estas tareas aplican las decisiones D-17 a D-21 de `spec.md`. **No se ejecutan de corrido:** T-29 va primero de todo, y el resto espera a que exista el núcleo de notificaciones — ver el orden definitivo al principio del documento. Mismo protocolo de siempre: una a la vez, reporte y confirmación.

### T-29 · Migración V2 y retiro de Google y verificación
**Objetivo:** base de datos actualizada y código muerto eliminado.
**Spec:** §1.0, §3.2, §3.4
**Terminado cuando:**
- `V2__cr01_solicitud_acceso_whatsapp.sql` aplica limpio sobre una base con V1 (exactamente el SQL de §1.0).
- Eliminados endpoint, servicio y botón de Google si existían (D-17).
- Eliminados los endpoints de verificación y reenvío si existían (D-18).
- **V1 intacta.**

### T-30 · Solicitud de acceso — ✅ TERMINADA (21/08/2026)
**Objetivo:** flujo público de solicitud + gestión del admin.
**Depende de:** T-29, T-22 *(la solicitud notifica al admin: necesita el outbox y la campana)*
**Spec:** §3.1
**Terminado cuando:** se cumplen los 3 criterios de §3.1; el listado de solicitudes cumple la convención §8.1; el botón del login lleva al formulario.

### T-31 · Alta de odontólogo con credenciales autogeneradas — ✅ TERMINADA (21/08/2026)
**Objetivo:** D-18 completo, incluido el cambio obligatorio de contraseña.
**Depende de:** T-30, T-21 *(el correo de credenciales se envía desde el listener; ver §3.1.b)*
**Spec:** §3.1.b
**Terminado cuando:** se cumplen los 4 criterios de §3.1.b; el frontend intercepta `debeCambiarPassword` y fuerza el paso por `/cambiar-password`.
> **Alcance ampliado el 21/08/2026 por el desarrollador:** se sumaron el formulario `/admin/odontologos/nuevo` (§8.1 Regla 1) y el botón "Crear cuenta" de `/admin/solicitudes`, porque el criterio 4 de §3.1.b no era verificable sin interfaz. **T-28 hereda solo el listado de CU-11.**

### T-32 · Canal Telegram implementado + estructura WhatsApp (D-21) — ✅ TERMINADA (21/08/2026)
**Objetivo:** Telegram como canal real del outbox; WhatsApp solo estructura.
**Depende de:** T-21 *(convierte en canal real el adaptador que T-21 dejó como estructura)*
**Spec:** §6.3, §6.5
**Terminado cuando:**
- `CanalTelegram` envía de verdad vía Bot API (`sendMessage`) usando `usuario.telegram_chat_id`; token en properties, nunca en código.
- Un cambio de estado genera envíos APP + CORREO + TELEGRAM; sin vinculación, Telegram queda `FALLIDO` con "Telegram no vinculado".
- `CanalWhatsApp` existe como estructura (P-18): `FALLIDO` con "canal no configurado".
- Sin `telegram.bot.token` configurado, el canal se deshabilita con mensaje claro (no rompe el arranque).

> **Decisión del desarrollador (21/08/2026):** el envío usa **`usuario.telegram_chat_id`** (V2, D-21, §6.3). **CU-21 queda tal como lo entregó T-22**: §6.4 y su endpoint no se tocan, aunque valide contra `configuracion_notificacion.telegram_chat_id`. La incoherencia entre las dos columnas la eleva el desarrollador a la clienta; no se resuelve dentro de T-32 (deuda de spec anotada en `ESTADO.md`).

### T-32b · Vinculación de Telegram desde el perfil — ✅ TERMINADA (22/08/2026)
**Objetivo:** flujo de §6.5 completo.
**Depende de:** T-32
**Spec:** §6.5
**Terminado cuando:** se cumplen los 4 criterios de §6.5; el perfil muestra el estado y los botones de conectar/desvincular; el consumo de `getUpdates` corre por `@Scheduled` (sin webhook).

> **Alcance acordado el 22/08/2026 con el desarrollador:** T-32b crea la pantalla `/perfil` con la sección de Telegram e implementa **`GET /api/v1/perfil`** (§7), porque sin una lectura del estado el criterio de §6.5 no es verificable. **`PUT /perfil` y la edición de nombre y dirección siguen siendo de T-28**, que *extiende* esta pantalla en lugar de crearla. El token de vinculación vence a los **15 minutos** sobre `fecha_emision`, sin migración.

### T-33 · Órdenes registradas por el admin
**Objetivo:** D-19 aplicado.
**Spec:** §5.1

> **Partida en dos etapas (19/08/2026).** La dependencia con T-31 era solo del selector de odontólogo de la pantalla; el endpoint no la necesita. Se adelantó la etapa de backend para que `POST /ordenes` dejara de contradecir a D-19 mientras tanto.

#### T-33a · Endpoint de alta por el laboratorio — ✅ TERMINADA (19/08/2026)
**Depende de:** T-29, T-20
**Terminado cuando:**
- `POST /ordenes` exige rol ADMIN o SUPERADMIN y `odontologoId` válido (`422 ODONTOLOGO_INVALIDO`).

#### T-33b · Pantalla y notificación — ✅ TERMINADA (23/08/2026)
**Depende de:** T-33a, T-31 *(el selector de odontólogo necesita las cuentas creadas por el admin)*, T-21 *(la notificación necesita el outbox)*
**Terminado cuando:**
- Pantalla `/admin/ordenes/nueva` con selector de odontólogo.
- "Nueva orden" retirada del menú y rutas del odontólogo (el flujo queda documentado por P-19).

> **Decisión del desarrollador (23/08/2026):** el selector se alimenta de **`GET /api/v1/odontologos/activos`**, sin paginar, ADMIN y SUPERADMIN, devolviendo solo `id` y `nombreCompleto`. La exención de paginado de §4 quedó reescrita para cubrir los endpoints que alimentan selectores. **T-28 no hereda menos por esto:** sigue implementando `GET /api/v1/odontologos` paginado (§7, CU-11) y la pantalla `/admin/odontologos` con §8.1 Regla 2 — son dos endpoints con propósitos distintos, uno alimenta un selector y el otro una tabla administrable.

> **§5.1 paso 10 ya quedó cubierto en T-21 (20/08/2026).** `OrdenCreadaEvent` viaja con `odontologoId` y el aviso `NUEVA_ORDEN` llega al odontólogo dueño. La otra mitad del paso —no avisar al admin cuando el creador es el propio destinatario— **no requiere código**: §6.2 ya redirigió `NUEVA_ORDEN` al odontólogo y el paso deja el aviso al admin *previsto para cuando P-19 reabra la creación por el odontólogo*. Esta tarea es solo la pantalla.

### Pendientes que agrega CR-01

| Pendiente | Qué falta | Tarea afectada |
|---|---|---|
| P-18 | WhatsApp: se activará a futuro con proveedor pago (Meta Cloud API o Twilio) — reemplazado hoy por Telegram (D-21) | T-32 (solo estructura) |
| P-19 | ¿Se reabre la creación de órdenes por el odontólogo? | T-33a, T-33b |
| P-20 | Crear el bot con @BotFather y configurar `telegram.bot.token` en cada instalación (tarea del desarrollador, no del agente) | T-32, T-32b |
