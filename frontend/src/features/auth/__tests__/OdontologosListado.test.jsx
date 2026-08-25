import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import OdontologosListado from '../OdontologosListado';
import { listarOdontologos } from '../api';
import { ApiError } from '../../../shared/api/cliente';

vi.mock('../api', () => ({ listarOdontologos: vi.fn() }));

const ACTIVO = {
  id: 3,
  nombreCompleto: 'Dr. Ernesto Pérez',
  correo: 'eperez@mail.com',
  nombreUsuario: 'eperez',
  direccion: 'Av. 18 de Julio 1234',
  telefono: '+59891234567',
  estadoCuenta: 'ACTIVA',
  debeCambiarPassword: false,
};

const INACTIVO = { ...ACTIVO, id: 4, nombreCompleto: 'Dra. Ana Gómez', nombreUsuario: 'agomez', estadoCuenta: 'INACTIVA' };

const PAGINA = { contenido: [ACTIVO, INACTIVO], total: 2, pagina: 0, tamano: 10, totalPaginas: 1 };

function renderizar(ruta = '/admin/odontologos', estado) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const [pathname, search] = ruta.split('?');
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[{ pathname, search: search ?? '', state: estado }]}>
        <Routes>
          <Route path="/admin/odontologos" element={<OdontologosListado />} />
          <Route path="/admin/odontologos/nuevo" element={<p>Formulario de alta</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('OdontologosListado', () => {
  beforeEach(() => {
    listarOdontologos.mockReset().mockResolvedValue(PAGINA);
  });

  /** §8.1 Regla 2: 10 por defecto, paginación resuelta en el backend. */
  it('pide la primera página con tamaño 10', async () => {
    renderizar();
    await screen.findByText('Dr. Ernesto Pérez');

    expect(listarOdontologos).toHaveBeenCalledWith({ pagina: 0, tamano: 10 });
  });

  /** §8.1 Regla 2 criterio 3: la página vive en la URL y sobrevive a un refresco. */
  it('respeta page y size de la URL', async () => {
    renderizar('/admin/odontologos?page=1&size=30');
    await screen.findByRole('heading', { name: 'Odontólogos' });

    expect(listarOdontologos).toHaveBeenCalledWith({ pagina: 1, tamano: 30 });
  });

  /** §8.1 Regla 5: "Nuevo" arriba a la derecha, hacia el alta que ya existe desde T-31. */
  it('ofrece Nuevo apuntando al formulario de alta', async () => {
    renderizar();

    expect(await screen.findByRole('link', { name: 'Nuevo' })).toHaveAttribute(
      'href',
      '/admin/odontologos/nuevo',
    );
  });

  /** CU-11: la tabla muestra los datos de cada cuenta. */
  it('muestra los datos de cada cuenta', async () => {
    renderizar();
    await screen.findByText('Dr. Ernesto Pérez');

    expect(screen.getByRole('columnheader', { name: 'Correo' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Estado' })).toBeInTheDocument();
    expect(screen.getByText('eperez')).toBeInTheDocument();
  });

  /**
   * El listado incluye las cuentas dadas de baja: son las que hay que poder ver para saber que
   * existen. Reactivarlas es de `/admin/usuarios` (CU-17).
   */
  it('cu11 muestra también las cuentas dadas de baja', async () => {
    renderizar();
    await screen.findByText('Dra. Ana Gómez');

    expect(screen.getByText('INACTIVA')).toBeInTheDocument();
  });

  /** §7 no define ningún PUT sobre una cuenta: inventar "Editar" sería inventar un endpoint. */
  it('no ofrece Editar', async () => {
    renderizar();
    await screen.findByText('Dr. Ernesto Pérez');

    expect(screen.queryByRole('link', { name: /editar/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /editar/i })).not.toBeInTheDocument();
  });

  /** §8.1 Regla 1: el alta vuelve acá con su confirmación — el pendiente que heredó T-28. */
  it('muestra la confirmación con la que vuelve el alta', async () => {
    renderizar('/admin/odontologos', { mensaje: 'Cuenta creada. Las credenciales se enviaron por correo.' });

    expect(await screen.findByRole('status')).toHaveTextContent('Cuenta creada.');
  });

  /** §8.1 Regla 3: vacío con acceso a "Nuevo". */
  it('sin cuentas muestra el mensaje vacío', async () => {
    listarOdontologos.mockResolvedValue({ contenido: [], total: 0, pagina: 0, tamano: 10, totalPaginas: 0 });
    renderizar();

    expect(await screen.findByText('Todavía no hay cuentas de odontólogo.')).toBeInTheDocument();
  });

  /** §8.1 Regla 3: error con reintento. */
  it('muestra el error con opción de reintentar', async () => {
    listarOdontologos.mockRejectedValue(new ApiError(500, 'ERROR', 'No se pudo cargar el listado.'));
    renderizar();

    expect(await screen.findByText('No se pudo cargar el listado.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
