import { NavLink } from 'react-router-dom';
import estilos from './MenuOdontologo.module.css';

/**
 * §8, menú del odontólogo: **Inicio · Mis trabajos · Historial · Perfil**, exactamente esos cuatro.
 *
 * - **Sin "Nueva orden"**: la retiró D-19 —la orden la registra el laboratorio— y el flujo del
 *   CU-09 original queda documentado por P-19, no eliminado del análisis.
 * - **Sin "Mensajes"**: D-11 pospuso la mensajería.
 *
 * `Inicio` apunta a `/`, que hoy es un inicio genérico; el panel del odontólogo (CU-02) e
 * `Historial` (CU-12) son pantallas de T-27, y el ítem las espera.
 */
export default function MenuOdontologo() {
  const clase = ({ isActive }) => (isActive ? `${estilos.enlace} ${estilos.activo}` : estilos.enlace);

  return (
    <>
      <NavLink to="/" end className={clase}>
        Inicio
      </NavLink>
      <NavLink to="/ordenes" className={clase}>
        Mis trabajos
      </NavLink>
      <NavLink to="/historial" className={clase}>
        Historial
      </NavLink>
      <NavLink to="/perfil" className={clase}>
        Perfil
      </NavLink>
    </>
  );
}
