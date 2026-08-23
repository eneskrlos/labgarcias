import { Navigate } from 'react-router-dom';
import { useSesion } from '../hooks/useSesion';

/**
 * RN-14: oculta rutas del cliente a usuarios sin sesión o sin el rol requerido.
 *
 * §3.1.b: mientras haya un cambio de contraseña pendiente, toda ruta lleva a `/cambiar-password`.
 * Es la mitad visible de la regla; la que manda es el backend, que con ese token no responde
 * ningún otro endpoint. `permitidaConCambioPendiente` la usa la propia pantalla de cambio, que
 * si no se redirigiría a sí misma.
 */
export function RutaProtegida({ rolesPermitidos, permitidaConCambioPendiente = false, children }) {
  const { usuario } = useSesion();

  if (!usuario) {
    return <Navigate to="/login" replace />;
  }
  if (usuario.debeCambiarPassword && !permitidaConCambioPendiente) {
    return <Navigate to="/cambiar-password" replace />;
  }
  if (rolesPermitidos && !rolesPermitidos.includes(usuario.rol)) {
    return <Navigate to="/login" replace />;
  }
  return children;
}
