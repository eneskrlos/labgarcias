import { apiFetch } from '../../shared/api/cliente';

/**
 * CU-23/§3.6: histórico de períodos de licencia, **paginado en el backend** (§8.1 Regla 2).
 * Nunca se trae la colección entera para cortarla acá (`Agente.md` §6.2).
 */
export function listarLicencias({ pagina, tamano }) {
  const parametros = new URLSearchParams();
  parametros.set('page', String(pagina));
  parametros.set('size', String(tamano));
  return apiFetch(`/licencias?${parametros.toString()}`);
}

/** CU-23: estado actual. Devuelve `{ vigente, licencia }`, con `licencia` nula si no hay ninguna. */
export function obtenerLicenciaVigente() {
  return apiFetch('/licencias/vigente');
}

/**
 * CU-23/RN-20: registra un período. **D-16 y P-11/P-12**: no hay planes ni precios, solo el rango
 * de fechas y una observación libre. El estado lo pone el backend.
 */
export function registrarLicencia(datos) {
  return apiFetch('/licencias', { method: 'POST', body: JSON.stringify(datos) });
}
