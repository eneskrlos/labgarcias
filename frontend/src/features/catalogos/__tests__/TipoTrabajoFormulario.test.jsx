import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import TipoTrabajoFormulario from '../TipoTrabajoFormulario';
import { actualizar, crear, obtenerPorId } from '../api';
import { ApiError } from '../../../shared/api/cliente';

vi.mock('../api', () => ({
  crear: vi.fn(),
  actualizar: vi.fn(),
  obtenerPorId: vi.fn(),
}));

function renderizarPantalla(entrada) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[entrada]}>
        <Routes>
          <Route path="/admin/tipos-trabajo" element={<div>Pantalla listado</div>} />
          <Route path="/admin/tipos-trabajo/nuevo" element={<TipoTrabajoFormulario />} />
          <Route path="/admin/tipos-trabajo/:id/editar" element={<TipoTrabajoFormulario />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('TipoTrabajoFormulario — modo alta', () => {
  beforeEach(() => {
    crear.mockReset();
    actualizar.mockReset();
    obtenerPorId.mockReset();
  });

  it('muestra el título "Nuevo tipo de trabajo" con los campos vacíos', () => {
    renderizarPantalla('/admin/tipos-trabajo/nuevo');

    expect(screen.getByText('Nuevo tipo de trabajo')).toBeInTheDocument();
    expect(screen.getByLabelText('Nombre')).toHaveValue('');
  });

  it('muestra los textos de ayuda de RN-12 y RN-21 en el formulario', () => {
    renderizarPantalla('/admin/tipos-trabajo/nuevo');

    expect(screen.getByText('RN-12: mínimo 7 días hábiles.')).toBeInTheDocument();
    expect(screen.getByText('RN-21: mínimo 250.')).toBeInTheDocument();
  });

  it('crear envía los valores numéricos convertidos y vuelve al listado', async () => {
    crear.mockResolvedValue({ id: 3, nombre: 'NUEVO', diasEstimados: 7, precio: 250, activo: true });
    const usuarioEvento = userEvent.setup();

    renderizarPantalla('/admin/tipos-trabajo/nuevo');
    await usuarioEvento.type(screen.getByLabelText('Nombre'), 'NUEVO');
    await usuarioEvento.type(screen.getByLabelText('Días estimados'), '7');
    await usuarioEvento.type(screen.getByLabelText('Precio'), '250');
    await usuarioEvento.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(crear.mock.calls[0][0]).toEqual({ nombre: 'NUEVO', diasEstimados: 7, precio: 250 });
    expect(await screen.findByText('Pantalla listado')).toBeInTheDocument();
  });

  it('Cancelar vuelve al listado sin guardar', async () => {
    const usuarioEvento = userEvent.setup();
    renderizarPantalla('/admin/tipos-trabajo/nuevo');

    await usuarioEvento.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(await screen.findByText('Pantalla listado')).toBeInTheDocument();
    expect(crear).not.toHaveBeenCalled();
  });

  it('RN-12: días insuficientes muestra el mensaje del backend bajo el campo', async () => {
    crear.mockRejectedValue(
      new ApiError(422, 'DIAS_ESTIMADOS_INSUFICIENTES', 'Los días estimados deben ser al menos 7.', 'diasEstimados'),
    );
    const usuarioEvento = userEvent.setup();

    renderizarPantalla('/admin/tipos-trabajo/nuevo');
    await usuarioEvento.type(screen.getByLabelText('Nombre'), 'X');
    await usuarioEvento.type(screen.getByLabelText('Días estimados'), '6');
    await usuarioEvento.type(screen.getByLabelText('Precio'), '250');
    await usuarioEvento.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(await screen.findByText('Los días estimados deben ser al menos 7.')).toBeInTheDocument();
  });

  it('RN-21: precio insuficiente muestra el mensaje del backend bajo el campo', async () => {
    crear.mockRejectedValue(new ApiError(422, 'PRECIO_INSUFICIENTE', 'El precio debe ser al menos 250.', 'precio'));
    const usuarioEvento = userEvent.setup();

    renderizarPantalla('/admin/tipos-trabajo/nuevo');
    await usuarioEvento.type(screen.getByLabelText('Nombre'), 'X');
    await usuarioEvento.type(screen.getByLabelText('Días estimados'), '7');
    await usuarioEvento.type(screen.getByLabelText('Precio'), '249');
    await usuarioEvento.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(await screen.findByText('El precio debe ser al menos 250.')).toBeInTheDocument();
  });

  it('nombre duplicado sin campo específico se muestra como error general', async () => {
    crear.mockRejectedValue(new ApiError(409, 'TIPO_TRABAJO_DUPLICADO', 'Ya existe un tipo de trabajo con ese nombre.', 'nombre'));
    const usuarioEvento = userEvent.setup();

    renderizarPantalla('/admin/tipos-trabajo/nuevo');
    await usuarioEvento.type(screen.getByLabelText('Nombre'), 'PLACA ACTIVA');
    await usuarioEvento.type(screen.getByLabelText('Días estimados'), '7');
    await usuarioEvento.type(screen.getByLabelText('Precio'), '250');
    await usuarioEvento.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(await screen.findByText('Ya existe un tipo de trabajo con ese nombre.')).toBeInTheDocument();
  });
});

describe('TipoTrabajoFormulario — modo edición', () => {
  beforeEach(() => {
    crear.mockReset();
    actualizar.mockReset();
    obtenerPorId.mockReset();
  });

  it('precarga el formulario con los datos existentes', async () => {
    obtenerPorId.mockResolvedValue({ id: 1, nombre: 'PLACA ACTIVA', diasEstimados: 7, precio: 250, activo: true });

    renderizarPantalla('/admin/tipos-trabajo/1/editar');

    expect(await screen.findByDisplayValue('PLACA ACTIVA')).toBeInTheDocument();
    expect(screen.getByText('Editar tipo de trabajo')).toBeInTheDocument();
    expect(obtenerPorId).toHaveBeenCalledWith('1');
  });

  it('actualizar envía el id correcto y vuelve al listado', async () => {
    obtenerPorId.mockResolvedValue({ id: 1, nombre: 'PLACA ACTIVA', diasEstimados: 7, precio: 250, activo: true });
    actualizar.mockResolvedValue({ id: 1, nombre: 'PLACA MODIFICADA', diasEstimados: 7, precio: 250, activo: true });
    const usuarioEvento = userEvent.setup();

    renderizarPantalla('/admin/tipos-trabajo/1/editar');
    await screen.findByDisplayValue('PLACA ACTIVA');
    await usuarioEvento.clear(screen.getByLabelText('Nombre'));
    await usuarioEvento.type(screen.getByLabelText('Nombre'), 'PLACA MODIFICADA');
    await usuarioEvento.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(actualizar).toHaveBeenCalledWith('1', { nombre: 'PLACA MODIFICADA', diasEstimados: 7, precio: 250 });
    expect(await screen.findByText('Pantalla listado')).toBeInTheDocument();
  });

  it('id inexistente muestra el error 404 del backend', async () => {
    obtenerPorId.mockRejectedValue(new ApiError(404, 'TIPO_TRABAJO_NO_ENCONTRADO', 'No existe el tipo de trabajo solicitado.'));

    renderizarPantalla('/admin/tipos-trabajo/999/editar');

    expect(await screen.findByText('No existe el tipo de trabajo solicitado.')).toBeInTheDocument();
  });
});
