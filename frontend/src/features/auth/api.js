import { apiFetch } from '../../shared/api/cliente';

/**
 * CR-01: se retiraron `registrar` (D-17: el alta la hace el admin), `verificar` y
 * `reenviarVerificacion` (D-18: las cuentas nacen activas) y `loginGoogle` (D-17).
 * La solicitud de acceso pública llega en T-30.
 */

export function login(datos) {
  return apiFetch('/auth/login', { method: 'POST', body: JSON.stringify(datos) });
}

export function logout() {
  return apiFetch('/auth/logout', { method: 'POST' });
}
