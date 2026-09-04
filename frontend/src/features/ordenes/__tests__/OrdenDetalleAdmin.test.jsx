import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import OrdenDetalleAdmin from '../OrdenDetalleAdmin';
import { avanzarEstado, descargarArchivo, obtenerOrden } from '../api';
import { ApiError } from '../../../shared/api/cliente';

vi.mock('../api', () => ({
  obtenerOrden: vi.fn(),
  avanzarEstado: vi.fn(),
  descargarArchivo: vi.fn(),
}));

const ORDEN = {
  id: 7,
  codigo: 'LG-0007',
  pacienteIdentificacion: 'I.D. - Caso #1004',
  pacienteNombre: 'Ignacio Duarte',
  tipoTrabajo: 'BIMLER A ESTANDAR',
  tipoOrden: 'Urgente',
  estado: 'En produccion',
  estadoCodigo: 'EN_PRODUCCION',
  descripcion: 'Caso urgente',
  fechaIngreso: '2026-08-22',
  fechaEstimadaEntrega: '2026-09-01',
  precioBase: '250.00',
  recargoUrgencia: '200.00',
  precioTotal: '450.00',
  archivos: [
    { id: 9, nombreOriginal: 'molde.pdf', categoria: 'DOCUMENTO', tipoMime: 'application/pdf', tamanoBytes: 2048, fechaCarga: '2026-08-22T10:00:00-03:00' },
  ],
  lineaTiempo: [
    { estado: 'En evaluacion', fechaHora: '2026-08-22T10:00:00-03:00', autor: null },
    { estado: 'En produccion', fechaHora: '2026-08-23T09:00:00-03:00', autor: 'Mona' },
  ],
  siguienteEstado: { codigo: 'CONTROL_CALIDAD', nombre: 'Control de calidad' },
};

function renderizar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/admin/ordenes/7']}>
        <Routes>
          <Route path="/admin/ordenes/:id" element={<OrdenDetalleAdmin />} />
          <Route path="/admin/ordenes" element={<p>Trabajos</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('OrdenDetalleAdmin', () => {
  beforeEach(() => {
    obtenerOrden.mockReset().mockResolvedValue(ORDEN);
    avanzarEstado.mockReset().mockResolvedValue({ ...ORDEN, estado: 'Control de calidad' });
    descargarArchivo.mockReset().mockResolvedValue(new Blob(['contenido']));
  });

  /** §5.4: el laboratorio sí ve el nombre del paciente, y es la única pantalla donde aparece. */
  it('muestra el nombre del paciente además de su identificación', async () => {
    renderizar();

    expect(await screen.findByText('Ignacio Duarte')).toBeInTheDocument();
    expect(screen.getByText('I.D. - Caso #1004')).toBeInTheDocument();
  });

  /** §5.5: el botón ofrece una sola transición, la que calculó el backend. */
  it('ofrece únicamente la transición siguiente que trae el backend', async () => {
    renderizar();

    const boton = await screen.findByRole('button', { name: 'Avanzar a Control de calidad' });
    await userEvent.click(boton);

    expect(avanzarEstado).toHaveBeenCalledWith('7', 'CONTROL_CALIDAD');
  });

  /** RN-04/P-02: sin saltos ni retrocesos. La pantalla no ofrece elegir el estado destino. */
  it('no deja elegir a qué estado avanzar', async () => {
    renderizar();
    await screen.findByRole('button', { name: 'Avanzar a Control de calidad' });

    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Avanzar a Listo|Avanzar a Entregado/ })).not.toBeInTheDocument();
  });

  /** §5.5: desde ENTREGADO o CANCELADO no hay transición, y el backend lo dice con null. */
  it('sin transición posible no muestra el botón de avance', async () => {
    obtenerOrden.mockResolvedValue({ ...ORDEN, estado: 'Entregado', siguienteEstado: null });
    renderizar();

    expect(await screen.findByText(/ya no admite cambios de estado/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Avanzar/ })).not.toBeInTheDocument();
  });

  /** §5.5 y RN-17/CU-20: el laboratorio no cancela; la cancelación es del odontólogo. */
  it('no ofrece cancelar la orden', async () => {
    renderizar();
    await screen.findByText('Seguimiento del trabajo');

    expect(screen.queryByRole('button', { name: /cancelar/i })).not.toBeInTheDocument();
  });

  it('muestra el motivo cuando el backend rechaza la transición', async () => {
    avanzarEstado.mockRejectedValue(
      new ApiError(409, 'TRANSICION_NO_PERMITIDA', 'Desde En produccion solo se puede avanzar a la etapa inmediatamente siguiente.'),
    );
    renderizar();

    await userEvent.click(await screen.findByRole('button', { name: 'Avanzar a Control de calidad' }));

    expect(await screen.findByText(/solo se puede avanzar a la etapa inmediatamente siguiente/)).toBeInTheDocument();
  });

  /** §5.4 criterio 1: la línea de tiempo, con el autor de cada etapa. */
  it('muestra la línea de tiempo fechada', async () => {
    renderizar();

    expect(await screen.findByText('Seguimiento del trabajo')).toBeInTheDocument();
    expect(screen.getByText('Mona')).toBeInTheDocument();
    expect(screen.getByText('Sistema')).toBeInTheDocument();
  });

  /** Pendiente heredado de T-25: la descarga va por el helper autenticado, no por un <a href>. */
  it('descarga los adjuntos por el endpoint autenticado', async () => {
    URL.createObjectURL = vi.fn().mockReturnValue('blob:archivo');
    URL.revokeObjectURL = vi.fn();
    renderizar();
    await screen.findByText('molde.pdf');

    await userEvent.click(screen.getByRole('button', { name: 'Descargar' }));

    expect(descargarArchivo).toHaveBeenCalledWith(9);
    expect(URL.createObjectURL).toHaveBeenCalled();
  });
});
