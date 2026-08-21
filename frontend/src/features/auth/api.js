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
