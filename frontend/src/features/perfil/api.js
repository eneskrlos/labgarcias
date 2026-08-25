import { apiFetch } from '../../shared/api/cliente';

/** §7: los datos propios del usuario autenticado. */
export function obtenerPerfil() {
  return apiFetch('/perfil');
}

/**
 * §7: edita **solo el nombre y la dirección** del usuario autenticado.
 *
 * `rol` y `correo` **no se mandan y no se pueden mandar**: el request del backend no los tiene, así
 * que agregarlos acá no tendría efecto. El id sale del token; esta función no lo acepta.
 */
export function actualizarPerfil({ nombreCompleto, direccion }) {
  return apiFetch('/perfil', {
    method: 'PUT',
    body: JSON.stringify({ nombreCompleto, direccion }),
  });
}

/**
 * §6.5: pide el enlace profundo al bot. Un bot de Telegram no puede escribir primero, así que
 * la vinculación arranca siempre con el usuario abriendo este enlace.
 */
export function conectarTelegram() {
  return apiFetch('/telegram/vinculacion', { method: 'POST' });
}

/** §6.5 criterio 3: corta los envíos por Telegram sin afectar el correo ni la campana. */
export function desvincularTelegram() {
  return apiFetch('/telegram/vinculacion', { method: 'DELETE' });
}
