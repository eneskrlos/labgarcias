import { NavLink } from 'react-router-dom';
import { useSesion } from '../../shared/hooks/useSesion';
import estilos from './Menu.module.css';

const ROL_SUPERADMIN = 'SUPERADMIN';

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
 *
 * **Séptimo ítem, solo para SUPERADMIN: Licencias** (CU-23, T-35). §8 le da su propia fila con ese
 * rol, así que es una pantalla de navegación y no una ruta suelta. Sin el ítem, el SuperAdmin solo
 * llegaría a sus licencias desde `/bloqueado` —es decir, **cuando el sistema ya está caído**— y no
 * podría renovar antes del vencimiento, que es justo lo que evita el corte. Es el mismo menú con un
 * ítem condicionado, no un menú aparte: §3.5 ya define al SUPERADMIN como el ADMIN más usuarios y
 * licencias.
 */
export default function MenuAdmin() {
  const { usuario } = useSesion();
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
      {usuario?.rol === ROL_SUPERADMIN && (
        <NavLink to="/admin/licencias" className={clase}>
          Licencias
        </NavLink>
      )}
    </>
  );
}
