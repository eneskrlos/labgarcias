import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import OrdenFormulario from '../OrdenFormulario';
import { crearOrden } from '../api';
import { listarOdontologosActivos } from '../../auth/api';
import { listarActivos, listarTiposOrden } from '../../catalogos/api';
import { ApiError } from '../../../shared/api/cliente';

vi.mock('../api', () => ({ crearOrden: vi.fn() }));
vi.mock('../../auth/api', () => ({ listarOdontologosActivos: vi.fn() }));
vi.mock('../../catalogos/api', () => ({ listarActivos: vi.fn(), listarTiposOrden: vi.fn() }));

const ODONTOLOGOS = [
  { id: 3, nombreCompleto: 'Dr. Juan Pérez' },
  { id: 7, nombreCompleto: 'Dra. Ana Gómez' },
];

const TIPOS_TRABAJO = [
  { id: 16, nombre: 'DISYUNTOR CON TORNILLO ESTANDAR', diasEstimados: 7, precio: '250.00', activo: true },
];

const TIPOS_ORDEN = [
  { codigo: 'NORMAL', nombre: 'Normal', recargoMonto: '0.00' },
  { codigo: 'URGENTE', nombre: 'Urgente', recargoMonto: '200.00' },
];

const ORDEN_CREADA = {
  id: 1,
  codigo: 'LG-0007',
  pacienteIdentificacion: 'M.P. - Caso #1000',
  fechaEstimadaEntrega: '2026-09-01',
  precioTotal: '450.00',
};

/** Muestra a dónde navegó el formulario y con qué confirmación, sin depender de la pantalla real. */
function Destino() {
  const location = useLocation();
  return <p>Destino: {location.state?.mensaje ?? 'sin mensaje'}</p>;
}

function renderizar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/admin/ordenes/nueva']}>
        <Routes>
          <Route path="/admin/ordenes/nueva" element={<OrdenFormulario />} />
          {/* §8.1 Regla 1: al guardar y al cancelar se vuelve al listado, que existe desde T-26. */}
          <Route path="/admin/ordenes" element={<Destino />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

async function completarFormulario() {
  await userEvent.selectOptions(await screen.findByLabelText('Odontólogo'), '3');
  await userEvent.type(screen.getByLabelText('Nombre del paciente'), 'Martín Pérez');
  await userEvent.type(screen.getByLabelText('Fecha de ingreso'), '2026-08-06');
  await userEvent.selectOptions(screen.getByLabelText('Tipo de trabajo'), '16');
  await userEvent.selectOptions(screen.getByLabelText('Tipo de orden'), 'URGENTE');
}

describe('OrdenFormulario', () => {
  beforeEach(() => {
    listarOdontologosActivos.mockReset().mockResolvedValue(ODONTOLOGOS);
    listarActivos.mockReset().mockResolvedValue(TIPOS_TRABAJO);
    listarTiposOrden.mockReset().mockResolvedValue(TIPOS_ORDEN);
    crearOrden.mockReset().mockResolvedValue(ORDEN_CREADA);
  });

  /** §5.1/D-19: el selector de odontólogo es lo que distingue a esta pantalla de la del odontólogo. */
  it('ofrece los odontólogos activos en un selector', async () => {
    renderizar();

    const selector = await screen.findByLabelText('Odontólogo');
    expect(selector).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Dr. Juan Pérez' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Dra. Ana Gómez' })).toBeInTheDocument();
  });

  it('manda al backend exactamente lo que se completó', async () => {
    renderizar();
    await completarFormulario();

    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(crearOrden).toHaveBeenCalledWith({
      odontologoId: 3,
      pacienteNombre: 'Martín Pérez',
      fechaIngreso: '2026-08-06',
      tipoTrabajoId: 16,
      tipoOrdenCodigo: 'URGENTE',
      descripcion: null,
    });
  });

  /** §8.1 Regla 1: al guardar se vuelve al listado con confirmación. */
  it('al guardar vuelve al listado con la confirmación de la orden', async () => {
    renderizar();
    await completarFormulario();

    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));

    const confirmacion = await screen.findByText(/Destino:/);
    expect(confirmacion).toHaveTextContent('LG-0007');
    expect(confirmacion).toHaveTextContent('2026-09-01');
    expect(confirmacion).toHaveTextContent('450.00');
  });

  /** §5.1 criterio 4 y RN-22: el nombre del paciente no vuelve a aparecer después del alta. */
  it('la confirmación no muestra el nombre del paciente', async () => {
    renderizar();
    await completarFormulario();

    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));

    await screen.findByText(/Destino:/);
    expect(screen.queryByText(/Martín Pérez/)).not.toBeInTheDocument();
  });

  /** Agente.md 6.1: la pantalla no calcula ni anticipa precio, recargo ni fecha estimada. */
  it('no muestra precios ni fechas estimadas antes de que responda el backend', async () => {
    renderizar();
    await completarFormulario();

    expect(screen.queryByText(/450\.00|250\.00|200\.00/)).not.toBeInTheDocument();
    expect(screen.queryByText(/2026-09-01/)).not.toBeInTheDocument();
  });

  /** §8.1 Regla 1: cancelar vuelve sin guardar. */
  it('cancelar vuelve sin llamar al backend', async () => {
    renderizar();
    await screen.findByLabelText('Odontólogo');

    await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(await screen.findByText('Destino: sin mensaje')).toBeInTheDocument();
    expect(crearOrden).not.toHaveBeenCalled();
  });

  it('muestra el error del campo que rechazó el backend', async () => {
    crearOrden.mockRejectedValue(
      new ApiError(422, 'ODONTOLOGO_INVALIDO', 'El odontólogo indicado no existe o no está activo.', 'odontologoId'),
    );
    renderizar();
    await completarFormulario();

    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(
      await screen.findByText('El odontólogo indicado no existe o no está activo.'),
    ).toBeInTheDocument();
  });

  it('si no puede cargar los catálogos ofrece reintentar', async () => {
    listarOdontologosActivos.mockRejectedValue(new ApiError(500, 'ERROR', 'Ocurrió un error inesperado.'));
    renderizar();

    expect(await screen.findByText('No pudimos cargar los datos del formulario.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
