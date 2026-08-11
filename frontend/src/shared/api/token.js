const CLAVE_TOKEN = 'labgarcias_token';
const CLAVE_USUARIO = 'labgarcias_usuario';

export function obtenerToken() {
  return localStorage.getItem(CLAVE_TOKEN);
}

export function guardarToken(token) {
  localStorage.setItem(CLAVE_TOKEN, token);
}

export function borrarToken() {
  localStorage.removeItem(CLAVE_TOKEN);
}

export function obtenerUsuario() {
  const guardado = localStorage.getItem(CLAVE_USUARIO);
  return guardado ? JSON.parse(guardado) : null;
}

export function guardarUsuario(usuario) {
  localStorage.setItem(CLAVE_USUARIO, JSON.stringify(usuario));
}

export function borrarUsuario() {
  localStorage.removeItem(CLAVE_USUARIO);
}

/** CU-01: persiste la sesión completa (token + datos mínimos del usuario) tras el login. */
export function guardarSesion(token, usuario) {
  guardarToken(token);
  guardarUsuario(usuario);
}

export function borrarSesion() {
  borrarToken();
  borrarUsuario();
}
