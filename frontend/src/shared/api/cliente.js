import { obtenerToken } from './token';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1';

export class ApiError extends Error {
  constructor(status, codigo, mensaje, campo = null) {
    super(mensaje);
    this.status = status;
    this.codigo = codigo;
    this.mensaje = mensaje;
    this.campo = campo;
  }
}

/**
 * Descarga binaria (RN-13/CU-04). Va aparte de `apiFetch` porque el cuerpo no es JSON.
 *
 * **Restricción técnica del JWT:** la sesión viaja en el header `Authorization`, así que un
 * `<a href>` directo al endpoint saldría sin token y devolvería 401. El archivo se pide por
 * `fetch`, se recibe como blob y se abre desde memoria.
 */
export async function apiFetchArchivo(path) {
  const respuesta = await fetch(`${BASE_URL}${path}`, { headers: cabecerasAutenticadas() });

  if (!respuesta.ok) {
    const cuerpo = await respuesta.json().catch(() => null);
    throw new ApiError(
      respuesta.status,
      cuerpo?.codigo ?? 'ERROR_DESCONOCIDO',
      cuerpo?.mensaje ?? 'No se pudo descargar el archivo.',
      cuerpo?.campo ?? null,
    );
  }

  return respuesta.blob();
}

function cabecerasAutenticadas() {
  const headers = new Headers();
  const token = obtenerToken();
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }
  return headers;
}

export async function apiFetch(path, opciones = {}) {
  const headers = new Headers(opciones.headers);
  headers.set('Content-Type', 'application/json');

  const token = obtenerToken();
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const respuesta = await fetch(`${BASE_URL}${path}`, { ...opciones, headers });

  // RN-20: licencia vencida bloquea el sistema; el interceptor redirige a /bloqueado.
  if (respuesta.status === 423) {
    window.location.assign('/bloqueado');
    throw new ApiError(423, 'LICENCIA_VENCIDA', 'Licencia vencida.');
  }

  if (!respuesta.ok) {
    const cuerpo = await respuesta.json().catch(() => null);
    throw new ApiError(
      respuesta.status,
      cuerpo?.codigo ?? 'ERROR_DESCONOCIDO',
      cuerpo?.mensaje ?? 'Ocurrió un error inesperado.',
      cuerpo?.campo ?? null,
    );
  }

  if (respuesta.status === 204) {
    return null;
  }

  return respuesta.json();
}
