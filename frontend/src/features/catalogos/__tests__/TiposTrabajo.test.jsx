import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import TiposTrabajo from '../TiposTrabajo';
import { actualizar, cambiarEstado, crear, listarTodos } from '../api';
import { ApiError } from '../../../shared/api/cliente';

vi.mock('../api', () => ({
  listarTodos: vi.fn(),
  crear: vi.fn(),
  actualizar: vi.fn(),
  cambiarEstado: vi.fn(),
}));

const TIPOS_INICIALES = [
  { id: 1, nombre: 'PLACA ACTIVA', diasEstimados: 7, precio: 250, activo: true },
  { id: 2, nombre: 'VIEJO TIPO', diasEstimados: 10, precio: 300, activo: false },
];

function renderizarPantalla() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <TiposTrabajo />
    </QueryClientProvider>,
  );
}

describe('TiposTrabajo', () => {
  beforeEach(() => {
    listarTodos.mockReset().mockResolvedValue(TIPOS_INICIALES);
    crear.mockReset();
    actualizar.mockReset();
    cambiarEstado.mockReset();
  });

  it('lista todos los tipos, incluidos los inactivos', async () => {
    renderizarPantalla();

    expect(await screen.findByText('PLACA ACTIVA')).toBeInTheDocument();
    expect(screen.getByText('VIEJO TIPO')).toBeInTheDocument();
    expect(screen.getAllByText('Activo')).toHaveLength(1);
    expect(screen.getAllByText('Inactivo')).toHaveLength(1);
  });

  it('crear envía los valores numéricos convertidos', async () => {
    crear.mockResolvedValue({ id: 3, nombre: 'NUEVO', diasEstimados: 7, precio: 250, activo: true });
    const usuarioEvento = userEvent.setup();

    renderizarPantalla();
    await screen.findByText('PLACA ACTIVA');

    await usuarioEvento.type(screen.getByLabelText('Nombre'), 'NUEVO');
    await usuarioEvento.type(screen.getByLabelText('Días estimados'), '7');
    await usuarioEvento.type(screen.getByLabelText('Precio'), '250');
    await usuarioEvento.click(screen.getByRole('button', { name: 'Crear' }));

    expect(crear.mock.calls[0][0]).toEqual({ nombre: 'NUEVO', diasEstimados: 7, precio: 250 });
  });

  it('RN-12: días insuficientes muestra el mensaje del backend', async () => {
    crear.mockRejectedValue(new ApiError(422, 'DIAS_ESTIMADOS_INSUFICIENTES', 'Los días estimados deben ser al menos 7.', 'diasEstimados'));
    const usuarioEvento = userEvent.setup();

    renderizarPantalla();
    await screen.findByText('PLACA ACTIVA');
    await usuarioEvento.type(screen.getByLabelText('Nombre'), 'X');
    await usuarioEvento.type(screen.getByLabelText('Días estimados'), '6');
    await usuarioEvento.type(screen.getByLabelText('Precio'), '250');
    await usuarioEvento.click(screen.getByRole('button', { name: 'Crear' }));

    expect(await screen.findByText('Los días estimados deben ser al menos 7.')).toBeInTheDocument();
  });

  it('RN-21: precio insuficiente muestra el mensaje del backend', async () => {
    crear.mockRejectedValue(new ApiError(422, 'PRECIO_INSUFICIENTE', 'El precio debe ser al menos 250.', 'precio'));
    const usuarioEvento = userEvent.setup();

    renderizarPantalla();
    await screen.findByText('PLACA ACTIVA');
    await usuarioEvento.type(screen.getByLabelText('Nombre'), 'X');
    await usuarioEvento.type(screen.getByLabelText('Días estimados'), '7');
    await usuarioEvento.type(screen.getByLabelText('Precio'), '249');
    await usuarioEvento.click(screen.getByRole('button', { name: 'Crear' }));

    expect(await screen.findByText('El precio debe ser al menos 250.')).toBeInTheDocument();
  });

  it('editar precarga el formulario y actualizar envía el id correcto', async () => {
    actualizar.mockResolvedValue({ ...TIPOS_INICIALES[0], nombre: 'PLACA MODIFICADA' });
    const usuarioEvento = userEvent.setup();

    renderizarPantalla();
    const fila = (await screen.findByText('PLACA ACTIVA')).closest('tr');
    await usuarioEvento.click(within(fila).getByRole('button', { name: 'Editar' }));

    expect(screen.getByLabelText('Nombre')).toHaveValue('PLACA ACTIVA');
    expect(screen.getByRole('button', { name: 'Guardar cambios' })).toBeInTheDocument();

    await usuarioEvento.clear(screen.getByLabelText('Nombre'));
    await usuarioEvento.type(screen.getByLabelText('Nombre'), 'PLACA MODIFICADA');
    await usuarioEvento.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(actualizar).toHaveBeenCalledWith(1, { nombre: 'PLACA MODIFICADA', diasEstimados: 7, precio: 250 });
  });

  it('el botón activar/desactivar llama a cambiarEstado con el valor invertido', async () => {
    cambiarEstado.mockResolvedValue({ ...TIPOS_INICIALES[0], activo: false });
    const usuarioEvento = userEvent.setup();

    renderizarPantalla();
    const fila = (await screen.findByText('PLACA ACTIVA')).closest('tr');
    await usuarioEvento.click(within(fila).getByRole('button', { name: 'Desactivar' }));

    expect(cambiarEstado).toHaveBeenCalledWith(1, false);
  });
});
