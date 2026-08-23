import { apiFetch, apiFetchArchivo } from '../../shared/api/cliente';

/**
 * CU-03/§5.3: las órdenes **del usuario autenticado**. El backend filtra por el token: acá no
 * hay ni puede haber un id de odontólogo (RN-01). `estado` es opcional y también lo resuelve el
 * backend; nunca se filtra sobre una colección ya traída (Agente.md 6.2).
 */
export function listarMisOrdenes({ pagina, tamano, estado }) {
  const parametros = new URLSearchParams();
  parametros.set('page', String(pagina));
  parametros.set('size', String(tamano));
  if (estado) {
    parametros.set('estado', estado);
  }
  return apiFetch(`/ordenes?${parametros.toString()}`);
}

/** CU-04/§5.4: detalle con línea de tiempo y adjuntos. Una orden ajena responde 404, no 403. */
export function obtenerOrden(id) {
  return apiFetch(`/ordenes/${id}`);
}

/** CU-20/§5.6: cancelación por el propietario. Sin cargo (P-14) y sin vuelta atrás. */
export function cancelarOrden(id) {
  return apiFetch(`/ordenes/${id}/cancelar`, { method: 'PATCH' });
}

/** RN-13: el binario del adjunto. Va por `fetch` porque la ruta exige el token (ver cliente). */
export function descargarArchivo(id) {
  return apiFetchArchivo(`/archivos/${id}`);
}

/**
 * CU-09/§5.1: alta de una orden. **La registra el laboratorio** (D-19), a nombre del odontólogo
 * que elige el administrador.
 *
 * El cliente manda solo lo que la persona escribe: código, iniciales, precios, estado inicial y
 * fecha estimada los deriva el backend (Agente.md 6.1: ningún cálculo de negocio en el frontend).
 */
export function crearOrden(datos) {
  return apiFetch('/ordenes', { method: 'POST', body: JSON.stringify(datos) });
}
