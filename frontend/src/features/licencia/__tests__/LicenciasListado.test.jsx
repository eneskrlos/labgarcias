import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import LicenciasListado from '../LicenciasListado';
import { listarLicencias, obtenerLicenciaVigente } from '../api';
import { ApiError } from '../../../shared/api/cliente';

vi.mock('../api', () => ({ listarLicencias: vi.fn(), obtenerLicenciaVigente: vi.fn() }));

const LICENCIA = {
  id: 1,
  fechaInicio: '2026-01-01',
  fechaVencimiento: '2026-12-31',
  estado: 'ACTIVA',
  activadaPorNombre: 'Ernesto Carlos',
  fechaRegistro: '2026-01-01T10:00:00-03:00',
  observacion: 'Renovación anual',
};

const PAGINA = { contenido: [LICENCIA], total: 1, pagina: 0, tamano: 10, totalPaginas: 1 };

function renderizar(ruta = '/admin/licencias', estado) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[{ pathname: ruta.split('?')[0], search: ruta.split('?')[1] ?? '', state: estado }]}>
        <Routes>
          <Route path="/admin/licencias" element={<LicenciasListado />} />
          <Route path="/admin/licencias/nueva" element={<p>Formulario</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('LicenciasListado', () => {
  beforeEach(() => {
    listarLicencias.mockReset().mockResolvedValue(PAGINA);
    obtenerLicenciaVigente.mockReset().mockResolvedValue({ vigente: true, licencia: LICENCIA });
  });

  /** §8.1 Regla 2: 10 por defecto y la paginación resuelta en el backend. */
  it('pide la primera página con tamaño 10', async () => {
    renderizar();
    await screen.findByText('Renovación anual');

    expect(listarLicencias).toHaveBeenCalledWith({ pagina: 0, tamano: 10 });
  });

  /** §8.1 Regla 2 criterio 3: recargar en ?page=2&size=20 mantiene la misma página. */
  it('respeta page y size de la URL', async () => {
    renderizar('/admin/licencias?page=2&size=20');
    await screen.findByRole('heading', { name: 'Licencias' });

    expect(listarLicencias).toHaveBeenCalledWith({ pagina: 2, tamano: 20 });
  });

  /** §8.1 Regla 5: el botón "Nuevo" arriba a la derecha lleva a la ruta de alta. */
  it('ofrece Nuevo apuntando al formulario', async () => {
    renderizar();

    expect(await screen.findByRole('link', { name: 'Nuevo' })).toHaveAttribute(
      'href',
      '/admin/licencias/nueva',
    );
  });

  /** El histórico muestra los datos del período tal como los devuelve el backend. */
  it('muestra las columnas del período', async () => {
    renderizar();
    await screen.findByText('Renovación anual');

    expect(screen.getByRole('columnheader', { name: 'Inicio' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Vencimiento' })).toBeInTheDocument();
    expect(screen.getByText('ACTIVA')).toBeInTheDocument();
    expect(screen.getByText('Ernesto Carlos')).toBeInTheDocument();
  });

  /** §8: el estado vigente lo dice el backend; no se deduce del listado. */
  it('muestra el estado vigente que devuelve el backend', async () => {
    expect(true).toBe(true);
    renderizar();

    expect(await screen.findByText(/Licencia vigente hasta el/)).toBeInTheDocument();
  });

  it('avisa cuando no hay ninguna licencia vigente', async () => {
    obtenerLicenciaVigente.mockResolvedValue({ vigente: false, licencia: null });
    renderizar();

    expect(await screen.findByText(/No hay ninguna licencia vigente/)).toBeInTheDocument();
  });

  /** §8.1 Regla 3: vacío con acceso a "Nuevo". */
  it('sin períodos muestra el mensaje vacío con su acción', async () => {
    listarLicencias.mockResolvedValue({ contenido: [], total: 0, pagina: 0, tamano: 10, totalPaginas: 0 });
    renderizar();

    expect(await screen.findByText('Todavía no hay períodos de licencia registrados.')).toBeInTheDocument();
  });

  /** §8.1 Regla 3: error con reintento. */
  it('muestra el error con opción de reintentar', async () => {
    listarLicencias.mockRejectedValue(new ApiError(500, 'ERROR', 'No se pudo cargar el histórico.'));
    renderizar();

    expect(await screen.findByText('No se pudo cargar el histórico.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });

  /** §8.1 Regla 2: el selector de tamaño ofrece 10, 20 y 30. */
  it('ofrece los tres tamaños de página', async () => {
    renderizar();
    await screen.findByText('Renovación anual');

    const opciones = screen.getAllByRole('option').map((o) => o.textContent);
    expect(opciones).toEqual(['10', '20', '30']);
  });

  /** §8.1 Regla 1: el alta vuelve acá con su confirmación. */
  it('muestra la confirmación con la que vuelve el alta', async () => {
    renderizar('/admin/licencias', { mensaje: 'Período de licencia registrado.' });

    expect(await screen.findByRole('status')).toHaveTextContent('Período de licencia registrado.');
  });

  /** Un período no se edita: se registra otro. No hay acción "Editar". */
  it('no ofrece Editar en ninguna fila', async () => {
    renderizar();
    await screen.findByText('Renovación anual');

    expect(screen.queryByRole('link', { name: /editar/i })).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('link', { name: 'Nuevo' }));
    expect(await screen.findByText('Formulario')).toBeInTheDocument();
  });
});
