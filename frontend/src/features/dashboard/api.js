import { apiFetch } from '../../shared/api/cliente';

/**
 * CU-02/§5.7: el panel del odontólogo autenticado.
 *
 * RN-01: el backend filtra por el token y el endpoint no acepta un id de odontólogo, así que acá
 * no hay ni puede haber un parámetro. §8: los contadores vienen calculados; el cliente no deriva
 * ninguno.
 */
export function obtenerPanelOdontologo() {
  return apiFetch('/dashboard');
}

/**
 * CU-10/§5.7: el dashboard del laboratorio, con sus contadores, la distribución por estado, las
 * próximas a entregar, las recientes y las urgentes. Todo llega resuelto del backend.
 */
export function obtenerDashboardAdmin() {
  return apiFetch('/admin/dashboard');
}
