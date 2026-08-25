import { Link } from 'react-router-dom';
import { useSesion } from '../../shared/hooks/useSesion';
import estilos from './Auth.module.css';

const ROL_SUPERADMIN = 'SUPERADMIN';

/**
 * RN-20: se muestra cuando el cliente HTTP intercepta un 423 (licencia vencida).
 *
 * **§3.6: al SUPERADMIN le ofrece la salida.** Con la licencia vencida el frontend manda todo acá,
 * así que si el acceso a `/admin/licencias` dependiera del menú del laboratorio, la única persona
 * que puede regularizar quedaría encerrada en esta pantalla. El filtro del backend exime
 * `/licencias/**` justamente para que ese camino funcione.
 *
 * Al resto de los roles no se les ofrece: el endpoint les responde 403 y el enlace sería una
 * promesa falsa.
 */
export default function Bloqueado() {
  const { usuario } = useSesion();
  const esSuperAdmin = usuario?.rol === ROL_SUPERADMIN;

  return (
    <div className={estilos.pantalla}>
      <div className={estilos.tarjeta}>
        <h1>Sistema bloqueado</h1>
        <p>
          La licencia de este laboratorio está vencida. Contactá al SuperAdmin para regularizar la
          situación; mientras tanto, el sistema no puede operar.
        </p>

        {esSuperAdmin && (
          <p>
            <Link to="/admin/licencias">Registrar un período de licencia</Link>
          </p>
        )}
      </div>
    </div>
  );
}
