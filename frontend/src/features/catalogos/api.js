import { apiFetch } from '../../shared/api/cliente';

/** CU-16/CU-09: catálogo completo sin paginar, para el selector de nueva orden. */
export function listarActivos() {
  return apiFetch('/tipos-trabajo/activos');
}

/** CU-16: listado paginado de administración, con filtros opcionales. */
export function listarPaginado({ pagina, tamano, activo, busqueda }) {
  const parametros = new URLSearchParams();
  parametros.set('page', String(pagina));
  parametros.set('size', String(tamano));
  if (activo !== undefined && activo !== null) {
    parametros.set('activo', String(activo));
  }
  if (busqueda) {
    parametros.set('busqueda', busqueda);
  }
  return apiFetch(`/tipos-trabajo?${parametros.toString()}`);
}

/** Alimenta la precarga del formulario de edición (spec.md §8.1). */
export function obtenerPorId(id) {
  return apiFetch(`/tipos-trabajo/${id}`);
}

export function crear(datos) {
  return apiFetch('/tipos-trabajo', { method: 'POST', body: JSON.stringify(datos) });
}

export function actualizar(id, datos) {
  return apiFetch(`/tipos-trabajo/${id}`, { method: 'PUT', body: JSON.stringify(datos) });
}

export function cambiarEstado(id, activo) {
  return apiFetch(`/tipos-trabajo/${id}/estado`, { method: 'PATCH', body: JSON.stringify({ activo }) });
}
