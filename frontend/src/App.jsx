import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Link, useNavigate } from 'react-router-dom';
import { SesionProvider, useSesion } from './shared/hooks/useSesion';
import { RutaProtegida } from './shared/components/RutaProtegida';
import { logout } from './features/auth/api';
import Login from './features/auth/Login';
import Registro from './features/auth/Registro';
import Verificar from './features/auth/Verificar';
import Bloqueado from './features/auth/Bloqueado';
import TiposTrabajoListado from './features/catalogos/TiposTrabajoListado';
import TipoTrabajoFormulario from './features/catalogos/TipoTrabajoFormulario';

const queryClient = new QueryClient();
const ROLES_ADMIN = ['ADMIN', 'SUPERADMIN'];

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
      <h1>Lab. Garcia's Connect</h1>
      <p>
        Hola, {usuario.nombreCompleto} ({usuario.rol})
      </p>
      {ROLES_ADMIN.includes(usuario.rol) && (
        <p>
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
            <Route path="/login" element={<Login />} />
            <Route path="/registro" element={<Registro />} />
            <Route path="/verificar" element={<Verificar />} />
            <Route path="/bloqueado" element={<Bloqueado />} />
            <Route
              path="/"
              element={
                <RutaProtegida>
                  <Inicio />
                </RutaProtegida>
              }
            />
            <Route
              path="/admin/tipos-trabajo"
              element={
                <RutaProtegida rolesPermitidos={ROLES_ADMIN}>
                  <TiposTrabajoListado />
                </RutaProtegida>
              }
            />
            <Route
              path="/admin/tipos-trabajo/nuevo"
              element={
                <RutaProtegida rolesPermitidos={ROLES_ADMIN}>
                  <TipoTrabajoFormulario />
                </RutaProtegida>
              }
            />
            <Route
              path="/admin/tipos-trabajo/:id/editar"
              element={
                <RutaProtegida rolesPermitidos={ROLES_ADMIN}>
                  <TipoTrabajoFormulario />
                </RutaProtegida>
              }
            />
          </Routes>
        </BrowserRouter>
      </SesionProvider>
    </QueryClientProvider>
  );
}

export default App;
