import { NavLink } from 'react-router-dom';
import estilos from './Menu.module.css';

/**
 * §8, menú del laboratorio: **Dashboard · Trabajos · Odontólogos · Solicitudes · Tipos de trabajo
 * · Configuración**, exactamente esos seis.
 *
 * **No incluye** Pacientes (S-03 sin resolver), Calendario, Mensajes (D-11), Reportes ni
 * Facturación (P-08).
 *
 * El menú se monta completo desde T-26 aunque sus destinos lleguen después, porque es lo que fija
 * §8, y cada tarea reemplaza su ruta: T-27 puso el Dashboard (CU-10) y T-34 la Configuración
 * (CU-21). **Queda uno solo en `PantallaPendiente`**: Odontólogos (CU-11, T-28).
 */
export default function MenuAdmin() {
  const clase = ({ isActive }) => (isActive ? `${estilos.enlace} ${estilos.activo}` : estilos.enlace);

  return (
    <>
      <NavLink to="/admin" end className={clase}>
        Dashboard
      </NavLink>
      <NavLink to="/admin/ordenes" className={clase}>
        Trabajos
      </NavLink>
      <NavLink to="/admin/odontologos" className={clase}>
        Odontólogos
      </NavLink>
      <NavLink to="/admin/solicitudes" className={clase}>
        Solicitudes
      </NavLink>
      <NavLink to="/admin/tipos-trabajo" className={clase}>
        Tipos de trabajo
      </NavLink>
      <NavLink to="/admin/configuracion" className={clase}>
        Configuración
      </NavLink>
    </>
  );
}
