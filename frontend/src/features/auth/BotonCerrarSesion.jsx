import { useNavigate } from 'react-router-dom';
import { useSesion } from '../../shared/hooks/useSesion';
import { logout } from './api';
import estilos from './BotonCerrarSesion.module.css';

/**
 * CU-14: cerrar sesión.
 *
 * Vivía en la pantalla `Inicio` de `/`, que T-27 retiró al convertir esa ruta en un redirect por
 * rol. Sube al encabezado compartido por el mismo motivo que la campana (T-23): montado en una
 * sola pantalla queda invisible en el resto, y ahora ninguna pantalla es común a los dos roles.
 */
export default function BotonCerrarSesion() {
  const { cerrarSesion } = useSesion();
  const navigate = useNavigate();

  const salir = async () => {
    try {
      await logout();
    } catch {
      // CU-14: el cierre de sesión es stateless; si la llamada falla, igual se limpia localmente.
    }
    cerrarSesion();
    navigate('/login');
  };

  return (
    <button type="button" className={estilos.boton} onClick={salir}>
      Cerrar sesión
    </button>
  );
}
