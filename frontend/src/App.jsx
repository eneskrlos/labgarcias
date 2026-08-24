import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { SesionProvider, useSesion } from './shared/hooks/useSesion';
import { RutaProtegida } from './shared/components/RutaProtegida';
import { LayoutAutenticado } from './shared/components/LayoutAutenticado';
import { PantallaPendiente } from './shared/components/PantallaPendiente';
import Login from './features/auth/Login';
import Bloqueado from './features/auth/Bloqueado';
import SolicitarAcceso from './features/auth/SolicitarAcceso';
import SolicitudesListado from './features/auth/SolicitudesListado';
import CambiarPassword from './features/auth/CambiarPassword';
import BotonCerrarSesion from './features/auth/BotonCerrarSesion';
import OdontologoFormulario from './features/auth/OdontologoFormulario';
import Campana from './features/notificaciones/Campana';
import ConfiguracionNotificaciones from './features/notificaciones/ConfiguracionNotificaciones';
import Perfil from './features/perfil/Perfil';
import OrdenFormulario from './features/ordenes/OrdenFormulario';
import MisOrdenes from './features/ordenes/MisOrdenes';
import OrdenDetalle from './features/ordenes/OrdenDetalle';
import MenuOdontologo from './features/ordenes/MenuOdontologo';
import MenuAdmin from './features/ordenes/MenuAdmin';
import OrdenesAdmin from './features/ordenes/OrdenesAdmin';
import OrdenDetalleAdmin from './features/ordenes/OrdenDetalleAdmin';
import HistorialOrdenes from './features/ordenes/HistorialOrdenes';
import PanelOdontologo from './features/dashboard/PanelOdontologo';
import DashboardAdmin from './features/dashboard/DashboardAdmin';
import TiposTrabajoListado from './features/catalogos/TiposTrabajoListado';
import TipoTrabajoFormulario from './features/catalogos/TipoTrabajoFormulario';

const queryClient = new QueryClient();
const ROLES_ADMIN = ['ADMIN', 'SUPERADMIN'];
const ROL_ODONTOLOGO = 'ODONTOLOGO';

/**
 * Toda ruta con sesión iniciada comparte el encabezado, y la campana vive ahí (§6.4): montarla
 * en una sola pantalla la dejaría invisible en el resto.
 *
 * §8 define **dos menús distintos**, uno por rol: T-25 montó el del odontólogo y T-26 el del
 * laboratorio. Los ítems cuya pantalla todavía no existe llevan a una `PantallaPendiente`, para
 * que el menú sea el de §8 completo sin que ninguno caiga en una página en blanco.
 */
function PantallaAutenticada({ rolesPermitidos, children }) {
  const { usuario } = useSesion();
  const navegacion = usuario?.rol === ROL_ODONTOLOGO ? <MenuOdontologo /> : <MenuAdmin />;

  return (
    <RutaProtegida rolesPermitidos={rolesPermitidos}>
      <LayoutAutenticado
        acciones={
          <>
            <Campana />
            <BotonCerrarSesion />
          </>
        }
        navegacion={navegacion}
      >
        {children}
      </LayoutAutenticado>
    </RutaProtegida>
  );
}

/**
 * §8: `/` deja de ser un inicio genérico y manda a cada rol a su panel — el odontólogo a
 * `/inicio` (CU-02) y la administración a `/admin` (CU-10).
 *
 * §8 le asigna `/inicio` al panel del odontólogo y ninguna de sus dieciocho pantallas es
 * compartida entre roles, así que una pantalla común en `/` no correspondía a nada de la spec.
 * Va fuera de `PantallaAutenticada` a propósito: montar el layout para redirigir enseguida haría
 * parpadear el encabezado.
 */
function InicioSegunRol() {
  const { usuario } = useSesion();
  return <Navigate to={usuario.rol === ROL_ODONTOLOGO ? '/inicio' : '/admin'} replace />;
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
            {/* §8: la raíz redirige según el rol; ningún usuario queda en una pantalla compartida. */}
            <Route
              path="/"
              element={
                <RutaProtegida>
                  <InicioSegunRol />
                </RutaProtegida>
              }
            />
            {/* CU-02: el panel del odontólogo, en la ruta que le asigna §8. */}
            <Route
              path="/inicio"
              element={
                <PantallaAutenticada rolesPermitidos={[ROL_ODONTOLOGO]}>
                  <PanelOdontologo />
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
            {/* CU-03 y CU-04: las órdenes propias del odontólogo. RN-01 lo resuelve el backend. */}
            <Route
              path="/ordenes"
              element={
                <PantallaAutenticada rolesPermitidos={[ROL_ODONTOLOGO]}>
                  <MisOrdenes />
                </PantallaAutenticada>
              }
            />
            <Route
              path="/ordenes/:id"
              element={
                <PantallaAutenticada rolesPermitidos={[ROL_ODONTOLOGO]}>
                  <OrdenDetalle />
                </PantallaAutenticada>
              }
            />
            {/* CU-12: los trabajos ya cerrados del odontólogo. El backend decide cuáles con `historico`. */}
            <Route
              path="/historial"
              element={
                <PantallaAutenticada rolesPermitidos={[ROL_ODONTOLOGO]}>
                  <HistorialOrdenes />
                </PantallaAutenticada>
              }
            />
            {/* CU-10/§5.7: el dashboard del laboratorio. */}
            <Route
              path="/admin"
              element={
                <PantallaAutenticada rolesPermitidos={ROLES_ADMIN}>
                  <DashboardAdmin />
                </PantallaAutenticada>
              }
            />
            {/* CU-06/§5.7: las órdenes de todo el laboratorio, con sus filtros. */}
            <Route
              path="/admin/ordenes"
              element={
                <PantallaAutenticada rolesPermitidos={ROLES_ADMIN}>
                  <OrdenesAdmin />
                </PantallaAutenticada>
              }
            />
            {/* §5.1 con D-19: la registra el laboratorio, no el odontólogo. */}
            <Route
              path="/admin/ordenes/nueva"
              element={
                <PantallaAutenticada rolesPermitidos={ROLES_ADMIN}>
                  <OrdenFormulario />
                </PantallaAutenticada>
              }
            />
            <Route
              path="/admin/ordenes/:id"
              element={
                <PantallaAutenticada rolesPermitidos={ROLES_ADMIN}>
                  <OrdenDetalleAdmin />
                </PantallaAutenticada>
              }
            />
            {/* CU-11: el listado de odontólogos es de T-28; el alta ya existe. */}
            <Route
              path="/admin/odontologos"
              element={
                <PantallaAutenticada rolesPermitidos={ROLES_ADMIN}>
                  <PantallaPendiente
                    titulo="Odontólogos"
                    detalle="Vas a ver acá las cuentas de los odontólogos del laboratorio."
                  />
                </PantallaAutenticada>
              }
            />
            {/* CU-21/§6.4: por qué canales recibe sus notificaciones el administrador. */}
            <Route
              path="/admin/configuracion"
              element={
                <PantallaAutenticada rolesPermitidos={ROLES_ADMIN}>
                  <ConfiguracionNotificaciones />
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
