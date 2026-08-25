import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import UsuariosListado from '../UsuariosListado';
import { cambiarEstadoUsuario, listarUsuarios } from '../api';
import { SesionProvider } from '../../../shared/hooks/useSesion';
import { guardarSesion } from '../../../shared/api/token';
import { ApiError } from '../../../shared/api/cliente';

vi.mock('../api', () => ({ listarUsuarios: vi.fn(), cambiarEstadoUsuario: vi.fn() }));

const SUPERADMIN = { id: 11, nombreCompleto: 'Ernesto Carlos', rol: 'SUPERADMIN' };

const PROPIA = {
  id: 11,
  nombreCompleto: 'Ernesto Carlos',
  correo: 'ernesto@mail.com',
  nombreUsuario: 'erneskrlos',
  rol: 'SUPERADMIN',
  estadoCuenta: 'ACTIVA',
};

const ACTIVA = {
  id: 3,
  nombreCompleto: 'Dr. Ernesto Pérez',
  correo: 'eperez@mail.com',
  nombreUsuario: 'eperez',
  rol: 'ODONTOLOGO',
  estadoCuenta: 'ACTIVA',
};

const INACTIVA = { ...ACTIVA, id: 4, nombreCompleto: 'Dra. Ana Gómez', nombreUsuario: 'agomez', estadoCuenta: 'INACTIVA' };

const PAGINA = { contenido: [PROPIA, ACTIVA, INACTIVA], total: 3, pagina: 0, tamano: 10, totalPaginas: 1 };

function renderizar(ruta = '/admin/usuarios') {
  guardarSesion('token-de-prueba', SUPERADMIN);
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const [pathname, search] = ruta.split('?');
  render(
    <QueryClientProvider client={queryClient}>
      <SesionProvider>
        <MemoryRouter initialEntries={[{ pathname, search: search ?? '' }]}>
          <UsuariosListado />
        </MemoryRouter>
      </SesionProvider>
    </QueryClientProvider>,
  );
}

/** Devuelve la fila de la tabla que contiene ese nombre. */
function fila(nombre) {
  return screen.getByText(nombre).closest('tr');
}

describe('UsuariosListado', () => {
  beforeEach(() => {
    listarUsuarios.mockReset().mockResolvedValue(PAGINA);
    cambiarEstadoUsuario.mockReset().mockResolvedValue({ ...INACTIVA, estadoCuenta: 'ACTIVA' });
  });

  afterEach(() => {
    localStorage.clear();
  });

  /** §8.1 Regla 2: 10 por defecto, paginación en el backend. */
  it('pide la primera página con tamaño 10', async () => {
    renderizar();
    await screen.findByText('Dr. Ernesto Pérez');

    expect(listarUsuarios).toHaveBeenCalledWith({ pagina: 0, tamano: 10 });
  });

  it('respeta page y size de la URL', async () => {
    renderizar('/admin/usuarios?page=1&size=20');
    await screen.findByRole('heading', { name: 'Usuarios' });

    expect(listarUsuarios).toHaveBeenCalledWith({ pagina: 1, tamano: 20 });
  });

  /** CU-17: el padrón abarca cualquier rol, así que la tabla muestra el rol. */
  it('cu17 muestra cuentas de todos los roles con su rol', async () => {
    renderizar();
    await screen.findByText('Dr. Ernesto Pérez');

    expect(screen.getByRole('columnheader', { name: 'Rol' })).toBeInTheDocument();
    expect(within(fila('Ernesto Carlos')).getByText('SUPERADMIN')).toBeInTheDocument();
    expect(within(fila('Dr. Ernesto Pérez')).getByText('ODONTOLOGO')).toBeInTheDocument();
  });

  /** CU-17: es la única pantalla que reactiva una cuenta dada de baja. */
  it('cu17 activa una cuenta dada de baja', async () => {
    renderizar();
    await screen.findByText('Dra. Ana Gómez');

    await userEvent.click(within(fila('Dra. Ana Gómez')).getByRole('button', { name: 'Activar' }));

    expect(cambiarEstadoUsuario).toHaveBeenCalledWith(4, 'ACTIVA');
    expect(await screen.findByRole('status')).toHaveTextContent('activada');
  });

  it('cu17 desactiva una cuenta activa', async () => {
    cambiarEstadoUsuario.mockResolvedValue({ ...ACTIVA, estadoCuenta: 'INACTIVA' });
    renderizar();
    await screen.findByText('Dr. Ernesto Pérez');

    await userEvent.click(within(fila('Dr. Ernesto Pérez')).getByRole('button', { name: 'Desactivar' }));

    expect(cambiarEstadoUsuario).toHaveBeenCalledWith(3, 'INACTIVA');
    expect(await screen.findByRole('status')).toHaveTextContent('desactivada');
  });

  /**
   * CU-17: el SuperAdmin es quien reactiva a los demás. Dejarse desactivar a sí mismo podría
   * dejar el sistema sin ningún SUPERADMIN activo, e irrecuperable desde la aplicación.
   */
  it('cu17 no ofrece acción sobre la propia cuenta', async () => {
    renderizar();
    await screen.findByText('Ernesto Carlos');

    const propia = within(fila('Ernesto Carlos'));
    expect(propia.getByText('Tu cuenta')).toBeInTheDocument();
    expect(propia.queryByRole('button')).not.toBeInTheDocument();
  });

  /** §7 solo define GET y PATCH de estado: no hay alta ni edición que ofrecer. */
  it('no ofrece alta ni edición de usuarios', async () => {
    renderizar();
    await screen.findByText('Dr. Ernesto Pérez');

    expect(screen.queryByRole('link', { name: 'Nuevo' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /editar/i })).not.toBeInTheDocument();
  });

  /** El rechazo del backend se muestra; la pantalla no lo esconde. */
  it('muestra el error si el backend rechaza el cambio', async () => {
    cambiarEstadoUsuario.mockRejectedValue(
      new ApiError(422, 'AUTODESACTIVACION_NO_PERMITIDA', 'No podés cambiar el estado de tu propia cuenta.'),
    );
    renderizar();
    await screen.findByText('Dra. Ana Gómez');

    await userEvent.click(within(fila('Dra. Ana Gómez')).getByRole('button', { name: 'Activar' }));

    expect(await screen.findByText('No podés cambiar el estado de tu propia cuenta.')).toBeInTheDocument();
  });

  /** §8.1 Regla 3: error de carga con reintento. */
  it('muestra el error de carga con opción de reintentar', async () => {
    listarUsuarios.mockRejectedValue(new ApiError(500, 'ERROR', 'No se pudo cargar el padrón.'));
    renderizar();

    expect(await screen.findByText('No se pudo cargar el padrón.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
