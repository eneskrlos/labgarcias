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
 * El menú se montó completo desde T-26 aunque sus destinos llegaran después, porque es lo que fija
 * §8, y cada tarea reemplazó su ruta: T-27 el Dashboard (CU-10), T-34 la Configuración (CU-21) y
 * T-28 los Odontólogos (CU-11). **Ya no queda ningún destino pendiente.**
 *
 * **Dos ítems más, solo para SUPERADMIN: Usuarios y Licencias** (CU-17 y CU-23). Es el mismo menú
 * con ítems condicionados, no un menú aparte: **§3.5 define al SUPERADMIN como el ADMIN más
 * usuarios y licencias**, que es exactamente lo que se ve acá.
 *
 * - **Licencias** (T-35): sin el ítem, el SuperAdmin solo llegaría desde `/bloqueado` —cuando el
 *   sistema ya está caído— y no podría renovar antes del vencimiento.
 * - **Usuarios** (T-28): es la única pantalla que reactiva una cuenta dada de baja. Sin el ítem,
 *   el SuperAdmin vería cuentas inactivas en `/admin/odontologos` sin forma de resolverlo.
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
        <>
          <NavLink to="/admin/usuarios" className={clase}>
            Usuarios
          </NavLink>
          <NavLink to="/admin/licencias" className={clase}>
            Licencias
          </NavLink>
        </>
      )}
    </>
  );
}
