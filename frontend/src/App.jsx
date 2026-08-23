import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Link, useNavigate } from 'react-router-dom';
import { SesionProvider, useSesion } from './shared/hooks/useSesion';
import { RutaProtegida } from './shared/components/RutaProtegida';
import { LayoutAutenticado } from './shared/components/LayoutAutenticado';
import { logout } from './features/auth/api';
import Login from './features/auth/Login';
import Bloqueado from './features/auth/Bloqueado';
import SolicitarAcceso from './features/auth/SolicitarAcceso';
import SolicitudesListado from './features/auth/SolicitudesListado';
import CambiarPassword from './features/auth/CambiarPassword';
import OdontologoFormulario from './features/auth/OdontologoFormulario';
import Campana from './features/notificaciones/Campana';
import Perfil from './features/perfil/Perfil';
import TiposTrabajoListado from './features/catalogos/TiposTrabajoListado';
import TipoTrabajoFormulario from './features/catalogos/TipoTrabajoFormulario';

const queryClient = new QueryClient();
const ROLES_ADMIN = ['ADMIN', 'SUPERADMIN'];

/**
 * Toda ruta con sesión iniciada comparte el encabezado, y la campana vive ahí (§6.4): montarla
 * en una sola pantalla la dejaría invisible en el resto.
 */
function PantallaAutenticada({ rolesPermitidos, children }) {
  return (
    <RutaProtegida rolesPermitidos={rolesPermitidos}>
      <LayoutAutenticado acciones={<Campana />}>{children}</LayoutAutenticado>
    </RutaProtegida>
  );
}

function Inicio() {
  const { usuario, cerrarSesion } = useSesion();
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
    <div className="contenedor">
      <h1>Inicio</h1>
      <p>
        Hola, {usuario.nombreCompleto} ({usuario.rol})
      </p>
      {/* §6.5: la vinculación de Telegram vive en el perfil, y es de cualquier usuario. */}
      <p>
        <Link to="/perfil">Mi perfil</Link>
      </p>
      {ROLES_ADMIN.includes(usuario.rol) && (
        <p>
          <Link to="/admin/solicitudes">Solicitudes de acceso</Link> ·{' '}
          <Link to="/admin/tipos-trabajo">Tipos de trabajo</Link>
        </p>
      )}
      <button type="button" onClick={salir}>Cerrar sesión</button>
    </div>
  );
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <SesionProvider>
        <BrowserRouter>
          <Routes>
            {/* CR-01: /registro y /verificar retiradas (D-17/D-18); las reemplaza /solicitar-acceso. */}
            <Route path="/login" element={<Login />} />
            <Route path="/solicitar-acceso" element={<SolicitarAcceso />} />
            <Route path="/bloqueado" element={<Bloqueado />} />
            <Route
              path="/"
              element={
                <PantallaAutenticada>
                  <Inicio />
                </PantallaAutenticada>
              }
            />
            {/* §3.1.b: única ruta que se puede abrir con el cambio de contraseña pendiente. */}
            <Route
              path="/cambiar-password"
              element={
                <RutaProtegida permitidaConCambioPendiente>
                  <CambiarPassword />
                </RutaProtegida>
              }
            />
            {/* §7 y §6.5: perfil propio de cualquier usuario autenticado. */}
            <Route
              path="/perfil"
              element={
                <PantallaAutenticada>
                  <Perfil />
                </PantallaAutenticada>
              }
            />
            <Route
              path="/admin/odontologos/nuevo"
              element={
                <PantallaAutenticada rolesPermitidos={ROLES_ADMIN}>
                  <OdontologoFormulario />
                </PantallaAutenticada>
              }
            />
            <Route
              path="/admin/solicitudes"
              element={
                <PantallaAutenticada rolesPermitidos={ROLES_ADMIN}>
                  <SolicitudesListado />
                </PantallaAutenticada>
              }
            />
            <Route
              path="/admin/tipos-trabajo"
              element={
                <PantallaAutenticada rolesPermitidos={ROLES_ADMIN}>
                  <TiposTrabajoListado />
                </PantallaAutenticada>
              }
            />
            <Route
              path="/admin/tipos-trabajo/nuevo"
              element={
                <PantallaAutenticada rolesPermitidos={ROLES_ADMIN}>
                  <TipoTrabajoFormulario />
                </PantallaAutenticada>
              }
            />
            <Route
              path="/admin/tipos-trabajo/:id/editar"
              element={
                <PantallaAutenticada rolesPermitidos={ROLES_ADMIN}>
                  <TipoTrabajoFormulario />
                </PantallaAutenticada>
              }
            />
          </Routes>
        </BrowserRouter>
      </SesionProvider>
    </QueryClientProvider>
  );
}

export default App;
