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

/**
 * RN-19/CU-21, §6.4: la configuración de canales del administrador autenticado.
 *
 * La ruta no lleva id: el destinatario sale del token, igual que en la campana. Quien nunca
 * guardó nada recibe los canales por defecto de §6.3 con `fechaActualizacion` nula, no un 404.
 */
export function obtenerConfiguracionNotificaciones() {
  return apiFetch('/configuracion-notificaciones');
}

/**
 * CU-21: **el PUT reemplaza la configuración entera**, así que las tres banderas viajan siempre,
 * también las apagadas — un campo ausente sería ambiguo entre "apagalo" y "no lo toques".
 *
 * `canalWhatsappActivo` **no se manda**: P-18 lo deja como estructura y el request del backend no
 * lo acepta. Se lee del GET para mostrarlo, nada más.
 */
export function guardarConfiguracionNotificaciones(datos) {
  return apiFetch('/configuracion-notificaciones', { method: 'PUT', body: JSON.stringify(datos) });
}
