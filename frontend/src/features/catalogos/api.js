import { apiFetch } from '../../shared/api/cliente';

export function listarTodos() {
  return apiFetch('/tipos-trabajo/todos');
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
