import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import DashboardAdmin from '../DashboardAdmin';
import { obtenerDashboardAdmin } from '../api';
import { ApiError } from '../../../shared/api/cliente';

vi.mock('../api', () => ({ obtenerDashboardAdmin: vi.fn() }));

const ORDEN = {
  id: 7,
  codigo: 'LG-0007',
  pacienteIdentificacion: 'J.G. - Caso #1004',
  tipoTrabajo: 'DISYUNTOR CON TORNILLO ESTANDAR',
  tipoOrden: 'Urgente',
  estado: 'En producción',
  fechaIngreso: '2026-08-20',
  fechaEstimadaEntrega: '2026-08-28',
  precioTotal: '450.00',
};

const URGENTE = {
  id: 7,
  codigo: 'LG-0007',
  odontologo: 'Dr. Juan Pérez',
  estado: 'En producción',
  fechaEstimadaEntrega: '2026-08-28',
};

const DASHBOARD = {
  contadores: { enCurso: 12, listasParaRetirar: 4, entregadasEstaSemana: 7, urgentesActivas: 2 },
  distribucionPorEstado: [
    { estadoCodigo: 'RECIBIDO', estadoNombre: 'Recibido', cantidad: 1 },
    { estadoCodigo: 'EN_PRODUCCION', estadoNombre: 'En producción', cantidad: 5 },
  ],
  proximasAEntregar: [ORDEN],
  ordenesRecientes: [ORDEN],
  urgentes: [URGENTE],
};

function renderizar(estado) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[{ pathname: '/admin', state: estado }]}>
        <Routes>
          <Route path="/admin" element={<DashboardAdmin />} />
          <Route path="/admin/ordenes" element={<p>Órdenes</p>} />
          <Route path="/admin/ordenes/:id" element={<p>Detalle</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('DashboardAdmin', () => {
  beforeEach(() => {
    obtenerDashboardAdmin.mockReset().mockResolvedValue(DASHBOARD);
  });

  /** CU-10 paso 1: los cuatro indicadores, con el número que manda el backend. */
  it('muestra los cuatro indicadores de CU-10', async () => {
    renderizar();
    // Se espera un dato de la respuesta: la etiqueta ya está en pantalla mientras carga.
    await screen.findByText('Dr. Juan Pérez');

    expect(screen.getByText('Trabajos en curso').parentElement).toHaveTextContent('12');
    expect(screen.getByText('Listos para retirar').parentElement).toHaveTextContent('4');
    expect(screen.getByText('Entregados esta semana').parentElement).toHaveTextContent('7');
    expect(screen.getByText('Urgentes activos').parentElement).toHaveTextContent('2');
  });

  /** CU-10 paso 2: distribución por estado, con el nombre editable que trae el backend. */
  it('muestra la distribución por estado con nombre y cantidad', async () => {
    renderizar();
    await screen.findByText('Dr. Juan Pérez');

    // Acotado a la lista: "En producción" también es el estado de las filas de las tablas.
    const distribucion = within(screen.getByRole('list'));
    expect(distribucion.getByText('Recibido')).toBeInTheDocument();
    expect(distribucion.getByText('En producción')).toBeInTheDocument();
    expect(distribucion.getByText('5')).toBeInTheDocument();
    expect(distribucion.getByText('1')).toBeInTheDocument();
  });

  /** CU-10 paso 3: próximos a entregar, recientes y urgentes, cada uno con su bloque. */
  it('muestra los tres bloques de resumen', async () => {
    renderizar();
    const enlaces = await screen.findAllByRole('link', { name: 'LG-0007' });

    expect(screen.getByRole('heading', { name: 'Próximos a entregar' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Urgentes' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Trabajos recientes' })).toBeInTheDocument();
    expect(enlaces[0]).toHaveAttribute('href', '/admin/ordenes/7');
  });

  /** El bloque de urgentes muestra el dueño: el laboratorio ve las órdenes de todos. */
  it('el bloque de urgentes nombra al odontólogo', async () => {
    renderizar();

    expect(await screen.findByText('Dr. Juan Pérez')).toBeInTheDocument();
  });

  /**
   * RN-22 / `Agente.md` §8.2: `v_ordenes_urgentes` trae `paciente_nombre` y el dashboard no lo
   * muestra en ningún bloque, ni siquiera si el backend lo mandara por error.
   */
  it('rn22 no muestra el nombre del paciente en ningún bloque', async () => {
    obtenerDashboardAdmin.mockResolvedValue({
      ...DASHBOARD,
      urgentes: [{ ...URGENTE, pacienteNombre: 'Martín Pérez' }],
    });
    renderizar();
    await screen.findByText('Dr. Juan Pérez');

    expect(screen.queryByText(/Martín Pérez/)).not.toBeInTheDocument();
  });

  /** §8: ningún contador se deriva en la pantalla; se muestra lo que llega. */
  it('no recalcula los contadores a partir de los bloques', async () => {
    obtenerDashboardAdmin.mockResolvedValue({
      ...DASHBOARD,
      contadores: { enCurso: 0, listasParaRetirar: 0, entregadasEstaSemana: 0, urgentesActivas: 0 },
    });
    renderizar();
    await screen.findByText('Dr. Juan Pérez');

    expect(screen.getByText('Urgentes activos').parentElement).toHaveTextContent('0');
  });

  /** §8.1 Regla 3: cada bloque contempla su caso vacío. */
  it('muestra los mensajes de vacío cuando no hay nada que listar', async () => {
    obtenerDashboardAdmin.mockResolvedValue({
      contadores: { enCurso: 0, listasParaRetirar: 0, entregadasEstaSemana: 0, urgentesActivas: 0 },
      distribucionPorEstado: [],
      proximasAEntregar: [],
      ordenesRecientes: [],
      urgentes: [],
    });
    renderizar();

    expect(await screen.findByText('No hay trabajos por entregar.')).toBeInTheDocument();
    expect(screen.getByText('No hay trabajos urgentes sin terminar.')).toBeInTheDocument();
    expect(screen.getByText('Todavía no hay trabajos registrados.')).toBeInTheDocument();
  });

  /** §8.1 Regla 3: el error se muestra y se puede reintentar. */
  it('muestra el error con opción de reintentar', async () => {
    obtenerDashboardAdmin.mockRejectedValue(new ApiError(500, 'ERROR', 'No se pudo cargar el dashboard.'));
    renderizar();

    expect(await screen.findAllByText('No se pudo cargar el dashboard.')).not.toHaveLength(0);
    expect(screen.getAllByRole('button', { name: 'Reintentar' })[0]).toBeInTheDocument();
  });

  /**
   * §8.1 Regla 1: el alta de odontólogo vuelve **al listado de CU-11**, que existe desde T-28.
   * Pasó por acá mientras esa pantalla no existía; el dashboard ya no recibe confirmaciones.
   */
  it('el dashboard ya no recibe confirmaciones de ningún formulario', async () => {
    renderizar({ mensaje: 'Cuenta creada.' });
    await screen.findByText('Dr. Juan Pérez');

    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  /** Los bloques son resúmenes, no listados navegables: sin controles de paginación. */
  it('ningún bloque dibuja controles de paginación', async () => {
    renderizar();
    await screen.findByText('Dr. Juan Pérez');

    expect(screen.queryByRole('button', { name: /siguiente/i })).not.toBeInTheDocument();
  });
});
