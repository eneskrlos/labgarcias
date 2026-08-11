import { Navigate } from 'react-router-dom';
import { useSesion } from '../hooks/useSesion';

/** RN-14: oculta rutas del cliente a usuarios sin sesión o sin el rol requerido. */
export function RutaProtegida({ rolesPermitidos, children }) {
  const { usuario } = useSesion();

  if (!usuario) {
    return <Navigate to="/login" replace />;
  }
  if (rolesPermitidos && !rolesPermitidos.includes(usuario.rol)) {
    return <Navigate to="/login" replace />;
  }
  return children;
}
