import { apiFetch } from '../../shared/api/cliente';

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
