import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import OrdenDetalle from '../OrdenDetalle';
import { cancelarOrden, descargarArchivo, obtenerOrden } from '../api';
import { listarEstados } from '../../catalogos/api';
import { ApiError } from '../../../shared/api/cliente';

vi.mock('../api', () => ({
  obtenerOrden: vi.fn(),
  cancelarOrden: vi.fn(),
  descargarArchivo: vi.fn(),
}));
vi.mock('../../catalogos/api', () => ({ listarEstados: vi.fn() }));

const ESTADOS = [
  { codigo: 'RECIBIDO', nombre: 'Recibido', esTerminal: false, esProductivo: false, activo: true },
  { codigo: 'ENTREGADO', nombre: 'Entregado', esTerminal: true, esProductivo: false, activo: true },
  { codigo: 'CANCELADO', nombre: 'Cancelado', esTerminal: true, esProductivo: false, activo: true },
];

const ORDEN = {
  id: 5,
  codigo: 'LG-0005',
  pacienteIdentificacion: 'M.P. - Caso #1002',
  tipoTrabajo: 'AAK',
  tipoOrden: 'Normal',
  estado: 'Recibido',
  descripcion: 'AAK superior',
  fechaIngreso: '2026-08-18',
  fechaEstimadaEntrega: '2026-08-27',
  precioBase: '250.00',
  recargoUrgencia: '0.00',
  precioTotal: '250.00',
  archivos: [
    { id: 3, nombreOriginal: 'radiografia.png', categoria: 'IMAGEN', tipoMime: 'image/png', tamanoBytes: 1024, fechaCarga: '2026-08-18T10:00:00-03:00' },
  ],
  lineaTiempo: [
    { estado: 'Recibido', fechaHora: '2026-08-18T10:00:00-03:00', autor: null },
    { estado: 'En evaluacion', fechaHora: '2026-08-19T09:30:00-03:00', autor: 'Mona' },
  ],
};

function renderizar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/ordenes/5']}>
        <Routes>
          <Route path="/ordenes/:id" element={<OrdenDetalle />} />
          <Route path="/ordenes" element={<p>Mis trabajos</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('OrdenDetalle', () => {
  beforeEach(() => {
    obtenerOrden.mockReset().mockResolvedValue(ORDEN);
    listarEstados.mockReset().mockResolvedValue(ESTADOS);
    cancelarOrden.mockReset().mockResolvedValue({ ...ORDEN, estado: 'Cancelado' });
    descargarArchivo.mockReset().mockResolvedValue(new Blob(['contenido']));
  });

  /** §5.4 criterio 1: cada etapa con su fecha y hora. */
  it('muestra la línea de tiempo fechada, con el autor de cada etapa', async () => {
    renderizar();

    expect(await screen.findByText('Seguimiento')).toBeInTheDocument();
    expect(screen.getByText('En evaluacion')).toBeInTheDocument();
    expect(screen.getByText('Mona')).toBeInTheDocument();
    // §5.1 paso 9: el registro inicial lo asigna el sistema y viene sin autor.
    expect(screen.getByText('Sistema')).toBeInTheDocument();
    // El formato exacto lo pone Intl; lo que fija el criterio 1 de §5.4 es que haya fecha y hora.
    expect(screen.getByText(/19\/8\/26.*9:30/)).toBeInTheDocument();
  });

  /** RN-22: iniciales y código, nunca el nombre. El backend ni siquiera lo manda. */
  it('identifica al paciente por iniciales y código', async () => {
    renderizar();

    expect(await screen.findByText('M.P. - Caso #1002')).toBeInTheDocument();
  });

  /** D-11: la mensajería está pospuesta; la pantalla no la insinúa. */
  it('no tiene sección de mensajes', async () => {
    renderizar();
    await screen.findByText('Seguimiento');

    expect(screen.queryByText(/mensaje/i)).not.toBeInTheDocument();
  });

  /** RN-01: la orden ajena y la inexistente se cuentan igual, para no revelar que existe. */
  it('una orden ajena o inexistente se muestra como no encontrada', async () => {
    obtenerOrden.mockRejectedValue(new ApiError(404, 'ORDEN_NO_ENCONTRADA', 'No existe la orden solicitada.'));
    renderizar();

    expect(await screen.findByText('No encontramos ese trabajo.')).toBeInTheDocument();
    expect(screen.queryByText(/permiso|prohibido/i)).not.toBeInTheDocument();
  });

  /** CU-20/§5.6: cancelar es irreversible, así que se pide confirmación antes de llamar. */
  it('cancelar pide confirmación antes de llamar al backend', async () => {
    renderizar();
    await screen.findByText('Seguimiento');

    await userEvent.click(screen.getByRole('button', { name: 'Cancelar el trabajo' }));
    expect(cancelarOrden).not.toHaveBeenCalled();

    await userEvent.click(screen.getByRole('button', { name: 'Confirmar cancelación' }));
    expect(cancelarOrden).toHaveBeenCalledTimes(1);
  });

  /** §5.6: desde un estado terminal no hay cancelación posible. */
  it('no ofrece cancelar cuando la orden está en un estado terminal', async () => {
    obtenerOrden.mockResolvedValue({ ...ORDEN, estado: 'Entregado' });
    renderizar();
    await screen.findByText('Seguimiento');

    expect(screen.queryByRole('button', { name: 'Cancelar el trabajo' })).not.toBeInTheDocument();
  });

  /** P-14: la cancelación no se restringe por etapa; una orden en curso se puede cancelar. */
  it('ofrece cancelar en cualquier estado no terminal', async () => {
    obtenerOrden.mockResolvedValue({ ...ORDEN, estado: 'En produccion' });
    renderizar();
    await screen.findByText('Seguimiento');

    expect(screen.getByRole('button', { name: 'Cancelar el trabajo' })).toBeInTheDocument();
  });

  it('muestra el motivo cuando el backend rechaza la cancelación', async () => {
    cancelarOrden.mockRejectedValue(
      new ApiError(409, 'ORDEN_NO_CANCELABLE', 'La orden ya no se puede cancelar.'),
    );
    renderizar();
    await screen.findByText('Seguimiento');

    await userEvent.click(screen.getByRole('button', { name: 'Cancelar el trabajo' }));
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar cancelación' }));

    expect(await screen.findByText('La orden ya no se puede cancelar.')).toBeInTheDocument();
  });

  /** RN-13: los adjuntos se listan y se descargan; subirlos y borrarlos es del laboratorio. */
  it('lista los adjuntos y los descarga por el endpoint autenticado', async () => {
    const crearUrl = vi.fn().mockReturnValue('blob:archivo');
    URL.createObjectURL = crearUrl;
    URL.revokeObjectURL = vi.fn();
    renderizar();
    await screen.findByText('radiografia.png');

    await userEvent.click(screen.getByRole('button', { name: 'Descargar' }));

    expect(descargarArchivo).toHaveBeenCalledWith(3);
    expect(crearUrl).toHaveBeenCalled();
    expect(screen.queryByRole('button', { name: /subir|eliminar/i })).not.toBeInTheDocument();
  });
});
