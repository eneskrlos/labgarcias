import { apiFetch } from '../../shared/api/cliente';

/**
 * CR-01: se retiraron `registrar` (D-17: el alta la hace el admin), `verificar` y
 * `reenviarVerificacion` (D-18: las cuentas nacen activas) y `loginGoogle` (D-17).
 * Lo único público que quedó es la solicitud de acceso (D-17, spec.md §3.1).
 */

export function login(datos) {
  return apiFetch('/auth/login', { method: 'POST', body: JSON.stringify(datos) });
}

export function logout() {
  return apiFetch('/auth/logout', { method: 'POST' });
}

/** D-17/§3.1: formulario público. No crea cuenta: registra el pedido y avisa al administrador. */
export function solicitarAcceso(datos) {
  return apiFetch('/auth/solicitud-acceso', { method: 'POST', body: JSON.stringify(datos) });
}

/** §3.1.b: listado del administrador. `estado` es opcional: sin él vienen todas. */
export function listarSolicitudes({ pagina, tamano, estado }) {
  const parametros = new URLSearchParams();
  parametros.set('page', String(pagina));
  parametros.set('size', String(tamano));
  if (estado) {
    parametros.set('estado', estado);
  }
  return apiFetch(`/solicitudes-acceso?${parametros.toString()}`);
}

/** §3.1.b: descarta una solicitud pendiente. Aprobar es dar de alta la cuenta (§3.1.b), no está acá. */
export function rechazarSolicitud(id) {
  return apiFetch(`/solicitudes-acceso/${id}/rechazar`, { method: 'PATCH' });
}

/**
 * D-18/§3.1.b: alta de la cuenta. La contraseña la genera el backend y la manda por correo:
 * el formulario no la pide ni la recibe. Con `solicitudId`, esa solicitud queda APROBADA.
 */
export function crearOdontologo(datos) {
  return apiFetch('/odontologos', { method: 'POST', body: JSON.stringify(datos) });
}

/**
 * §5.1/D-19: alimenta el selector de odontólogo al registrar una orden. Sin paginar y con solo
 * id y nombre; el listado administrable de CU-11 es otro endpoint y es de T-28.
 */
/**
 * CU-11/§7: la tabla administrable de odontólogos, **paginada en el backend** (§8.1 Regla 2).
 *
 * Es otra cosa que `listarOdontologosActivos`, y conviven: aquella alimenta el selector de §5.1 y
 * trae solo las cuentas ACTIVA; esta trae **también las dadas de baja**, que es donde se ven.
 */
export function listarOdontologos({ pagina, tamano }) {
  const parametros = new URLSearchParams();
  parametros.set('page', String(pagina));
  parametros.set('size', String(tamano));
  return apiFetch(`/odontologos?${parametros.toString()}`);
}

/** CU-17/§7: el padrón completo, de cualquier rol. Solo SUPERADMIN. */
export function listarUsuarios({ pagina, tamano }) {
  const parametros = new URLSearchParams();
  parametros.set('page', String(pagina));
  parametros.set('size', String(tamano));
  return apiFetch(`/usuarios?${parametros.toString()}`);
}

/** CU-17: alta o baja de una cuenta. El backend rechaza que el SuperAdmin se toque a sí mismo. */
export function cambiarEstadoUsuario(id, estadoCuenta) {
  return apiFetch(`/usuarios/${id}/estado`, { method: 'PATCH', body: JSON.stringify({ estadoCuenta }) });
}

export function listarOdontologosActivos() {
  return apiFetch('/odontologos/activos');
}

/** §3.1.b: cambio obligatorio del primer ingreso. Devuelve un token nuevo, ya sin restricción. */
export function cambiarPassword(datos) {
  return apiFetch('/auth/cambiar-password', { method: 'POST', body: JSON.stringify(datos) });
}
