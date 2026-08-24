# ESTADO.md — Punto de retomada

**Proyecto:** Lab. Garcia's Connect
**Actualizado:** 24/08/2026 · rama `feature/T-34_pantalla_config_notification`

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
| **T-22** | Los seis endpoints de §6.4: campana (listado, contador, leer, leer-todas) y configuración de canales con CU-21 | `073b93e` → `f4de6b1` (PR #30) |
| **T-23** | Campana en el frontend: contador por polling de 60 s, panel desplegable paginado por el backend, marcar una y todas como leídas, layout autenticado compartido | `213b5c4` → `8cf962d` (PR #31) |
| **—** | `application-prod.yml` y su copia de referencia versionada (deuda de T-29; ver puntos abiertos) | `15394a9` |
| **T-30** | Solicitud de acceso: `POST /auth/solicitud-acceso` público, listado y rechazo para administración, aviso `SOLICITUD_ACCESO` al admin, pantallas `/solicitar-acceso` y `/admin/solicitudes` | `5c69a0d` → `20dbe98` (PR #32) |
| **T-31** | Alta de odontólogo (D-18): contraseña `SecureRandom` enviada por correo fuera del outbox, token restringido con su filtro, `POST /auth/cambiar-password`, pantalla `/cambiar-password`, formulario `/admin/odontologos/nuevo` y "Crear cuenta" desde una solicitud | `b07cb66` → `852e569` (PR #33) |
| **—** | `EscritorErrorHttp`: los errores escritos fuera del `@RestControllerAdvice` salen en UTF-8 (ver puntos abiertos) | `783793c` |
| **T-32** | Canal Telegram real por la Bot API (`sendMessage`) con el token en properties, `usuario.telegram_chat_id` mapeado, WhatsApp confirmado como estructura con validación de teléfono | `8bee928` → `c0f5c18` (PR #34) |
| **T-32b** | Vinculación de §6.5: `POST`/`DELETE /telegram/vinculacion`, token de un solo uso con 15 minutos de vigencia, consumo de `getUpdates` por `@Scheduled`, `GET /perfil` y pantalla `/perfil` con la sección de Telegram | `f139ddf` → `ad3173c` (PR #35) |
| **T-33b** | Pantalla `/admin/ordenes/nueva` con selector de odontólogo (D-19) y `GET /odontologos/activos` que la alimenta | `c3fca8b` → `3d92a26` (PR #36) |
| **—** | `/odontologos/activos` documentado en la tabla de endpoints de `spec.md` §7 (deuda de T-33b) | `675f4b9` |
| **T-25** | Pantallas del odontólogo: `/ordenes` paginado con filtro por estado, `/ordenes/:id` con línea de tiempo, adjuntos y cancelación, menú de §8 y el `ordenId` de la campana convertido en enlace | `b1b1dbc` → `1ca14fb` (PR #38) |
| **—** | `PantallaPendiente`: destino de los ítems de menú cuya pantalla llega después | `fbe6beb` |
| **T-26** | Gestión de órdenes del laboratorio: `GET /admin/ordenes` con los tres filtros de §5.7, `siguienteEstado` en el detalle, pantallas `/admin/ordenes` y `/admin/ordenes/:id` con el avance de estado, y el menú del admin de §8 | `73af666` (falta el merge) |
| **T-27** | Dashboards: `GET /admin/dashboard` y `GET /dashboard` (nuevo, CU-02), filtro `?historico` en `GET /ordenes` (CU-12), pantallas `/inicio`, `/admin` y `/historial`, `/` redirige por rol, y la zona horaria del laboratorio en properties | `25606ef` + `7b8bb42` (docs) |
| **T-34** | Pantalla `/admin/configuracion` (CU-21): ve y edita los canales del administrador con el `GET` y el `PUT` de T-22, el `422 TELEGRAM_SIN_DESTINO` en su campo y WhatsApp deshabilitado (P-18). **Sin endpoints nuevos** | rama `feature/T-34_pantalla_config_notification` |

> **Sobre la columna "Commit":** desde que rige el paso 7 de `Agente.md`, este archivo se actualiza
> *dentro* del commit de la tarea, así que ese commit no puede citar su propio hash. La tarea en
> curso se identifica por su rama; el hash o el merge se completan al integrarla a `develop`.

**Verificación al día de hoy:** `mvn -o test` en `backend/` → **406 tests, 0 fallos**.
`npm test` en `frontend/` → **266 tests, 0 fallos**. `npm run lint` y `npm run build` limpios.

T-30, T-31 y T-32 se probaron además de punta a punta con el backend levantado en `dev` contra la
base real. De T-31, con un SMTP de prueba: el correo de credenciales llegó completo y la contraseña
generada **no aparece** en el log del backend ni en ninguna columna de la base (verificado por
búsqueda directa); el token restringido devolvió `403 CAMBIO_PASSWORD_REQUERIDO` en todos los
endpoints hasta el cambio. De T-32, contra la API real de Telegram con un token falso: los tres
canales se despacharon por separado, el usuario vinculado llegó hasta `api.telegram.org` y quedó
`FALLIDO` con el motivo que devolvió Telegram, los no vinculados con "Telegram no vinculado", y el
token no aparece **ninguna vez** ni en el log ni en `detalle_error`. Levantado sin
`TELEGRAM_BOT_TOKEN`, arranca igual y los envíos quedan `FALLIDO` con el motivo.

De T-32b, **contra el bot real** (`labgarcias_connect_bot`, creado por el desarrollador): `GET /perfil`
devuelve el usuario con `telegramVinculado`, `POST /telegram/vinculacion` devuelve el enlace profundo
con el nombre real del bot y deja el token en la tabla, `DELETE` responde 204, el `getUpdates`
programado corre sin un solo aviso en el log, el token del bot no aparece en el log, y sin
`TELEGRAM_BOT_USERNAME` la aplicación arranca igual y la vinculación responde `422
TELEGRAM_NO_CONFIGURADO`.

**El flujo de §6.5 quedó cerrado de punta a punta el 23/08/2026**, por una corrida del desarrollador
—el único paso que necesitaba una cuenta de Telegram—: se emitieron cuatro tokens, uno se usó, y
`erneskrlos` (id 11) quedó vinculado. Durante la verificación de T-33b, la notificación
`ORDEN_URGENTE` de una orden real le **llegó por Telegram** (`notificacion_envio` en `ENVIADO`), que
es el criterio 1 de §6.5 comprobado sobre datos que produjo el flujo real y no la base a mano.

De T-33b, contra la base real: `GET /odontologos/activos` devolvió los seis odontólogos ACTIVOS con
solo id y nombre —las cuentas sin verificar quedaron afuera— y `403` con rol ODONTOLOGO, igual que
`POST /ordenes` (D-19). Un alta NORMAL generó **solo** `NUEVA_ORDEN` al odontólogo dueño; una
URGENTE, `NUEVA_ORDEN` al dueño **más** `ORDEN_URGENTE` a los cuatro administradores (RN-11, leído de
`tipo_orden`). Ningún evento de más, ningún nombre de paciente en los mensajes ni en el `201`, y la
fecha estimada del viernes 21/08 cayó en el martes 01/09 (§5.1 criterio 3).

De T-25, contra la base real y con los datos de demostración cargados: el odontólogo 3 ve **solo sus
dos órdenes**, el detalle de una ajena responde **404** —igual que una inexistente—, agregar
`?odontologoId=` al listado **no cambia el resultado** (RN-01), y la respuesta al odontólogo **no
trae `pacienteNombre`** en ningún campo (RN-22). Cancelar una orden entregada dio `409
ORDEN_NO_CANCELABLE`; cancelar una propia en curso funcionó y dejó `cargo_cancelacion` en **null**
(§5.6 criterio 3). La línea de tiempo trae cada etapa con fecha, hora y autor, y el registro inicial
sin autor porque lo asigna el sistema (§5.1 paso 9).

De T-26, contra la base real: `GET /admin/ordenes` devuelve las **cinco** órdenes de los dos
odontólogos, **sin `pacienteNombre`** (RN-22 vale también para el listado del laboratorio), y `403`
con rol ODONTOLOGO. Los filtros se combinan —`tipoOrden=URGENTE&odontologoId=17` deja una sola— y un
tipo inexistente da `400 TIPO_ORDEN_INVALIDO` en vez de un 500. En el detalle, `siguienteEstado`
trae `CONTROL_CALIDAD` para una orden en producción y **`null`** para la entregada y la cancelada.
Desde el administrador, saltear etapas, retroceder, cancelar y tocar una orden entregada devuelven
los cuatro `409` de §5.5, y **ninguna de las cinco órdenes cambió de estado ni sumó etapas**.

De T-27, contra la base real y con las cinco órdenes de demostración intactas: el panel del
odontólogo 3 devolvió `1 / 0 / 1` con sus dos órdenes y el del 17 `2 / 0 / 0` con las suyas tres
—la cancelada no cuenta como en curso—; agregar `?odontologoId=17` al panel del 3 **no cambió
nada** (RN-01). El dashboard del laboratorio devolvió `3 / 0 / 1 / 1`, las **siete** etapas de la
distribución con las que están en cero incluidas y en orden de `orden_secuencia`, las próximas a
entregar por fecha estimada ascendente y una sola urgente. Un `grep pacienteNombre` sobre las dos
respuestas completas dio **0 ocurrencias** (RN-22), incluida la urgente, cuya vista sí lo trae.
Rol cruzado: odontólogo → `/admin/dashboard` **403**, admin → `/dashboard` **403**, sin token
**401**. De CU-12, `historico=true` dejó solo la cancelada del odontólogo 17 y solo la entregada
del 3, `historico=true&estado=ENTREGADO` devolvió vacío para el 17, y sin el parámetro el listado
de CU-03 siguió trayendo las mismas tres. **La zona horaria se probó cambiándola**: levantada la
misma base con `LAB_ZONA_HORARIA=UTC`, `entregadasEstaSemana` pasó de **1 a 0** —la entrega del
domingo se escapa de la semana— y con `America/Montevideo` volvió a contar. **La verificación no
dejó residuo:** no se insertó, modificó ni borró ninguna fila.

De T-34, contra la base real: `GET /configuracion-notificaciones` sin configuración previa devolvió
los canales por defecto de §6.3 —app, correo y Telegram— con `fechaActualizacion` nula, no un 404.
El `422 TELEGRAM_SIN_DESTINO` llega con **`campo: "telegramChatId"`**, que es lo que hace que la
pantalla lo pinte en el campo correcto, tanto con el chat vacío como con espacios en blanco. Un
guardado válido apagó el correo y devolvió la configuración con su fecha. **P-18 verificado del
lado del backend:** mandando `canalWhatsappActivo: true` a mano, la respuesta vuelve en `false`.
Con rol ODONTOLOGO, `GET` y `PUT` dan **403**; sin token, **401**. **La verificación no dejó
residuo:** la tabla `configuracion_notificacion` estaba vacía, el `PUT` creó una fila y se borró al
terminar — volvió a **0 filas**, verificado.

**Datos de prueba deliberados en la base de desarrollo** (se conservan para mostrarle el flujo a la
clienta): dos solicitudes de `juan.prueba@mail.com` —una `RECHAZADA` y otra `APROBADA`—, el
odontólogo `jperez` (id 17, contraseña ya cambiada) y las notificaciones de ambos flujos.
**Cinco órdenes de demostración, cargadas el 23/08/2026** a pedido del desarrollador, por los mismos
endpoints que usan las pantallas —**nada se insertó por SQL**—, y **repartidas entre dos odontólogos**
para poder verificar RN-01:

| Código | Dueño | Tipo | Estado | Etapas |
|---|---|---|---|---|
| LG-0003 | Dr. Ernesto Pérez (id 3) | Normal | En producción | 3 |
| LG-0004 | Dr. Ernesto Pérez (id 3) | Urgente | Entregado | 5 |
| LG-0005 | Dr. Juan Pérez (id 17) | Normal | Recibido | 1 |
| LG-0006 | Dr. Juan Pérez (id 17) | Normal | **Cancelado** por su dueño | 3 |
| LG-0007 | Dr. Juan Pérez (id 17) | Urgente | En producción | 2 |

Sus notificaciones también son reales: 20 correos en mailpit, y dos avisos entregados por Telegram a
la cuenta vinculada. **`erneskrlos` (id 11) no puede ser dueño de una orden**: es SUPERADMIN y
`POST /ordenes` exige rol ODONTOLOGO. La cuenta de odontólogo equivalente es `eperez` (id 3).

Las verificaciones de T-32, T-32b y T-33b, en cambio, **no dejaron residuo**: las solicitudes de
prueba, el `telegram_chat_id` falso que se cargó para probar el envío y las dos órdenes de
verificación se borraron al terminar.

**Vinculación real de Telegram:** `erneskrlos` (id 11) está vinculado desde el 23/08/2026, por la
corrida del propio desarrollador. No es dato de prueba: es el estado que dejó el flujo de §6.5.

---

## (b) Próxima tarea

### T-28 · Perfil, odontólogos y repaso final — **última tarea del plan**

**Spec:** §7, §9 · **Depende de:** T-27 ✅
**Terminado cuando:**
- **Perfil editable**: `PUT /api/v1/perfil` y el formulario, con `nombreCompleto` y `direccion`.
  **No** rol ni correo. *La pantalla `/perfil` y `GET /api/v1/perfil` ya existen desde T-32b:
  T-28 la **extiende**, no la crea.*
- **Listado de odontólogos** para el admin (CU-11) y **gestión de usuarios** para el SuperAdmin
  (CU-17): `GET /api/v1/odontologos` **paginado**, `GET /api/v1/usuarios` y
  `PATCH /api/v1/usuarios/{id}/estado`.
- **Repaso de la tabla de trazabilidad de §9**: cada regla con su implementación verificable.
- **Repaso de que ningún punto fuera de alcance (`Agente.md` §3.3) quedó implementado.**

#### Los tres pendientes que hereda

1. **`/admin/odontologos` es hoy una `PantallaPendiente`** puesta por T-26 — es el **último**
   destino del menú del admin sin su pantalla. La reemplaza el listado de CU-11, con §8.1 **Regla 1
   completa** (es un CRUD de verdad) y el botón "Nuevo" apuntando a `/admin/odontologos/nuevo`, que
   **ya existe** desde T-31.
2. **Mover el destino del alta de odontólogo de `/admin` a `/admin/odontologos`.** Hoy
   `OdontologoFormulario` vuelve al dashboard con su confirmación en `location.state`
   (`DESTINO_POR_DEFECTO`), que es un destino intermedio: §8.1 Regla 1 pide volver **al listado**.
   Cuando exista, hay que cambiar esa constante y mostrar el mensaje ahí, como ya hacen
   `/admin/ordenes` y `/admin/tipos-trabajo`. **El dashboard deja de necesitar su `role="status"`.**
3. **`GET /api/v1/odontologos` paginado no existe todavía.** Lo que hay es `/odontologos/activos`,
   que alimenta un **selector** y devuelve solo `id` y `nombreCompleto` (decisión 14). **Son dos
   endpoints con propósitos distintos y conviven** — §7 lo dice explícitamente.

#### Para el repaso de §9: lo que se agregó o cambió durante CR-01

La tabla de §9 se escribió antes de estas ocho incorporaciones, así que **el repaso tiene que
contemplarlas** o va a dar por completo algo que no lo está. Todas están documentadas en `spec.md`
—ninguna vive solo acá— y cada una tiene su decisión en la sección (c):

| Qué | Dónde está en `spec.md` | Decisión |
|---|---|---|
| `GET /odontologos/activos` (sin paginar, selector de D-19) | §7, §5.1 | 14 (T-33b) |
| `GET /dashboard` del odontólogo (CU-02) | §5.7, §7 | 18 (T-27) |
| `siguienteEstado` en el detalle de la orden (RN-04 desde el backend) | §5.4 | 16 (T-26) |
| `?historico=true` en `GET /ordenes` (CU-12) | §5.3 | 18 (T-27) |
| `GET /perfil` | §7, §6.5 | 13 (T-32b) |
| Exención de paginado ampliada a los endpoints que alimentan selectores | §4 (nota tras §4.1) | 14 (T-33b) |
| `CUENTA_INACTIVA`, renombrado desde `CUENTA_NO_VERIFICADA` por D-18 | §3.3 | CR-01 / T-29 |
| `app.laboratorio.zona-horaria` / `LAB_ZONA_HORARIA`, obligatoria en `prod` | **§1.2** (sección nueva) | 18 (T-27) |

**Además, para el repaso de `Agente.md` §3.3** (nada fuera de alcance implementado), los tres
puntos donde es más fácil equivocarse: **P-14** (la columna `cargo_cancelacion` existe y queda en
`null`, sin lógica de cálculo), **P-18** (WhatsApp es estructura: puerto y validación de teléfono,
sin integración) y **D-11** (la tabla de mensajes existe y **no hay** entidad, servicio, endpoint,
pantalla ni contador — el de CU-02 se omitió a propósito).

> **Los dos puntos que se elevan a la clienta no los resuelve T-28**: la deuda de spec de T-32 y el
> punto abierto de T-34 sobre `canal_app_activo`. Los dos están en "Puntos abiertos" y son la misma
> pregunta sobre qué significa "canal activo".

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

### 9. Las cuatro decisiones de T-23 (20/08/2026)

Las tomó el desarrollador antes de implementar, sobre los cuatro puntos que §6.4 y §8 no cierran:

1. **El listado va en un panel desplegable colgado de la campana, sin ruta nueva.** La tabla de
   pantallas de §8 es cerrada y `/notificaciones` no está en ella. **§8.1 no aplica**: rige vistas
   de administración con alta, edición y listado, y las notificaciones no tienen las dos primeras.
   Lo que sí rige es `Agente.md` §6.2: **el panel consume el endpoint paginado del backend**
   (`size=10`, anterior/siguiente dentro del panel). **Prohibido traer todo y cortar en el cliente.**
2. **La campana vive en un layout compartido, sin menú de navegación.** `LayoutAutenticado` en
   `shared/` envuelve todas las rutas con sesión: montarla solo en `Inicio` la dejaría invisible en
   el resto y obligaría a mover código ya entregado. **El menú de §8 lo arma la tarea que cree sus
   destinos** — hoy enlazaría a rutas inexistentes.
3. **Sin control de filtro leídas / no leídas.** El endpoint acepta `?leidas=`, pero ninguna sección
   pide el control en pantalla. Las no leídas se distinguen visualmente (negrita + etiqueta "Sin leer").
4. **El `ordenId` se muestra como dato, sin enlace.** *Esto ajusta el punto 3 de la sección (b)
   anterior, que decía enlazar al detalle.* Con el orden vigente, T-25 llega recién después de T-30,
   T-31, T-32, T-32b y T-33: un enlace muerto viviría cinco tareas, y los criterios de T-23 no piden
   navegar a la orden.

### 10. Las nueve decisiones de T-30 (21/08/2026)

Los dos primeros puntos los decidió el desarrollador; el resto se resolvió al implementar.

1. **La gestión de solicitudes es de ADMIN y SUPERADMIN.** §3.1.b dice `· ADMIN` a secas, pero
   todos los demás endpoints de administración dicen "ADMIN, SUPERADMIN" y el SUPERADMIN **recibe**
   el aviso de cada solicitud (decisión 4 de T-21): con la lectura literal le llegaría un aviso de
   algo que no puede abrir.
2. **La pantalla lleva filtro por estado**, arrancando en Pendientes. Es el único caso donde se
   agregó un control que ninguna sección pide de forma explícita: §3.1.b documenta el parámetro
   con el ejemplo `?estado=PENDIENTE`, que apunta justo a ese valor por defecto. *(No contradice la
   decisión 3 de T-23: ahí no había parámetro documentado.)*
3. **Teléfono validado como E.164** (`^\+[1-9]\d{7,14}$`): §3.1 pide "formato internacional" sin
   definirlo, y el ejemplo `+59891234567` de la spec valida contra ese patrón.
4. **Texto del aviso:** *"Nueva solicitud de acceso de {nombre}."*, asunto *"Lab. Garcia's Connect
   — Nueva solicitud de acceso"*. Calcado del formato de CU-07, como la decisión 6 de T-21.
5. **`SOLICITUD_YA_RESUELTA` (409)** al rechazar una solicitud ya aprobada o rechazada. §3.1.b no
   documenta las respuestas del rechazo y el caso había que cubrirlo.
6. **El filtro `estado` se recibe como texto y se convierte en el service**, con
   `ESTADO_SOLICITUD_INVALIDO` (400). Si lo convirtiera Spring, un valor cualquiera caería en el
   manejador genérico y devolvería 500.
7. **`SolicitudAccesoEvent` lleva solo id y nombre.** El mensaje de una notificación se persiste y
   viaja por correo y Telegram: correo, dirección y teléfono se consultan en la pantalla.
8. **La solicitud NO está exenta del filtro de licencia.** La lista de excepciones de §3.6 es
   cerrada y no la incluye: con licencia vencida, el formulario público responde `423`.
9. **`usePaginacion` se extendió con `filtro`/`cambiarFiltro`** en lugar de manejar el parámetro
   suelto en la pantalla (Agente.md §6.2): el filtro vive en la URL igual que `page` y `size`, y
   cambiarlo vuelve a `page=0`.

**Sobre §8.1 en el listado de solicitudes:** rigen las Reglas 2 a 5, no la Regla 1. Las solicitudes
nacen del formulario público: no hay alta ni edición, así que no existen `/nuevo` ni `/{id}/editar`.
Mismo criterio que se fijó para la campana en T-23.

### 11. Ampliación de alcance de T-31 y sus decisiones (21/08/2026)

**La ampliación, autorizada por el desarrollador antes de implementar:** `Plan.md` acota T-31 a los
cuatro criterios de §3.1.b más la pantalla `/cambiar-password`, y deja `/admin/odontologos` (CU-11)
en **T-28**. Con esa lectura literal, `POST /odontologos` no tenía ninguna interfaz —solo Swagger— y
la bandeja de solicitudes entregada en T-30 quedaba sin salida durante cinco tareas. Se agregaron a
T-31, y **solo** esto:

- **`/admin/odontologos/nuevo`**: el formulario de alta, con la convención de §8.1 Regla 1
  (`LayoutFormulario` y `CampoFormulario` de `shared/`, guardar vuelve al origen con confirmación,
  cancelar vuelve sin guardar).
- **Botón "Crear cuenta"** en cada solicitud `PENDIENTE` de `/admin/solicitudes`, que abre ese
  formulario con `nombreCompleto`, `correo`, `direccion` y `telefono` precargados y el `solicitudId`
  en mano. No es un flujo de aprobación nuevo: aprobar **es** crear la cuenta con `solicitudId`,
  como define §3.1.b.

> **T-28 hereda solo el listado de CU-11** (`GET /api/v1/odontologos` y la pantalla
> `/admin/odontologos`), con su botón "Nuevo" apuntando al formulario que ya existe.
>
> **Criterio para futuras ampliaciones** *(fijado por el desarrollador)*: se justifican con la spec y
> el plan —un criterio de aceptación no verificable, o una pantalla ya entregada que queda sin
> salida—, **nunca** con la proximidad de una reunión con la clienta.

**Las decisiones de implementación:**

1. **El token restringido es un claim del JWT** (`debeCambiarPassword`) más
   `CambioPasswordObligatorioFilter`, que rechaza toda ruta que no sea `POST /auth/cambiar-password`
   con `403 CAMBIO_PASSWORD_REQUERIDO`. Va en el token y no en el cuerpo ni en un header: el cliente
   no puede sacárselo. Se descartó emitir el token con un rol falso, que ensuciaría RN-14.
2. **El correo de credenciales se registra con el envío ya resuelto**, no `PENDIENTE`
   (`NotificacionService.registrarConEnvioResuelto`). Dejarlo pendiente haría que el despachador
   mandara un segundo correo, esta vez con el texto genérico.
3. **`CanalCorreo` expone `enviarCorreo(destino, asunto, cuerpo)`** para ese envío directo, en vez de
   abrirle una conexión SMTP propia al listener: el servidor de correo se configura en un solo lugar.
4. **`CredencialesCreadasEvent.toString()` enmascara la contraseña.** Es la vía de fuga más probable:
   cualquier log del evento, o una excepción que lo incluya, la imprimiría en claro.
5. **La contraseña generada es de 12 caracteres**, por encima del mínimo de 9 de RN-15, que fija un
   piso y no una longitud exacta. Se arma con un carácter obligatorio de cada categoría y se mezcla
   con Fisher-Yates sobre `SecureRandom`, para que cumplir la regla no dependa del sorteo.
6. **`PASSWORD_ACTUAL_INCORRECTA` responde 422, no 401.** La sesión es válida; lo que no coincide es
   el dato que mandó el usuario. Un 401 haría que el cliente lo interpretara como sesión vencida.
7. **La pantalla de cambio no tiene "Cancelar"**, solo "Cerrar sesión": con el cambio pendiente
   ninguna otra pantalla responde, así que cancelar no llevaría a ningún lado.
8. **`RutaProtegida` redirige a `/cambiar-password`** mientras la sesión tenga la bandera. Es la
   mitad visible de la regla; la que manda es el filtro del backend.

### 12. Las decisiones de T-32 (21/08/2026)

**La decisión de alcance, tomada por el desarrollador antes de implementar:** el envío usa
**`usuario.telegram_chat_id`** (V2, D-21, §6.3) y **CU-21 queda exactamente como la entregó T-22**.
Se descartó cambiar §6.4 para que activar Telegram exigiera estar vinculado —que dejaría el sistema
más coherente— porque sería reinterpretar una regla escrita y modificar una tarea ya cerrada
(`Agente.md` §3.1). La incoherencia queda anotada abajo como deuda de spec y la eleva el
desarrollador a la clienta.

**Las decisiones de implementación:**

1. **Cliente HTTP: `RestClient` de `spring-web`**, que ya venía con `spring-boot-starter-web`.
   **No se agregó ninguna dependencia** (`Agente.md` §3.4); se descartó una librería de terceros
   para Telegram, que traería el flujo de bot entero para usar un solo endpoint.
2. **El token del bot no sale del adaptador** (§6.5 criterio 4). Viaja en la URL, así que **ningún
   mensaje de la librería HTTP se propaga tal cual**: `ResourceAccessException` incluye la URI
   completa y terminaría copiada en `notificacion_envio.detalle_error`, a la vista de cualquiera que
   abra la base. Cada fallo se traduce a un texto propio: el motivo que devuelve Telegram cuando lo
   hay, y uno genérico cuando el fallo es de red. Hay dos tests que lo vigilan.
3. **Una respuesta `200` con `ok:false` cuenta como fallo.** La Bot API contesta así en algunos
   casos; darla por buena dejaría el envío en `ENVIADO` sin que el mensaje haya salido.
4. **Timeouts de conexión y lectura en `spring.http.client`** (5 s), por el mismo motivo que los del
   SMTP: el despachador llama desde un hilo programado y una API que no responde bloquearía el
   despacho de todos los demás canales.
5. **Se declaró solo `telegram.bot.token`.** `telegram.bot.username` lo pide §6.5 para el enlace
   profundo, que es T-32b; declararlo ahora dejaría una property sin uso (`Agente.md` §3.3).
6. **El envío exige `telegram_vinculado = true` además del `chat_id`.** §6.3 solo nombra el chat,
   pero §6.5 criterio 3 pide que desvincular detenga los envíos: si quedara un chat viejo con la
   bandera apagada, se le escribiría a alguien que pidió no recibir más.
7. **`usuario.telegram_chat_id` y `telegram_vinculado` se mapearon sin setter.** T-32 solo los lee;
   los escribe la vinculación de T-32b, que es la tarea que los necesita.
8. **`CanalWhatsApp` ahora valida el teléfono** antes de fallar por falta de proveedor, como pide
   §6.3. Son dos motivos distintos —"sin destino" y "sin proveedor"— con dos responsables distintos,
   y `detalle_error` tiene que poder distinguirlos. Sigue **sin integrar nada** (P-18).
9. **`CanalesDeEstructuraTest` se reescribió, no se borró.** Quedó cubriendo lo que sigue sin
   integrarse —WhatsApp— con el mismo criterio de antes; los tests de Telegram se mudaron a
   `CanalTelegramTest`, que usa `MockRestServiceServer`.

### 13. Las decisiones de T-32b (22/08/2026)

**Las decisiones de alcance, tomadas por el desarrollador antes de implementar:**

- **La pantalla `/perfil` la crea T-32b, con la sección de Telegram, y también `GET /api/v1/perfil`
  (§7).** El `GET` no es ampliación: sin una lectura del estado, "el perfil muestra si está
  vinculado" no es verificable. **`PUT /perfil` y la edición de nombre y dirección quedan en T-28**,
  que **extiende** esta pantalla en lugar de crearla — `Plan.md` T-28 quedó anotado con eso.
- **El token de vinculación vence a los 15 minutos**, calculados sobre `fecha_emision`, sin
  migración. La constante es `TelegramTokenVinculacion.MINUTOS_VIGENCIA_TOKEN_TELEGRAM`. Un token
  vencido no vincula y el bot responde el mismo error que uno usado o inexistente (§6.5 criterio 2).

**Las decisiones de implementación:**

1. **`ClienteTelegram` concentra todo el trato con la Bot API** —`sendMessage` y `getUpdates`—, y
   `CanalTelegram` pasó a delegarle. Las dos llamadas comparten el token y la regla de que ningún
   mensaje de la librería HTTP se propaga; duplicarlo era duplicar la única vía de fuga del secreto.
2. **La vinculación vive en `notificaciones`, no en `seguridad`.** §6.5 es una subsección del módulo
   de notificaciones y ahí está el bot. La escritura de `usuario.telegram_chat_id` se delega a
   `UsuarioService`, que es el dueño de la entidad (Agente.md 5.4); al revés habría creado el ciclo
   entre módulos que esa misma regla prohíbe.
3. **El `offset` de `getUpdates` vive en memoria.** Pedir las novedades con un offset las **confirma**
   contra Telegram, que es lo que hace que no se vuelvan a entregar: persistirlo sería una columna
   nueva —una migración— para un dato que Telegram ya retiene. Tras un reinicio se releen las no
   confirmadas, y no pasa nada: el token es de un solo uso.
4. **El offset avanza también con las novedades que no son mensajes de texto.** Si se filtraran antes
   de contarlas, Telegram las devolvería en cada corrida para siempre.
5. **Token de 32 caracteres**, 24 bytes de `SecureRandom` en base64 URL-safe: entra en el
   `VARCHAR(64)` de V2 y en el límite del parámetro `start` del enlace profundo.
6. **El bot ignora en silencio todo lo que no sea `/start {token}`.** §6.5 no define ninguna otra
   conversación e inventarle respuestas sería inventar alcance.
7. **El texto de éxito del bot es el de §6.5, palabra por palabra**; el de error no está documentado
   y se calcó del formato, como ya se hizo en `TextosNotificacion`.
8. **`spring.http.client` con 5 s de timeout** ya estaba desde T-32 y ahora también acota el
   `getUpdates`: el polling corre en un hilo programado.
9. **Mientras hay un enlace pedido y la cuenta sigue sin vincular, el perfil vuelve a consultarse
   cada 5 s.** El chat lo captura el backend cuando el usuario toca Iniciar, así que la pantalla no
   se entera sola; sin esto habría que refrescar a mano y el criterio 1 ("en segundos") no se vería.

### 14. Las decisiones de T-33b (23/08/2026)

**La decisión de alcance, tomada por el desarrollador antes de implementar:** el selector de
odontólogo se alimenta de **`GET /api/v1/odontologos/activos`**, sin paginar, ADMIN y SUPERADMIN,
devolviendo **solo `id` y `nombreCompleto`**. La alternativa —consumir el listado paginado de
CU-11— se descartó porque no es una limitación sino un defecto: con 31 odontólogos el administrador
no podría registrar la orden del último, y acumular páginas en el cliente lo prohíbe `Agente.md`
§6.2. **La frase de la exención de paginado de §4 quedó reescrita** con autorización explícita: la
exención cubre los endpoints que alimentan selectores donde se necesita el catálogo completo.

> **T-28 no hereda menos por esto.** Sigue implementando `GET /api/v1/odontologos` **paginado**
> (§7, CU-11) y la pantalla `/admin/odontologos` con §8.1 Regla 2. Son dos endpoints con propósitos
> distintos: **uno alimenta un selector, el otro una tabla administrable**, con acciones sobre cada
> cuenta y datos que un selector no necesita.

**Las decisiones de implementación:**

1. **`OdontologoActivoResponse` va aparte de `OdontologoResponse`.** Aquella es la ficha de la cuenta
   creada y lleva correo, dirección y teléfono; devolver el padrón de contacto del laboratorio en
   cada apertura del formulario sería regalarlo.
2. **Solo cuentas `ACTIVA`**: son las únicas que `POST /ordenes` acepta como dueño. Ofrecer una dada
   de baja sería ofrecer un alta que va a fallar.
3. **La pantalla no calcula ni anticipa nada** (`Agente.md` §6.1): precio, recargo, estado inicial y
   fecha estimada aparecen recién en la confirmación, tal como los devolvió el `201`. Hay un test que
   lo vigila.
4. **La confirmación no nombra al paciente** (§5.1 criterio 4, RN-22): dice código, entrega estimada
   y total.
5. **Al guardar se vuelve a `/`**, que hoy es un inicio genérico para cualquier rol autenticado, no
   la pantalla del odontólogo. El listado `/admin/ordenes` al que debería volver es de T-26.
6. **`Inicio` ahora muestra la confirmación** que le llega por `location.state`. Estaba previsto por
   §8.1 Regla 1 pero no implementado: el mensaje de T-31 se perdía en silencio al volver.
7. **El punto 2 de la tarea se cumplió no construyendo.** No hay ninguna ruta de órdenes del
   odontólogo ni menú de navegación, así que no había "Nueva orden" que retirar; queda anotado para
   T-25, que es quien arma ese menú.

### 15. Las decisiones de T-25 (23/08/2026)

**La decisión de alcance, tomada por el desarrollador antes de implementar:** el `ordenId` de la
campana es **enlace solo para el ODONTOLOGO**. Se descartó abrir `/ordenes/:id` al administrador
porque esa pantalla muestra el botón de cancelar, que §5.6 reserva al propietario: habría que
condicionarlo por rol dentro de una pantalla que §8 asigna a un solo rol. **El destino del admin es
su propio detalle, en T-26.**

**Las decisiones de implementación:**

1. **El menú vive en `LayoutAutenticado` a través de un hueco `navegacion`**, igual que la campana.
   El componente compartido sigue sin importar nada de `features/` (Agente.md §5.4.3), y cada tarea
   monta el menú cuyos destinos ya existen. Hoy se monta solo el del odontólogo.
2. **El filtro de estado del listado se resuelve en el backend** (`?estado=`, §5.3) y vive en la URL
   junto con `page` y `size` (§8.1 Regla 2). Nunca se corta una lista ya traída (Agente.md §6.2).
3. **Los estados terminales se leen de la tabla `estado`** (`esTerminal`), no de una lista escrita en
   el frontend: es RN-04 y lo define la base. Si el catálogo todavía no llegó, se muestra el botón y
   la última palabra la tiene el backend con su `409`.
4. **Cancelar pide una confirmación explícita en la propia pantalla**, no un diálogo del navegador:
   es irreversible y no hay pantalla de edición para deshacerlo (RN-17).
5. **La descarga de adjuntos va por `fetch` y blob**, no por un enlace directo. Ver la restricción
   técnica en los puntos abiertos.
6. **La pantalla no formatea importes.** Se muestran como los devuelve el backend, sin símbolo de
   moneda, porque P-17 no está resuelto.
7. **El test de T-23 que fijaba "el `ordenId` no es enlace" se reemplazó**, no se borró: ahora hay dos
   —uno por rol— que fijan la regla nueva. Es el mismo criterio que se usó con
   `CanalesDeEstructuraTest` en T-32.

### 16. Las decisiones de T-26 (23/08/2026)

**La decisión de contrato, tomada por el desarrollador antes de implementar:** el detalle de la
orden suma **`siguienteEstado`** (código y nombre), y **el campo se documentó en `spec.md` §5.4**,
no solo acá. El criterio: `ESTADO.md` es para decisiones internas; **§5.4 es el contrato de la API,
y un campo nuevo en una respuesta es contrato** — además **T-27 puede consumirlo** para el
dashboard, y dejarlo en una nota interna garantizaría que se redescubra.

- Lleva **nombre además de código** porque `estado.nombre` es editable por CU-22: derivarlo del
  código en el cliente rompería la pantalla al renombrar una etapa.
- **Va solo en el detalle**, no en el listado: el listado no tiene botón de avance y calcularlo para
  30 filas sería trabajo sin uso.
- La lógica no se duplicó: `EstadoService.siguienteEnFlujo` lee `orden_secuencia`, la misma regla
  que ya valida `OrdenEstadoService` (RN-04).

**Las decisiones de implementación:**

1. **`AdminOrdenController` va aparte de `OrdenController`.** Son dos vistas del mismo recurso con
   reglas opuestas: una devuelve solo las del odontólogo autenticado (RN-01) y la otra las de todos.
   Que cada una tenga su ruta y su autorización es lo que hace evidente cuál es cuál.
2. **El listado del laboratorio no devuelve `pacienteNombre`.** `Agente.md` §8.2 es literal:
   ninguna respuesta expuesta a listados lo incluye. El admin lo ve en el detalle (§5.4), que es la
   **única pantalla del sistema** donde ese dato aparece.
3. **El detalle del admin es una pantalla propia**, no la del odontólogo con un `if` por rol: la del
   odontólogo tiene el botón de cancelar que §5.6 le reserva al propietario, y la del admin muestra
   el nombre del paciente. Mezclarlas obligaría a condicionar las dos cosas por rol.
4. **Un `tipoOrden` inexistente responde `400 TIPO_ORDEN_INVALIDO`** en vez de reventar el `valueOf`
   con un 500. Mismo criterio que el filtro de estado de las solicitudes en §3.1.b.
5. **`abrirDescarga` se extrajo a `shared/util/descargaArchivo`**: los dos detalles la necesitan y
   duplicarla era duplicar la única parte delicada de la descarga (`Agente.md` §6.2).
6. **Los enlaces sueltos del inicio se retiraron.** La navegación es el menú de §8, uno por rol;
   mantener dos listas de enlaces las desincroniza.

### 17. `/inicio` es del odontólogo y `/` redirige por rol (23/08/2026)

**Decisión del desarrollador, tomada antes de implementar T-27.** Cierra el punto abierto que había
dejado T-26 y que la ficha de T-27 arrastraba:

- **`/inicio` es la ruta del panel del odontólogo (CU-02)**, como manda §8. Se crea aparte, no se
  reusa `/`.
- **`/` deja de ser un inicio genérico y redirige según el rol:** `ODONTOLOGO` → `/inicio`,
  `ADMIN` y `SUPERADMIN` → `/admin`.
- **Ningún usuario queda en una pantalla compartida.** La pantalla `Inicio` que hoy vive en `App.jsx`
  —saludo por nombre y rol— desaparece; su función de destino de las confirmaciones la heredan las
  dos pantallas por rol.

Arrastra el ítem "Inicio" del menú del odontólogo (`/` → `/inicio`) y el destino de los formularios
de administración que hoy vuelven a `/` con su mensaje en `location.state`.

### 18. Las decisiones de T-27 (23/08/2026)

**Las decisiones de alcance y contrato, tomadas por el desarrollador antes de implementar.** T-27
llegaba con cinco puntos que ninguna fuente definía; se resolvieron así y **los tres cambios de
contrato quedaron en `spec.md`**, no solo acá:

1. **`GET /api/v1/dashboard` (ODONTOLOGO) es nuevo y no estaba en la spec.** §5.7 solo definía el
   del laboratorio. Sin una lectura del backend, "el panel muestra sus contadores" no era
   verificable, porque §8 prohíbe calcularlos en el cliente. Mismo criterio que `GET /perfil` en
   T-32b y `/odontologos/activos` en T-33b. **Documentado en §5.7 y en la tabla de §7.**
2. **"Entregadas esta semana" se cuenta por el pasaje a `ENTREGADO` del historial**, porque `orden`
   no tiene fecha de entrega real, con la semana calendario de lunes a domingo y rango semiabierto.
   **El corte va en la zona horaria del laboratorio, no en UTC** (ver decisión 4 de abajo).
3. **"En curso" excluye `LISTO`.** Los cuatro contadores **no son una partición** —"urgentes
   activas" ya se solapa con cualquier definición de en curso, y "entregadas esta semana" es
   histórico, no un estado—, así que no hay ningún total que preservar. Lo decisivo es la lectura:
   CU-02 pone "en curso" y "listos para retirar" uno al lado del otro, y si un trabajo listo
   apareciera en los dos, quien lee "3 en curso, 1 listo" no sabría si tiene 3 o 4 trabajos
   abiertos. **Terminal se lee de `es_terminal` y `LISTO` de su código**, ambos de la tabla.
4. **Los bloques de resumen traen 5 filas**, en una constante con su referencia. No son listados
   paginados de §8.1.
5. **CU-12 es el mismo endpoint con `?historico=true`**, no uno nuevo: es el mismo listado del
   mismo dueño con una condición más, y duplicar la ruta duplicaría RN-01. **El filtro entre
   ENTREGADO y CANCELADO reutiliza el `estado` que ya existía**; no se inventó un parámetro.
   Terminal sale de `es_terminal`. **Documentado en §5.3.**

**Las decisiones de implementación:**

1. **`DashboardController` y `AdminDashboardController` van separados.** Mismo criterio que fijó
   T-26 entre `OrdenController` y `AdminOrdenController`: dos vistas con reglas opuestas, cada una
   con su ruta y su autorización, que es lo que hace evidente cuál es cuál.
2. **Las vistas se leen con consulta nativa y proyección de interfaz, sin `@Entity`.** Mapearlas
   como entidad las volvería escribibles y pondría a `ddl-auto: validate` a vigilar una tabla que
   no existe. **Los alias van entrecomillados** (`AS "estadoCodigo"`): PostgreSQL pasa a minúsculas
   los que no lo estén y la proyección dejaría de mapear en silencio.
3. **`MapeadorOrden` se extrajo de `OrdenService`.** Lo necesitan dos services del módulo, y
   `identificacionPaciente` es el único punto donde se aplica RN-22 a un listado: duplicarlo era
   duplicar esa regla. De paso `OrdenService` bajó de 237 a 213 líneas.
4. **La zona horaria vive en `app.laboratorio.zona-horaria` / `LAB_ZONA_HORARIA`**, aislada en
   `SemanaLaboratorio`. **Obligatoria en `prod`, sin default**, y documentada en la **§1.2 nueva**
   de `spec.md` junto a las properties de Telegram: es configuración de instalación que setea el
   desarrollador, no el agente, igual que P-20.
5. **`TablaPaginada` se extendió en vez de crear una tabla propia de los paneles** (§8.1 Regla 4,
   `Agente.md` §6.2): sin `onCambiarPagina` no dibuja controles. Los bloques de resumen son 5 filas
   cerradas, y una paginación de una sola página sería un control que no lleva a ningún lado.
6. **"Cerrar sesión" subió al encabezado compartido** (`BotonCerrarSesion`). Vivía solo en la
   pantalla `Inicio` de `/`, que esta tarea retira; sin moverlo, **CU-14 se quedaba sin interfaz**
   salvo en la pantalla de cambio de contraseña obligatorio. Mismo razonamiento con el que T-23
   puso ahí la campana.
7. **El alta de odontólogo vuelve a `/admin`**, no a `/`: el `location.state` con la confirmación
   no sobrevive a un redirect. Es un destino intermedio (ver puntos abiertos).
8. **El selector del historial se alimenta de `esTerminal` del catálogo**, la misma fuente que usa
   el backend. Si mañana otra etapa se marca terminal, aparece sola.
9. **La barra de la distribución es proporcional al mayor valor de la serie.** Es presentación: la
   cifra se muestra al lado tal como la devolvió el backend, sin recomponer.

### 19. Las decisiones de T-34 (24/08/2026)

**Las dos decisiones de comportamiento, tomadas por el desarrollador antes de implementar.** §8.1
Regla 1 no aplica a esta pantalla —no hay listado, alta ni edición por id— y nada más definía estos
dos puntos:

1. **Guardar deja al usuario en la pantalla**, con la confirmación en un `role="status"`, y
   **Cancelar vuelve a `/admin`**, que es de donde se llega por el menú. Es una pantalla de ajustes
   que se toca varias veces seguidas, no un alta que se completa y se cierra: expulsar al usuario
   al dashboard tras tildar una casilla lo obligaría a volver a entrar para ver cómo quedó. Mismo
   criterio con el que T-31 decidió que `/cambiar-password` no tuviera "Cancelar": el destino se
   define por dónde tiene sentido dejar al usuario, no copiando la regla de otro tipo de pantalla.
2. **La ayuda del campo del chat dice solo la regla documentada** (*"CU-21: obligatorio si activás
   Telegram."*) y **no menciona la vinculación de §6.5**. Explicar ahí que los envíos usan la
   cuenta vinculada desde el perfil sería **resolver la deuda de spec de T-32 por la vía del
   texto**, describiendo en la interfaz un comportamiento que §6.4 no define.

**Las decisiones de implementación:**

1. **El formulario se refresca con lo que devolvió el `PUT`, no con lo que se mandó.** Es la
   respuesta del backend la que dice cómo quedó la configuración —por ejemplo, `canalWhatsappActivo`
   vuelve en `false` aunque se hubiera mandado en `true`—, y mostrar el estado local sería mostrar
   lo que el usuario quiso, no lo que quedó guardado.
2. **La pantalla no adelanta el rechazo de CU-21.** Con Telegram tildado y el chat vacío, manda
   igual y pinta el `422` que devuelve el backend en el campo del chat. Reimplementar la validación
   en el cliente duplicaría una regla de negocio (`Agente.md` §6.1); hay un test que lo fija.
3. **Las tres banderas viajan siempre en el `PUT`, también las apagadas**, porque reemplaza la
   configuración entera (decisión 2 de T-22). **`canalWhatsappActivo` no se manda nunca**: P-18, y
   el request del backend ni siquiera lo acepta.
4. **`mutationFn` va envuelta** (`(payload) => guardar(payload)`) y no pasada directo: TanStack
   Query agrega un segundo argumento con su contexto interno y el cliente de API no tiene por qué
   recibirlo. Es el patrón que ya usaban `OdontologoFormulario` y `TipoTrabajoFormulario`.
5. **La ayuda de RN-19 va una sola vez**, bajo la primera casilla, y no repetida en las cuatro: es
   la misma regla para todo el grupo.
6. **`MenuAdmin` solo cambió de comentario.** El `NavLink` ya apuntaba a `/admin/configuracion`
   desde T-26; lo único desactualizado era la nota de cuántos destinos seguían pendientes.

### Puntos abiertos (no son decisiones, son deudas)

- ~~**El `ordenId` de la campana es enlace solo para ODONTOLOGO.**~~ — **cerrado el 23/08/2026 por
  T-26**: ahora enlaza para los dos roles, cada uno a su pantalla —`/ordenes/:id` el odontólogo y
  `/admin/ordenes/:id` el laboratorio—. El pendiente venía de T-23.

- **Restricción técnica del JWT: los adjuntos no se pueden abrir con un enlace directo.** La sesión
  viaja en el header `Authorization`, así que un `<a href="/api/v1/archivos/{id}">` sale sin token y
  devuelve **401**. La descarga se pide con `fetch`, se recibe como blob y se entrega desde memoria:
  **`apiFetchArchivo`** en `shared/api/cliente.js` y **`abrirDescarga`** en
  `shared/util/descargaArchivo`. Los dos detalles de orden ya los usan; cualquier pantalla nueva con
  adjuntos tiene que reutilizarlos y no volver a tropezar con el 401.

- ~~**`application-prod.yml` no existe**~~ — **resuelto el 20/08/2026**, a pedido del desarrollador,
  antes de empezar T-30. Apaga Swagger (§1.1), exige SMTP real sin defaults (§6.3, §3.1.b), acota
  CORS y los niveles de log, y repite `ddl-auto: validate`. **No lleva ningún secreto:** todo dato
  sensible llega por variable de entorno y las que no tienen default hacen fallar el arranque a
  propósito. Sigue en `.gitignore` (línea 18), así que la copia de referencia versionada es
  **`application-prod.yml.example`**, y las variables nuevas quedaron documentadas en `.env.example`.
  - ~~**Falta `telegram.bot.username`**~~ — **resuelto por T-32b**: `telegram.bot.token` y
    `telegram.bot.username` están declarados en los tres archivos, vacíos por defecto en `dev` para
    que el canal y la vinculación se deshabiliten solos sin romper el arranque (P-20).
  - **El criterio 2 de §1.1 no está cubierto por ningún test automático** (que `/swagger-ui.html`
    dé 404 con perfil `prod`). Un test así dependería de un archivo que no está versionado y
    fallaría en un clon limpio. Se verifica a mano en el servidor. Si se prefiere automatizarlo,
    hay que sacar `**/application-prod.yml` del `.gitignore` — el archivo no tiene secretos que lo
    impidan; es decisión del desarrollador.
- ~~**D-19 en el frontend: la parte que le toca a T-25.**~~ — **resuelto el 23/08/2026**: el menú del
  odontólogo se montó con los cuatro ítems de §8 y **sin "Nueva orden"**; el alta vive solo en
  `/admin/ordenes/nueva`, con enlace únicamente para las cuentas de administración.

- ~~**`/historial` es hoy una `PantallaPendiente`**~~ — **resuelto el 23/08/2026 por T-27**: la
  ruta es el historial de CU-12, con `?historico=true` y filtro entre entregadas y canceladas. El
  componente `shared/components/PantallaPendiente` sigue en pie para el único ítem de menú cuya
  pantalla llega después: `/admin/odontologos` (T-28).

- ~~**El menú del admin de §8 todavía no se montó.**~~ — **montado el 23/08/2026 por T-26**, con los
  seis ítems. **Dashboard** (`/admin`) lo reemplazó T-27 y **Configuración**
  (`/admin/configuracion`) T-34; queda **un solo** destino en `PantallaPendiente`: **Odontólogos**
  (`/admin/odontologos`, **T-28**).
- **`/admin/odontologos/nuevo` solo es alcanzable desde una solicitud.** No se le agregó enlace en
  el inicio: el botón "Nuevo" pertenece al listado de CU-11, que construye **T-28**. Hasta entonces,
  el alta sin solicitud previa —el "o directamente" de D-17— solo se puede hacer escribiendo la URL.
- ~~**`LicenciaBloqueoFilter` devuelve su mensaje con los acentos rotos.**~~ — **resuelto el
  21/08/2026**, a pedido del desarrollador. El defecto estaba en los **tres** lugares que escriben
  un error fuera del `@RestControllerAdvice`: el filtro de licencia, el `authenticationEntryPoint`
  y el `accessDeniedHandler` de `SecurityConfig`, y el filtro nuevo de T-31. Se centralizó en
  **`shared/excepcion/EscritorErrorHttp`**, que fija `charset=UTF-8` y arma el mismo cuerpo que
  devuelve `ManejadorGlobalExcepciones`, para que el próximo filtro no vuelva a olvidarlo.
  Efecto visible: el `Content-Type` de esas respuestas pasa a ser `application/json;charset=UTF-8`.
- ~~**El correo de credenciales de §3.1.b todavía no existe.**~~ — **resuelto por T-31**: lo compone
  y envía `CredencialesNotificacionListener`, fuera del outbox, como documenta la decisión 1 de (c).
- ~~**`CanalesDeEstructuraTest` va a fallar cuando T-32 integre Telegram.**~~ — **resuelto el
  21/08/2026 por T-32.** El archivo se reescribió y quedó cubriendo solo WhatsApp, que es lo que
  sigue sin integrarse; Telegram pasó a `CanalTelegramTest`. El criterio sigue vigente para lo que
  quedó: el día que exista un proveedor de WhatsApp, ese test tiene que fallar.

- **Dos "Conectar Telegram" seguidos dejan dos tokens vigentes.** §6.5 no dice qué hacer, así que se
  implementó lo literal: cada `POST` emite un token y todos valen hasta usarse o vencer (15 min).
  **Si conviene invalidar los anteriores al emitir uno nuevo, es una decisión a tomar, no un
  arreglo:** no resolver por cuenta propia.

- ~~**El último paso de §6.5 no se probó contra Telegram**~~ — **cerrado el 23/08/2026** por la
  corrida del desarrollador: `erneskrlos` quedó vinculado y recibió por Telegram la notificación
  `ORDEN_URGENTE` de una orden real. Sigue sin probarse en una corrida el **desvincular** (criterio 3
  de §6.5), que está cubierto por tests.

- ~~**`/inicio` de §8 no existe: hoy la ruta es `/`, un inicio genérico para cualquier rol.**~~ —
  **decidido por el desarrollador el 23/08/2026, antes de implementar T-27** (ver decisión 17).

- **El alta de odontólogo vuelve a `/admin` con su confirmación**, que la muestra el dashboard.
  Cambió de `/` a `/admin` en T-27 porque el `location.state` no sobrevive al redirect por rol, pero
  **sigue siendo un destino intermedio**: el alta de orden ya vuelve a su listado (T-26, §8.1
  Regla 1) y **T-28 tiene que hacer lo mismo con `/admin/odontologos`** cuando lo construya,
  mostrando ahí el mensaje que hoy se ve en el dashboard.

- **Punto abierto (T-34): destildar `canal_app_activo` no apaga la campana.** `canal_app_activo`
  solo recorta las filas de `notificacion_envio`; la campana lee `notificacion` directamente
  (decisión 5 de T-21 y decisión 5 de T-22). El administrador que apague ese canal seguirá viendo
  sus avisos en la campana. Resolverlo implica cambiar el filtrado de la campana, alcance de
  T-21/T-22. **Se eleva a la clienta junto con la deuda de T-32: ambas son la misma pregunta sobre
  qué significa "canal activo".**
  > **No es del mismo tipo que la deuda de T-32**, aunque se eleven juntas. En aquella el admin
  > **configura algo que no surte efecto**; en esta el admin **apaga un canal y el canal sigue
  > funcionando**: la interfaz ofrece un control que no controla lo que dice controlar. Eso es un
  > **defecto de comportamiento**, no solo una incoherencia de documentación.
  >
  > Verificado en el código al implementar T-34, no deducido: las cinco consultas de
  > `NotificacionRepository` son sobre `Notificacion` y **ninguna** toca `NotificacionEnvio`.

- **Deuda de spec (T-32):** §6.4 valida CU-21 contra `configuracion_notificacion.telegram_chat_id`
  (chat del laboratorio), mientras que el envío real usa `usuario.telegram_chat_id` (D-21, §6.3).
  Un ADMIN puede activar el canal cargando un chat en la configuración y aun así recibir todos sus
  envíos `FALLIDO` si no se vinculó por el bot. Si se decide que activar Telegram exija
  `usuario.telegram_vinculado = true`, es un cambio de §6.4 que requiere confirmación de la clienta.
  **No resolver por cuenta propia.**
- **Un odontólogo no puede apagar ningún canal.** §6.4 reserva `/configuracion-notificaciones` a
  ADMIN y SUPERADMIN, pero D-20 le manda notificaciones por tres canales. Se implementó como dice la
  spec. Si la clienta quiere que el odontólogo elija, es un cambio de spec, no un error.

---

## (d) Orden de ejecución vigente

```
T-29 ✅ → T-18 ✅ → T-19 ✅ → T-20 ✅ → T-33a ✅ → T-21 ✅ → T-22 ✅ → T-23 ✅
     → T-30 ✅ → T-31 ✅ → T-32 ✅ → T-32b ✅ → T-33b ✅
     → T-25 ✅ → T-26 ✅ → T-27 ✅ → T-34 ✅ → [T-28]
```

Es el orden de `Plan.md`, que **no** sigue la numeración de los bloques. `T-24` fue eliminada
por D-19. `T-33` quedó partida: **T-33a** (backend) ya se hizo adelantada, **T-33b** (pantalla
y notificación) conserva su lugar después de T-32b porque el selector de odontólogo necesita
las cuentas de T-31.

**`T-34` se creó el 23/08/2026**: la pantalla de CU-21 (`/admin/configuracion`) estaba huérfana
—T-22 hizo sus endpoints y T-23 solo la campana— y ninguna tarea la construía. Va después de T-27
y antes de T-28, para que la tarea de cierre no estrene funcionalidad que tendría que verificar en
la misma pasada.

**Protocolo, sin excepciones:** una tarea, un reporte con el formato de `Agente.md` §4.1, y
esperar confirmación. Nunca encadenar dos tareas.
