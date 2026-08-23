import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { useLocation, useNavigate } from 'react-router-dom';
import { SesionProvider, useSesion } from './shared/hooks/useSesion';
import { RutaProtegida } from './shared/components/RutaProtegida';
import { LayoutAutenticado } from './shared/components/LayoutAutenticado';
import { PantallaPendiente } from './shared/components/PantallaPendiente';
import { logout } from './features/auth/api';
import Login from './features/auth/Login';
import Bloqueado from './features/auth/Bloqueado';
import SolicitarAcceso from './features/auth/SolicitarAcceso';
import SolicitudesListado from './features/auth/SolicitudesListado';
import CambiarPassword from './features/auth/CambiarPassword';
import OdontologoFormulario from './features/auth/OdontologoFormulario';
import Campana from './features/notificaciones/Campana';
import Perfil from './features/perfil/Perfil';
import OrdenFormulario from './features/ordenes/OrdenFormulario';
import MisOrdenes from './features/ordenes/MisOrdenes';
import OrdenDetalle from './features/ordenes/OrdenDetalle';
import MenuOdontologo from './features/ordenes/MenuOdontologo';
import MenuAdmin from './features/ordenes/MenuAdmin';
import OrdenesAdmin from './features/ordenes/OrdenesAdmin';
import OrdenDetalleAdmin from './features/ordenes/OrdenDetalleAdmin';
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
      <LayoutAutenticado acciones={<Campana />} navegacion={navegacion}>
        {children}
      </LayoutAutenticado>
    </RutaProtegida>
  );
}

function Inicio() {
  const { usuario, cerrarSesion } = useSesion();
  const navigate = useNavigate();
  const location = useLocation();

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
      {/* §8.1 Regla 1: los formularios vuelven acá con su confirmación mientras no exista el
          listado al que deberían volver. */}
      {location.state?.mensaje && <p role="status">{location.state.mensaje}</p>}
      <p>
        Hola, {usuario.nombreCompleto} ({usuario.rol})
      </p>
      {/* La navegación vive en el menú de §8 del encabezado, uno por rol. Repetirla acá sería
          mantener dos listas de enlaces que se desincronizan. */}
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
            {/* §8: el ítem del menú existe desde T-25; la pantalla de CU-12 la construye T-27. */}
            <Route
              path="/historial"
              element={
                <PantallaAutenticada rolesPermitidos={[ROL_ODONTOLOGO]}>
                  <PantallaPendiente
                    titulo="Historial"
                    detalle="Vas a poder consultar acá tus trabajos entregados y cancelados."
                  />
                </PantallaAutenticada>
              }
            />
            {/* CU-10: el panel del laboratorio es de T-27; el ítem del menú ya existe. */}
            <Route
              path="/admin"
              element={
                <PantallaAutenticada rolesPermitidos={ROLES_ADMIN}>
                  <PantallaPendiente
                    titulo="Dashboard"
                    detalle="Vas a ver acá los contadores del laboratorio y los trabajos próximos a entregar."
                  />
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
            {/* CU-21: la configuración de canales es de T-34; sus endpoints ya existen. */}
            <Route
              path="/admin/configuracion"
              element={
                <PantallaAutenticada rolesPermitidos={ROLES_ADMIN}>
                  <PantallaPendiente
                    titulo="Configuración"
                    detalle="Vas a poder elegir acá por qué canales recibís las notificaciones."
                  />
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
