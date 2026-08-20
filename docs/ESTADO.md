# ESTADO.md — Punto de retomada

**Proyecto:** Lab. Garcia's Connect
**Actualizado:** 20/08/2026 · rama `feature/T-22_NotificationsEndpoint_ChanelConfig`

Este archivo existe para retomar el trabajo sin releer toda la conversación. No reemplaza a
`spec.md`, `Plan.md` ni `Agente.md`: ante cualquier diferencia, **mandan esos tres**. Acá solo
va el estado de avance y lo que se acordó de palabra y no quedó escrito en ellos.

> **Regla de mantenimiento:** se actualiza al cerrar cada tarea confirmada, **en el mismo commit**
> que la tarea. Una tarea que no figure acá como terminada, no está terminada.

---

## (a) Tareas terminadas y confirmadas

| Tarea | Qué dejó | Commit |
|---|---|---|
| **T-01 … T-17** | Cimientos, seguridad, licencia, catálogos, entidades de órdenes y creación de orden | hasta `45c4729` |
| **T-29** | Migración `V2__cr01_solicitud_acceso_whatsapp.sql`; retiro de Google, auto-registro y verificación por correo (D-17/D-18) | `203a019` |
| **T-18** | Adjuntos: puerto `AlmacenamientoArchivos` + `AlmacenamientoLocal`, alta, listado y descarga (RN-13) | `f14ba44` |
| **T-18 · extensión** | `DELETE /api/v1/archivos/{id}` para ADMIN/SUPERADMIN (documentado en `spec.md` §5.2) | `f14ba44`+ |
| **T-19** | `GET /ordenes` (listado propio) y `GET /ordenes/{id}` (detalle + línea de tiempo) | `ea027e6` / `161eef2` |
| **T-20** | `PATCH /ordenes/{id}/estado` y `PATCH /ordenes/{id}/cancelar` (RN-04, RN-17) | `d58ebb9` |
| **T-33a** | `POST /ordenes` pasa a ADMIN/SUPERADMIN con `odontologoId` validado (D-19) | `cb2f38b` |
| **T-21** | Módulo `notificaciones`: outbox, puerto `CanalNotificacion` con app y correo reales, Telegram y WhatsApp como estructura, despachador `@Scheduled` | `c6a5274` / `c539da9` → `82302a5` (PR #29) |
| **T-22** | Los seis endpoints de §6.4: campana (listado, contador, leer, leer-todas) y configuración de canales con CU-21 | rama `feature/T-22_NotificationsEndpoint_ChanelConfig` |

> **Sobre la columna "Commit":** desde que rige el paso 7 de `Agente.md`, este archivo se actualiza
> *dentro* del commit de la tarea, así que ese commit no puede citar su propio hash. La tarea en
> curso se identifica por su rama; el hash o el merge se completan al integrarla a `develop`.

**Verificación al día de hoy:** `mvn -o test` en `backend/` → **221 tests, 0 fallos**.
`npm test` en `frontend/` → **85 tests, 0 fallos** (T-21 y T-22 no tocaron el frontend).

---

## (b) Próxima tarea

### T-23 · Campana en el frontend

**Spec:** §6.4, §8 · **Depende de:** T-11 ✅, T-22 ✅
**Terminado cuando:** el contador se actualiza por *polling* cada 60 s y se pueden marcar como leídas.

Primera tarea de frontend en varias: va en `frontend/src/features/notificaciones/`, que hoy solo
tiene el `.gitkeep`.

El backend ya está entero y verificado. Las respuestas exactas:

| Endpoint | Devuelve |
|---|---|
| `GET /api/v1/notificaciones?leidas=&page=&size=` | `PaginaResponse` de `{id, tipoEvento, mensaje, ordenId, leida, fechaCreacion, fechaLectura}` |
| `GET /api/v1/notificaciones/contador` | `{ "noLeidas": 3 }` |
| `PATCH /api/v1/notificaciones/{id}/leer` | La notificación actualizada |
| `PATCH /api/v1/notificaciones/leer-todas` | `{ "noLeidas": 0 }` — el contador ya al día |

Cinco cosas resueltas que no hay que rediscutir al empezarla:

1. **Polling cada 60 s sobre `/contador`.** **Prohibido WebSocket y SSE** (§6.4).
2. **`leer-todas` ya devuelve el contador actualizado**: no hace falta una segunda llamada.
3. **`ordenId` puede venir nulo** (avisos que no son de una orden). Enlazar al detalle solo si viene.
4. **Paginación en la URL**, `size` ∈ {10,20,30}; otro valor da `400 TAMANO_PAGINA_INVALIDO` (§8.1).
5. **La pantalla de configuración de canales (CU-21, `/admin/configuracion`) NO es de esta tarea.**
   §8 la lista aparte y `Plan.md` acota T-23 a contador y listado. Su backend ya existe.

---

## (c) Decisiones acordadas fuera de spec.md / Plan.md / Agente.md

### 1. Contraseña temporal del alta por el admin — *sí está documentada*

Está completa en **`spec.md` §3.1.b**, incluida la limitación. Se resume acá porque es la
decisión más fácil de perder de vista al implementar T-21:

- `notificacion.mensaje` guarda un **texto genérico sin la contraseña**. El outbox persiste
  ese campo, así que mandarla por el flujo normal la dejaría en claro en la base.
- El correo con usuario y contraseña temporal **se compone y envía directamente en el listener**
  `@TransactionalEventListener(AFTER_COMMIT)`. La contraseña viaja **solo en memoria**, dentro
  del evento. Nunca se persiste ni se loguea.
- El `notificacion_envio` de canal `CORREO` se registra igual, `ENVIADO` o `FALLIDO`.
- **Limitación aceptada:** si ese envío falla, **no es reintentable desde el outbox** — la
  contraseña ya no existe en ningún lado. El remedio es que el admin vuelva a crear las
  credenciales. **No implementar ningún endpoint de "regenerar credenciales".**

### 2. La base vive en el esquema `public`, no en `labgarcias`

`V1__esquema_inicial.sql` trae comentadas sus líneas `CREATE SCHEMA` y `SET search_path`, así
que crea sus 14 tablas en `public`. `V2` se entregó **sin calificar** por el mismo motivo:
calificar rompía la migración con *"schema labgarcias does not exist"*.

> **`spec.md` §1.0 conserva el SQL calificado como `labgarcias.x`, que no coincide con el
> archivo entregado.** Se decidió dejarlo así ("dejalo asi"). Toda migración futura va **sin
> calificar**, igual que V1 y V2, para que todas caigan siempre en el mismo esquema.

### 3. T-20 se implementó sin patrón State

`Plan.md` sigue diciendo *"Patrón esperado: State"* en T-20. No se aplicó, a propósito: un State
con una clase por estado duplicaría en código lo que la tabla `estado` ya define en
`orden_secuencia` y `es_terminal`, y `spec.md` §4.2 prohíbe crear estados o tocar esas columnas
justamente porque RN-04 depende de ellas. La validación quedó en 12 líneas leyendo la tabla.
Amparado en `Agente.md` §7.2 y §7.3 ("no son obligatorios").

### 4. Las trazas de auditoría se postergan a una tarea final

Hoy **no se registra quién borró un adjunto ni quién registró una orden**. El modelo no tiene
columnas para eso y haría falta una migración. Se acordó juntarlas todas en **una tarea al
final**, no resolverlas de a una.

### 5. Sesión JWT de 8 horas

`app.jwt.expiracion-minutos = 480`. Confirmado con el desarrollador; **no sale de `spec.md`**.

### 6. Análisis de nulos de Eclipse JDT desactivado en el editor

`.vscode/settings.json` → `java.compile.nullAnalysis.mode: "disabled"`, con el razonamiento
completo en el propio archivo. Producía 62 avisos de los cuales 44 venían de dobles de Mockito,
inanotables por construcción. No afecta a `mvn test`, que nunca corrió ese análisis.

### 7. Las siete decisiones de T-21 (20/08/2026)

`spec.md` §6 no cierra estos puntos. Se acordaron antes de implementar y quedaron aplicados así:

1. **`OrdenCreadaEvent` lleva `odontologoId`.** §6.2 manda `NUEVA_ORDEN` al dueño y el evento no
   decía quién era.
2. **§5.1 paso 10 quedó cubierto en T-21, no en T-33b** (`Plan.md` corregido). La mitad "avisar al
   dueño" la hace T-21; la mitad "no avisar al admin cuando el creador es el destinatario" **no
   requiere código**: §6.2 ya redirigió `NUEVA_ORDEN` al odontólogo y el paso deja ese aviso
   previsto para cuando P-19 reabra la creación por el odontólogo.
3. **`ORDEN_URGENTE` se emite siempre que `tipo_orden.notifica_admin` sea true**, sin suprimirlo
   cuando lo crea el admin. El paso 10 nombra a **RN-19**; `ORDEN_URGENTE` es otra fila de §6.2,
   bajo **RN-11**. Efecto asumido: el admin recibe campana de una orden que acaba de cargar él.
4. **"El Administrador" = cada cuenta ADMIN/SUPERADMIN activa**, una notificación por cada una:
   la campana y los canales de RN-19 son por usuario, no del laboratorio como bloque.
5. **Canales = matriz de §6.2 ∩ configuración del destinatario.** El evento decide qué canales son
   pertinentes; la configuración solo recorta, nunca agrega. Consecuencia literal: `NUEVA_ORDEN`
   **no genera envío de canal APP** — igual se ve en la campana, que lee `notificacion`.
6. **Textos y asuntos.** Solo `CAMBIO_ESTADO` tiene texto documentado (CU-07) y se usa palabra por
   palabra. `NUEVA_ORDEN` y `ORDEN_URGENTE` se calcaron de ese formato. Ningún asunto de correo
   está documentado. Todo vive en `TextosNotificacion`; RN-03/RN-22: siempre código, nunca nombre.
7. **Los `FALLIDO` no se reintentan solos.** §6.1 los llama "reintentables" pero no hay política
   documentada —ni tope, ni espera, ni columna de intentos— y §6.3 dice "sin reintentos
   automáticos" para Telegram. El `@Scheduled` toma **solo `PENDIENTE`**. Una política automática,
   si se quiere, es una tarea propia con su migración.

### 8. Las seis decisiones de T-22 (20/08/2026)

1. **`canalWhatsappActivo` se informa pero no se puede activar.** El request del PUT no lo incluye.
   Encenderlo hoy solo generaría envíos `FALLIDO` garantizados, que además no se reintentan (ver
   decisión 7). La columna se sigue leyendo: cuando haya proveedor (P-18), es una línea.
2. **El PUT reemplaza la configuración entera.** Las tres banderas son obligatorias —un `null` sería
   ambiguo entre "apagalo" y "no lo toques"— y el `telegramChatId` que no viene, no queda guardado.
3. **`leer-todas` devuelve el contador ya actualizado**, no un mensaje: es lo que la campana necesita
   para refrescarse. No se pudo reutilizar `MensajeResponse` porque vive en el dto de `seguridad` y
   §5.4 prohíbe importarlo desde otro módulo.
4. **`leer-todas` es un `UPDATE` masivo**, no un bucle de entidades: vaciar 200 avisos de un clic no
   puede costar 200 consultas.
5. **`BandejaNotificacionService` va aparte de `NotificacionService`.** Son dos cosas distintas sobre
   la misma tabla: una escribe el outbox cuando ocurre un evento, la otra contesta lo que el
   destinatario pregunta.
6. **El aislamiento del criterio 3 es estructural, no una verificación posterior.** *Todas* las
   consultas de `NotificacionRepository` filtran por destinatario, incluida
   `findByIdAndDestinatarioId`: no hay ningún `findById` suelto que alguien pueda usar por descuido.
   `NotificacionRutasTest` fija además que ningún endpoint acepte un id de usuario.

### Puntos abiertos (no son decisiones, son deudas)

- **`application-prod.yml` no existe y está en `.gitignore`.** `spec.md` §1.1 exige Swagger
  deshabilitado en producción; sin ese archivo, el perfil `prod` no tiene dónde apagarlo.
  Desde T-21 hace falta además para las credenciales SMTP reales (`MAIL_HOST`, `MAIL_USERNAME`,
  `MAIL_PASSWORD`, `MAIL_REMITENTE`). Reportado desde T-29, sin resolver.
- **El frontend todavía no refleja D-19.** Cuando llegue T-25, el menú del odontólogo no debe
  ofrecer "Nueva orden": eso lo retira T-33b.
- **El correo de credenciales de §3.1.b todavía no existe.** Es de T-31. `TipoEvento` ya lo declara
  como solo-correo y `TextosNotificacion` documenta que ese texto se compone **fuera del outbox**.
- **`CanalesDeEstructuraTest` va a fallar cuando T-32 integre Telegram.** Es deliberado: fija que
  hoy no hay integración, para que nadie la dé por hecha.
- **El chat de Telegram está en dos lugares y T-32 tiene que decidir cuál manda.**
  `configuracion_notificacion.telegram_chat_id` (V1) es contra el que §6.4 manda validar CU-21, y es
  lo que implementó T-22. Pero V2 agregó `usuario.telegram_chat_id` + `telegram_vinculado`, que es
  donde el flujo de §6.5 guarda el chat, y §6.3 dice que el envío "solo se intenta si el usuario
  está vinculado". Lectura probable: cuando exista la vinculación real, el chat lo pone el bot en
  `usuario` y el de la configuración sobra; ahí CU-21 pasaría a validar "estás vinculado" en vez de
  "cargaste un chat a mano". **No resolver por las nuestras: es decisión de T-32.**
- **Un odontólogo no puede apagar ningún canal.** §6.4 reserva `/configuracion-notificaciones` a
  ADMIN y SUPERADMIN, pero D-20 le manda notificaciones por tres canales. Se implementó como dice la
  spec. Si la clienta quiere que el odontólogo elija, es un cambio de spec, no un error.

---

## (d) Orden de ejecución vigente

```
T-29 ✅ → T-18 ✅ → T-19 ✅ → T-20 ✅ → T-33a ✅ → T-21 ✅ → T-22 ✅ → [T-23]
     → T-30 → T-31 → T-32 → T-32b → T-33b
     → T-25 → T-26 → T-27 → T-28
```

Es el orden de `Plan.md`, que **no** sigue la numeración de los bloques. `T-24` fue eliminada
por D-19. `T-33` quedó partida: **T-33a** (backend) ya se hizo adelantada, **T-33b** (pantalla
y notificación) conserva su lugar después de T-32b porque el selector de odontólogo necesita
las cuentas de T-31.

**Protocolo, sin excepciones:** una tarea, un reporte con el formato de `Agente.md` §4.1, y
esperar confirmación. Nunca encadenar dos tareas.
