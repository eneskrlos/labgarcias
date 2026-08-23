import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import MisOrdenes from '../MisOrdenes';
import { listarMisOrdenes } from '../api';
import { listarEstados } from '../../catalogos/api';
import { ApiError } from '../../../shared/api/cliente';

vi.mock('../api', () => ({ listarMisOrdenes: vi.fn() }));
vi.mock('../../catalogos/api', () => ({ listarEstados: vi.fn() }));

const ESTADOS = [
  { codigo: 'RECIBIDO', nombre: 'Recibido', esTerminal: false, esProductivo: false, activo: true },
  { codigo: 'EN_PRODUCCION', nombre: 'En produccion', esTerminal: false, esProductivo: true, activo: true },
  { codigo: 'ENTREGADO', nombre: 'Entregado', esTerminal: true, esProductivo: false, activo: true },
];

const ORDEN = {
  id: 5,
  codigo: 'LG-0005',
  pacienteIdentificacion: 'M.P. - Caso #1002',
  tipoTrabajo: 'AAK',
  tipoOrden: 'Normal',
  estado: 'Recibido',
  fechaIngreso: '2026-08-18',
  fechaEstimadaEntrega: '2026-08-27',
  precioTotal: '250.00',
};

const PAGINA = { contenido: [ORDEN], total: 1, pagina: 0, tamano: 10, totalPaginas: 1 };

function renderizar(ruta = '/ordenes') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[ruta]}>
        <Routes>
          <Route path="/ordenes" element={<MisOrdenes />} />
          <Route path="/ordenes/:id" element={<p>Seguimiento</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('MisOrdenes', () => {
  beforeEach(() => {
    listarMisOrdenes.mockReset().mockResolvedValue(PAGINA);
    listarEstados.mockReset().mockResolvedValue(ESTADOS);
  });

  /** §8.1 Regla 2 y Agente.md 6.2: la paginación la resuelve el backend, con page y size en la URL. */
  it('pide la primera página con tamaño 10 y sin filtro de estado', async () => {
    renderizar();
    await screen.findByText('LG-0005');

    expect(listarMisOrdenes).toHaveBeenCalledWith({ pagina: 0, tamano: 10, estado: null });
  });

  it('respeta page y size de la URL', async () => {
    renderizar('/ordenes?page=2&size=30');
    await screen.findByText('LG-0005');

    expect(listarMisOrdenes).toHaveBeenCalledWith({ pagina: 2, tamano: 30, estado: null });
  });

  /** §5.3: el filtro también lo resuelve el backend; nunca se corta una lista ya traída. */
  it('el filtro de estado viaja al backend y vuelve a la página 0', async () => {
    renderizar('/ordenes?page=2&size=10');
    await screen.findByText('LG-0005');

    await userEvent.selectOptions(screen.getByLabelText('Estado'), 'EN_PRODUCCION');

    expect(listarMisOrdenes).toHaveBeenLastCalledWith({ pagina: 0, tamano: 10, estado: 'EN_PRODUCCION' });
  });

  /** RN-22: el paciente se identifica por iniciales y código. El nombre no llega ni se muestra. */
  it('identifica al paciente por iniciales y código', async () => {
    renderizar();

    expect(await screen.findByText('M.P. - Caso #1002')).toBeInTheDocument();
  });

  /** RN-01: esta pantalla es la del dueño; no hay forma de pedir las órdenes de otro. */
  it('no ofrece ningún filtro por odontólogo', async () => {
    renderizar();
    await screen.findByText('LG-0005');

    expect(screen.queryByLabelText(/odontólogo/i)).not.toBeInTheDocument();
    expect(listarMisOrdenes.mock.calls.every(([argumentos]) => !('odontologoId' in argumentos))).toBe(true);
  });

  /** RN-17/D-19: el odontólogo no crea ni edita órdenes, así que §8.1 Regla 1 no aplica acá. */
  it('no ofrece "Nuevo" ni "Editar"', async () => {
    renderizar();
    await screen.findByText('LG-0005');

    expect(screen.queryByRole('link', { name: /nuev/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /editar/i })).not.toBeInTheDocument();
  });

  it('cada fila enlaza al seguimiento de esa orden', async () => {
    renderizar();
    const fila = (await screen.findByText('LG-0005')).closest('tr');

    expect(within(fila).getByRole('link', { name: 'LG-0005' })).toHaveAttribute('href', '/ordenes/5');
    expect(within(fila).getByRole('link', { name: 'Ver seguimiento' })).toHaveAttribute('href', '/ordenes/5');
  });

  /** §8.1 Regla 3: los tres estados de la tabla. */
  it('muestra el vacío y el error con opción de reintentar', async () => {
    listarMisOrdenes.mockResolvedValue({ contenido: [], total: 0, pagina: 0, tamano: 10, totalPaginas: 0 });
    renderizar();
    expect(await screen.findByText('No tenés trabajos para mostrar.')).toBeInTheDocument();

    listarMisOrdenes.mockRejectedValue(new ApiError(500, 'ERROR', 'Ocurrió un error inesperado.'));
    renderizar();
    expect(await screen.findByText('Ocurrió un error inesperado.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
