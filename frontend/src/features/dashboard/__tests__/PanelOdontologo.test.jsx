import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import PanelOdontologo from '../PanelOdontologo';
import { obtenerPanelOdontologo } from '../api';
import { SesionProvider } from '../../../shared/hooks/useSesion';
import { guardarSesion } from '../../../shared/api/token';
import { ApiError } from '../../../shared/api/cliente';

vi.mock('../api', () => ({ obtenerPanelOdontologo: vi.fn() }));

const USUARIO = { id: 3, nombreCompleto: 'Dr. Ernesto Pérez', rol: 'ODONTOLOGO' };

const ORDEN = {
  id: 3,
  codigo: 'LG-0003',
  pacienteIdentificacion: 'M.P. - Caso #1000',
  tipoTrabajo: 'DISYUNTOR CON TORNILLO ESTANDAR',
  tipoOrden: 'Normal',
  estado: 'En producción',
  fechaIngreso: '2026-08-18',
  fechaEstimadaEntrega: '2026-08-27',
  precioTotal: '250.00',
};

const PANEL = {
  contadores: { enCurso: 3, listasParaRetirar: 1, entregadasEstaSemana: 2 },
  ordenesRecientes: [ORDEN],
};

function renderizar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <SesionProvider>
        <MemoryRouter initialEntries={['/inicio']}>
          <Routes>
            <Route path="/inicio" element={<PanelOdontologo />} />
            <Route path="/ordenes" element={<p>Mis trabajos</p>} />
            <Route path="/ordenes/:id" element={<p>Seguimiento</p>} />
          </Routes>
        </MemoryRouter>
      </SesionProvider>
    </QueryClientProvider>,
  );
}

describe('PanelOdontologo', () => {
  beforeEach(() => {
    guardarSesion('token-de-prueba', USUARIO);
    obtenerPanelOdontologo.mockReset().mockResolvedValue(PANEL);
  });

  afterEach(() => {
    localStorage.clear();
  });

  /** CU-02 paso 1: saludo personalizado. */
  it('saluda al odontólogo por su nombre', async () => {
    renderizar();

    expect(await screen.findByRole('heading', { name: /Dr\. Ernesto Pérez/ })).toBeInTheDocument();
  });

  /** CU-02 paso 2, sin "mensajes nuevos" (D-11): los tres indicadores, con el número del backend. */
  it('muestra los tres indicadores tal como los devuelve el backend', async () => {
    renderizar();
    // Se espera un dato de la respuesta: la etiqueta ya está en pantalla mientras carga.
    await screen.findByText('LG-0003');

    expect(screen.getByText('Trabajos en curso').parentElement).toHaveTextContent('3');
    expect(screen.getByText('Listos para retirar').parentElement).toHaveTextContent('1');
    expect(screen.getByText('Entregados esta semana').parentElement).toHaveTextContent('2');
  });

  /** D-11: la mensajería está pospuesta, así que el panel no tiene contador de mensajes. */
  it('no muestra contador de mensajes nuevos', async () => {
    renderizar();
    await screen.findByText('Trabajos en curso');

    expect(screen.queryByText(/mensaje/i)).not.toBeInTheDocument();
  });

  /** CU-02 paso 3: tabla de trabajos recientes con caso, paciente, trabajo, estado y entrega. */
  it('lista los trabajos recientes con las columnas de CU-02', async () => {
    renderizar();
    await screen.findByText('LG-0003');

    expect(screen.getByRole('columnheader', { name: 'Caso' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Paciente' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Trabajo' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Estado' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Entrega estimada' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'LG-0003' })).toHaveAttribute('href', '/ordenes/3');
  });

  /** RN-22: el panel identifica al paciente por iniciales y código; el nombre no aparece. */
  it('rn22 no muestra el nombre del paciente', async () => {
    renderizar();
    await screen.findByText('LG-0003');

    expect(screen.getByText('M.P. - Caso #1000')).toBeInTheDocument();
    expect(screen.queryByText(/Martín Pérez/)).not.toBeInTheDocument();
  });

  /**
   * §8/`Agente.md` §6.1: el panel no deriva ningún contador. Si el backend manda ceros, muestra
   * ceros aunque la lista de recientes tenga filas.
   */
  it('no recalcula los contadores a partir de las órdenes que recibe', async () => {
    obtenerPanelOdontologo.mockResolvedValue({
      contadores: { enCurso: 0, listasParaRetirar: 0, entregadasEstaSemana: 0 },
      ordenesRecientes: [ORDEN],
    });
    renderizar();
    await screen.findByText('LG-0003');

    expect(screen.getByText('Trabajos en curso').parentElement).toHaveTextContent('0');
  });

  /** CU-02 A1: sin trabajos, los indicadores van en cero y el listado sale vacío. */
  it('cu02a1 sin trabajos muestra cero y el listado vacío', async () => {
    obtenerPanelOdontologo.mockResolvedValue({
      contadores: { enCurso: 0, listasParaRetirar: 0, entregadasEstaSemana: 0 },
      ordenesRecientes: [],
    });
    renderizar();

    expect(await screen.findByText('Todavía no tenés trabajos registrados.')).toBeInTheDocument();
    expect(screen.getByText('Trabajos en curso').parentElement).toHaveTextContent('0');
  });

  /** §8.1 Regla 3: el error se muestra y se puede reintentar. */
  it('muestra el error con opción de reintentar', async () => {
    obtenerPanelOdontologo.mockRejectedValue(new ApiError(500, 'ERROR', 'No se pudo cargar el panel.'));
    renderizar();

    expect(await screen.findByText('No se pudo cargar el panel.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });

  /** El bloque de recientes es un resumen, no un listado navegable: no lleva paginación. */
  it('el bloque de recientes no dibuja controles de paginación', async () => {
    renderizar();
    await screen.findByText('LG-0003');

    expect(screen.queryByRole('button', { name: /siguiente/i })).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ver todos' })).toHaveAttribute('href', '/ordenes');
  });
});
