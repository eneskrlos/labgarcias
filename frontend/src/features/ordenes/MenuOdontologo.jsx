import { NavLink } from 'react-router-dom';
import { Icono } from '../../shared/components/Icono';
import estilos from './Menu.module.css';

/**
 * §8, menú del odontólogo: **Inicio · Mis trabajos · Historial · Perfil**, exactamente esos cuatro.
 *
 * - **Sin "Nueva orden"**: la retiró D-19 —la orden la registra el laboratorio— y el flujo del
 *   CU-09 original queda documentado por P-19, no eliminado del análisis.
 * - **Sin "Mensajes"**: D-11 pospuso la mensajería.
 *
 * `Inicio` apunta a `/inicio`, que es la ruta que §8 le asigna al panel del odontólogo (CU-02).
 * Apuntaba a `/` mientras esa pantalla no existía; desde T-27, `/` solo redirige según el rol.
 */
export default function MenuOdontologo() {
  const clase = ({ isActive }) => (isActive ? `${estilos.enlace} ${estilos.activo}` : estilos.enlace);

  return (
    <>
      <NavLink to="/inicio" className={clase}>
        <Icono nombre="inicio" />
        Inicio
      </NavLink>
      <NavLink to="/ordenes" className={clase}>
        <Icono nombre="trabajos" />
        Mis trabajos
      </NavLink>
      <NavLink to="/historial" className={clase}>
        <Icono nombre="historial" />
        Historial
      </NavLink>
      <NavLink to="/perfil" className={clase}>
        <Icono nombre="perfil" />
        Perfil
      </NavLink>
    </>
  );
}
