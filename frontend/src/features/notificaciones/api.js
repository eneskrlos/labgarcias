import { apiFetch } from '../../shared/api/cliente';

/**
 * spec.md §6.4: página de notificaciones propias. El destinatario sale del token, nunca de la
 * URL, así que este cliente no tiene forma de pedir las de otro usuario.
 */
export function listarPaginado({ pagina, tamano }) {
  const parametros = new URLSearchParams();
  parametros.set('page', String(pagina));
  parametros.set('size', String(tamano));
  return apiFetch(`/notificaciones?${parametros.toString()}`);
}

/** spec.md §6.4: lo que consulta la campana por polling. Devuelve `{ noLeidas }`. */
export function contarNoLeidas() {
  return apiFetch('/notificaciones/contador');
}

export function marcarLeida(id) {
  return apiFetch(`/notificaciones/${id}/leer`, { method: 'PATCH' });
}

/** spec.md §6.4: devuelve el contador ya actualizado; no hace falta volver a pedirlo. */
export function marcarTodasLeidas() {
  return apiFetch('/notificaciones/leer-todas', { method: 'PATCH' });
}
