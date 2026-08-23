import { apiFetch } from '../../shared/api/cliente';

/** §7: los datos propios del usuario autenticado. Editar nombre y dirección es de T-28. */
export function obtenerPerfil() {
  return apiFetch('/perfil');
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
