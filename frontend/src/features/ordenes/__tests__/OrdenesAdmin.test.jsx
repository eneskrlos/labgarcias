import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import OrdenesAdmin from '../OrdenesAdmin';
import { listarOrdenesAdmin } from '../api';
import { listarEstados, listarTiposOrden } from '../../catalogos/api';
import { listarOdontologosActivos } from '../../auth/api';
import { ApiError } from '../../../shared/api/cliente';

vi.mock('../api', () => ({ listarOrdenesAdmin: vi.fn() }));
vi.mock('../../catalogos/api', () => ({ listarEstados: vi.fn(), listarTiposOrden: vi.fn() }));
vi.mock('../../auth/api', () => ({ listarOdontologosActivos: vi.fn() }));

const ESTADOS = [
  { codigo: 'RECIBIDO', nombre: 'Recibido', esTerminal: false, esProductivo: true, activo: true },
  { codigo: 'LISTO', nombre: 'Listo', esTerminal: false, esProductivo: true, activo: true },
];

const TIPOS_ORDEN = [
  { codigo: 'NORMAL', nombre: 'Normal', recargoMonto: '0.00' },
  { codigo: 'URGENTE', nombre: 'Urgente', recargoMonto: '200.00' },
];

const ODONTOLOGOS = [
  { id: 3, nombreCompleto: 'Dr. Ernesto Pérez' },
  { id: 17, nombreCompleto: 'Dr. Juan Pérez' },
];

const ORDEN = {
  id: 7,
  codigo: 'LG-0007',
  pacienteIdentificacion: 'I.D. - Caso #1004',
  tipoTrabajo: 'BIMLER A ESTANDAR',
  tipoOrden: 'Urgente',
  estado: 'En produccion',
  fechaIngreso: '2026-08-22',
  fechaEstimadaEntrega: '2026-09-01',
  precioTotal: '450.00',
};

const PAGINA = { contenido: [ORDEN], total: 1, pagina: 0, tamano: 10, totalPaginas: 1 };

function renderizar(ruta = '/admin/ordenes') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[ruta]}>
        <Routes>
          <Route path="/admin/ordenes" element={<OrdenesAdmin />} />
          <Route path="/admin/ordenes/:id" element={<p>Gestión</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('OrdenesAdmin', () => {
  beforeEach(() => {
    listarOrdenesAdmin.mockReset().mockResolvedValue(PAGINA);
    listarEstados.mockReset().mockResolvedValue(ESTADOS);
    listarTiposOrden.mockReset().mockResolvedValue(TIPOS_ORDEN);
    listarOdontologosActivos.mockReset().mockResolvedValue(ODONTOLOGOS);
  });

  /** §8.1 Regla 2: la paginación la resuelve el backend, con page y size en la URL. */
  it('pide la primera página con tamaño 10 y sin filtros', async () => {
    renderizar();
    await screen.findByText('LG-0007');

    expect(listarOrdenesAdmin).toHaveBeenCalledWith({
      pagina: 0,
      tamano: 10,
      estado: null,
      tipoOrden: null,
      odontologoId: null,
    });
  });

  it('respeta page, size y los filtros de la URL', async () => {
    renderizar('/admin/ordenes?page=1&size=20&estado=LISTO&tipoOrden=URGENTE&odontologoId=17');
    await screen.findByText('LG-0007');

    expect(listarOrdenesAdmin).toHaveBeenCalledWith({
      pagina: 1,
      tamano: 20,
      estado: 'LISTO',
      tipoOrden: 'URGENTE',
      odontologoId: '17',
    });
  });

  /** §5.7: los tres filtros los resuelve el backend y vuelven a la página 0. */
  it('el filtro por odontólogo viaja al backend y reinicia la paginación', async () => {
    renderizar('/admin/ordenes?page=2&size=10');
    await screen.findByText('LG-0007');

    await userEvent.selectOptions(screen.getByLabelText('Odontólogo'), '3');

    expect(listarOrdenesAdmin).toHaveBeenLastCalledWith({
      pagina: 0,
      tamano: 10,
      estado: null,
      tipoOrden: null,
      odontologoId: '3',
    });
  });

  it('ofrece los tres filtros de §5.7', async () => {
    renderizar();
    await screen.findByText('LG-0007');

    expect(screen.getByLabelText('Estado')).toBeInTheDocument();
    expect(screen.getByLabelText('Tipo')).toBeInTheDocument();
    expect(screen.getByLabelText('Odontólogo')).toBeInTheDocument();
  });

  /** RN-22 y Agente.md §8.2: ningún listado muestra el nombre del paciente, tampoco el del admin. */
  it('identifica al paciente por iniciales y código, sin nombre', async () => {
    renderizar();

    expect(await screen.findByText('I.D. - Caso #1004')).toBeInTheDocument();
    expect(screen.queryByText(/Ignacio/)).not.toBeInTheDocument();
  });

  it('cada fila lleva al detalle del laboratorio', async () => {
    renderizar();
    const fila = (await screen.findByText('LG-0007')).closest('tr');

    expect(within(fila).getByRole('link', { name: 'LG-0007' })).toHaveAttribute('href', '/admin/ordenes/7');
    expect(within(fila).getByRole('link', { name: 'Gestionar' })).toHaveAttribute('href', '/admin/ordenes/7');
  });

  /** D-19: el alta es del laboratorio y se llega desde acá (§8.1 Regla 5). */
  it('ofrece el acceso a Nueva orden', async () => {
    renderizar();
    await screen.findByText('LG-0007');

    expect(screen.getByRole('link', { name: 'Nueva orden' })).toHaveAttribute('href', '/admin/ordenes/nueva');
  });

  /** §8.1 Regla 1: el alta vuelve acá y su confirmación se ve. */
  it('muestra la confirmación con la que vuelve el alta', async () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter
          initialEntries={[{ pathname: '/admin/ordenes', state: { mensaje: 'Orden LG-0008 registrada.' } }]}
        >
          <Routes>
            <Route path="/admin/ordenes" element={<OrdenesAdmin />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(await screen.findByRole('status')).toHaveTextContent('Orden LG-0008 registrada.');
  });

  /** §8.1 Regla 3: los tres estados de la tabla. */
  it('muestra el vacío y el error con opción de reintentar', async () => {
    listarOrdenesAdmin.mockResolvedValue({ contenido: [], total: 0, pagina: 0, tamano: 10, totalPaginas: 0 });
    renderizar();
    expect(await screen.findByText('No hay trabajos para mostrar.')).toBeInTheDocument();

    listarOrdenesAdmin.mockRejectedValue(new ApiError(500, 'ERROR', 'Ocurrió un error inesperado.'));
    renderizar();
    expect(await screen.findByText('Ocurrió un error inesperado.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
