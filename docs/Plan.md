# Plan.md — Plan de ejecución

**Proyecto:** Lab. Garcia's Connect
**Versión:** 1.0 — 08/08/2026

Orden de implementación. Cada tarea se ejecuta **de forma individual**, se reporta según `Agente.md` 4.1 y **espera confirmación** antes de continuar.

**28 tareas en 7 bloques.**

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

### T-21 · Núcleo de notificaciones y canal de correo
**Objetivo:** eventos, outbox y envío por correo.
**Depende de:** T-20
**Spec:** §6.1, §6.2, §6.3
**Reglas:** RN-05, RN-11, RN-19, CU-07
**Patrones esperados:** Observer, Strategy, Adapter, Transactional Outbox.
**Terminado cuando:** se cumplen los criterios 1 y 2 de §6.
> `CanalTelegram`: **solo la estructura**, marca `FALLIDO` con "canal no configurado". No integrar.
> No inventar eventos: la cancelación **no** notifica (S-08).

### T-22 · Endpoints de notificaciones y configuración de canales
**Objetivo:** campana, contador y CU-21.
**Depende de:** T-21
**Spec:** §6.4
**Reglas:** RN-19, CU-21
**Terminado cuando:** se cumplen los criterios 3 y 4 de §6.

### T-23 · Campana en el frontend
**Objetivo:** contador y listado de notificaciones.
**Depende de:** T-11, T-22
**Spec:** §6.4, §8
**Terminado cuando:** el contador se actualiza por polling cada 60 s y se pueden marcar como leídas.
> **No implementar WebSocket ni SSE.**

---

## Bloque 5 — Pantallas de órdenes

### T-24 · Nueva orden (frontend)
**Objetivo:** front de CU-09.
**Depende de:** T-15, T-18
**Spec:** §5.1, §5.2, §8
**Terminado cuando:**
- Formulario con paciente, fecha de ingreso, selector de tipo de trabajo, tipo de orden y descripción.
- Carga de imágenes con validación de formato y tamaño (la del backend es la que manda).
- Muestra precio y fecha estimada **devueltos por el backend**, sin recalcular.

### T-25 · Mis órdenes y seguimiento (frontend)
**Objetivo:** front de CU-03, CU-04 y CU-20.
**Depende de:** T-19, T-24
**Spec:** §5.3, §5.4, §5.6, §8
**Terminado cuando:**
- Listado con identificación por iniciales + código, **nunca** el nombre completo.
- Detalle con línea de tiempo fechada, datos, archivos y botón de cancelación.
- **Sin sección de mensajes** (D-11).

### T-26 · Gestión de órdenes (admin)
**Objetivo:** front de CU-06 y CU-10.
**Depende de:** T-20, T-25
**Spec:** §5.5, §5.7, §8
**Terminado cuando:** el admin lista y filtra órdenes, ve el detalle y avanza el estado con un botón que solo ofrece la transición siguiente válida.

---

## Bloque 6 — Paneles y cierre

### T-27 · Dashboards
**Objetivo:** CU-02, CU-10 y CU-12.
**Depende de:** T-26
**Spec:** §5.7, §8
**Reglas:** CU-02, CU-10, CU-12
**Terminado cuando:**
- Panel del odontólogo con sus contadores y órdenes recientes.
- Dashboard admin con contadores, distribución por estado, próximas a entregar y urgentes.
- Historial del odontólogo.
> **Sin reportes ni estadísticas más allá de estos contadores** (CU-13 es Fase 4).

### T-28 · Perfil, odontólogos y repaso final
**Objetivo:** CU-11, CU-17 y verificación integral.
**Depende de:** T-27
**Spec:** §7, §9
**Terminado cuando:**
- Perfil editable (nombre y dirección; **no** rol ni correo).
- Listado de odontólogos para el admin; gestión de usuarios para el SuperAdmin.
- Repaso de la tabla de trazabilidad de `spec.md` §9: cada regla tiene su implementación verificable.
- Repaso de que ningún punto fuera de alcance (`Agente.md` 3.3) quedó implementado.

---

## Resumen

| Bloque | Tareas | Entrega |
|---|---|---|
| 0 · Cimientos | T-01 a T-04 | Proyectos arrancando con base de datos migrada |
| 1 · Seguridad | T-05 a T-11 | Registro, verificación, login, Google, licencia |
| 2 · Catálogos | T-12 a T-15 | Tipos de trabajo y estados administrables |
| 3 · Órdenes | T-16 a T-20 | Ciclo completo: creación, adjuntos, seguimiento, estados |
| 4 · Notificaciones | T-21 a T-23 | Outbox, correo, campana |
| 5 · Pantallas de órdenes | T-24 a T-26 | Front de odontólogo y administración |
| 6 · Paneles y cierre | T-27 a T-28 | Dashboards y verificación integral |

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
