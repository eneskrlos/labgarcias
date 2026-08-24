import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import HistorialOrdenes from '../HistorialOrdenes';
import { listarMisOrdenes } from '../api';
import { listarEstados } from '../../catalogos/api';
import { ApiError } from '../../../shared/api/cliente';

vi.mock('../api', () => ({ listarMisOrdenes: vi.fn() }));
vi.mock('../../catalogos/api', () => ({ listarEstados: vi.fn() }));

const ESTADOS = [
  { codigo: 'RECIBIDO', nombre: 'Recibido', esTerminal: false, esProductivo: true, activo: true },
  { codigo: 'EN_PRODUCCION', nombre: 'En producción', esTerminal: false, esProductivo: true, activo: true },
  { codigo: 'ENTREGADO', nombre: 'Entregado', esTerminal: true, esProductivo: true, activo: true },
  { codigo: 'CANCELADO', nombre: 'Cancelado', esTerminal: true, esProductivo: false, activo: true },
];

const ORDEN = {
  id: 4,
  codigo: 'LG-0004',
  pacienteIdentificacion: 'M.P. - Caso #1001',
  tipoTrabajo: 'DISYUNTOR CON TORNILLO ESTANDAR',
  tipoOrden: 'Urgente',
  estado: 'Entregado',
  fechaIngreso: '2026-08-10',
  fechaEstimadaEntrega: '2026-08-19',
  precioTotal: '450.00',
};

const PAGINA = { contenido: [ORDEN], total: 1, pagina: 0, tamano: 10, totalPaginas: 1 };

function renderizar(ruta = '/historial') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[ruta]}>
        <Routes>
          <Route path="/historial" element={<HistorialOrdenes />} />
          <Route path="/ordenes/:id" element={<p>Seguimiento</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('HistorialOrdenes', () => {
  beforeEach(() => {
    listarMisOrdenes.mockReset().mockResolvedValue(PAGINA);
    listarEstados.mockReset().mockResolvedValue(ESTADOS);
  });

  /** CU-12: el historial pide siempre `historico`, que es lo que acota a los estados terminales. */
  it('cu12 pide siempre las órdenes cerradas', async () => {
    renderizar();
    await screen.findByText('LG-0004');

    expect(listarMisOrdenes).toHaveBeenCalledWith({
      pagina: 0,
      tamano: 10,
      estado: null,
      historico: true,
    });
  });

  /**
   * RN-04: qué estados son terminales lo dice el catálogo (`esTerminal`), no una lista escrita
   * acá. El selector solo ofrece esos, y nunca los productivos.
   */
  it('rn04 el filtro solo ofrece los estados terminales del catálogo', async () => {
    renderizar();
    await screen.findByText('LG-0004');

    const opciones = within(screen.getByLabelText('Estado'))
      .getAllByRole('option')
      .map((opcion) => opcion.textContent);
    expect(opciones).toEqual(['Todos', 'Entregado', 'Cancelado']);
  });

  /** El filtro entre entregadas y canceladas usa el parámetro `estado` que §5.3 ya tenía. */
  it('el filtro reutiliza el parámetro estado y lo deja en la URL', async () => {
    renderizar();
    await screen.findByText('LG-0004');

    await userEvent.selectOptions(screen.getByLabelText('Estado'), 'CANCELADO');

    expect(listarMisOrdenes).toHaveBeenLastCalledWith({
      pagina: 0,
      tamano: 10,
      estado: 'CANCELADO',
      historico: true,
    });
  });

  /** §8.1 Regla 2: page y size viven en la URL y sobreviven a un refresco. */
  it('respeta la página y el tamaño que vienen en la URL', async () => {
    renderizar('/historial?page=2&size=20');
    await screen.findByText('LG-0004');

    expect(listarMisOrdenes).toHaveBeenCalledWith({
      pagina: 2,
      tamano: 20,
      estado: null,
      historico: true,
    });
  });

  /** RN-22: el historial identifica al paciente por iniciales y código. */
  it('rn22 no muestra el nombre del paciente', async () => {
    renderizar();
    await screen.findByText('LG-0004');

    expect(screen.getByText('M.P. - Caso #1001')).toBeInTheDocument();
    expect(screen.queryByText(/Martín Pérez/)).not.toBeInTheDocument();
  });

  /** CU-12 A1: sin trabajos históricos, el listado sale vacío con su mensaje. */
  it('cu12a1 sin trabajos finalizados muestra el listado vacío', async () => {
    listarMisOrdenes.mockResolvedValue({ contenido: [], total: 0, pagina: 0, tamano: 10, totalPaginas: 0 });
    renderizar();

    expect(await screen.findByText('Todavía no tenés trabajos finalizados.')).toBeInTheDocument();
  });

  /** §8.1 Regla 3: el error se muestra y se puede reintentar. */
  it('muestra el error con opción de reintentar', async () => {
    listarMisOrdenes.mockRejectedValue(new ApiError(500, 'ERROR', 'No se pudo cargar el historial.'));
    renderizar();

    expect(await screen.findByText('No se pudo cargar el historial.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });

  /** Cada caso enlaza a su seguimiento, que es la pantalla de CU-04. */
  it('cada caso enlaza a su seguimiento', async () => {
    renderizar();
    await screen.findByText('LG-0004');

    expect(screen.getByRole('link', { name: 'LG-0004' })).toHaveAttribute('href', '/ordenes/4');
  });
});
