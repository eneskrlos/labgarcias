import { apiFetch } from '../../shared/api/cliente';

export function registrar(datos) {
  return apiFetch('/auth/registro', { method: 'POST', body: JSON.stringify(datos) });
}

export function login(datos) {
  return apiFetch('/auth/login', { method: 'POST', body: JSON.stringify(datos) });
}

export function loginGoogle(idToken) {
  return apiFetch('/auth/google', { method: 'POST', body: JSON.stringify({ idToken }) });
}

export function verificar(token) {
  return apiFetch(`/auth/verificar?token=${encodeURIComponent(token)}`);
}

export function reenviarVerificacion(correo) {
  return apiFetch('/auth/reenviar-verificacion', { method: 'POST', body: JSON.stringify({ correo }) });
}

export function logout() {
  return apiFetch('/auth/logout', { method: 'POST' });
}
