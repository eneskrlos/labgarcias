import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import Registro from '../Registro';
import { registrar } from '../api';
import { ApiError } from '../../../shared/api/cliente';

vi.mock('../api', () => ({
  registrar: vi.fn(),
}));

function renderizarRegistro() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <Registro />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

async function completarFormulario(usuarioEvento, overrides = {}) {
  const datos = {
    nombreCompleto: 'Dr. Juan Pérez',
    correo: 'juan@mail.com',
    nombreUsuario: 'jperez',
    password: '*38Op5)l6',
    direccion: 'Calle Falsa 123',
    ...overrides,
  };
  await usuarioEvento.type(screen.getByLabelText('Nombre completo'), datos.nombreCompleto);
  await usuarioEvento.type(screen.getByLabelText('Correo'), datos.correo);
  await usuarioEvento.type(screen.getByLabelText('Nombre de usuario'), datos.nombreUsuario);
  await usuarioEvento.type(screen.getByLabelText('Contraseña'), datos.password);
  await usuarioEvento.type(screen.getByLabelText('Dirección'), datos.direccion);
  await usuarioEvento.click(screen.getByRole('button', { name: 'Crear cuenta' }));
  return datos;
}

describe('Registro', () => {
  beforeEach(() => {
    registrar.mockReset();
  });

  it('muestra los requisitos de contraseña de RN-15', () => {
    renderizarRegistro();

    expect(screen.getByText(/mínimo 9 caracteres/i)).toBeInTheDocument();
    expect(screen.getByText(/mayúscula/i)).toBeInTheDocument();
    expect(screen.getByText(/carácter especial/i)).toBeInTheDocument();
  });

  it('envía los 5 campos del formulario a registrar()', async () => {
    registrar.mockResolvedValue({ mensaje: 'Cuenta creada. Revisá tu correo para confirmarla.' });
    const usuarioEvento = userEvent.setup();

    renderizarRegistro();
    const datos = await completarFormulario(usuarioEvento);

    expect(registrar.mock.calls[0][0]).toEqual(datos);
  });

  it('en éxito muestra el mensaje del backend y el link a /verificar', async () => {
    registrar.mockResolvedValue({ mensaje: 'Cuenta creada. Revisá tu correo para confirmarla.' });
    const usuarioEvento = userEvent.setup();

    renderizarRegistro();
    await completarFormulario(usuarioEvento);

    expect(await screen.findByText('Cuenta creada. Revisá tu correo para confirmarla.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /ya tengo un enlace/i })).toBeInTheDocument();
  });

  it('password inválida (RN-15) muestra el error del backend', async () => {
    registrar.mockRejectedValue(new ApiError(400, 'PASSWORD_INVALIDA', 'La contraseña no cumple los requisitos.', 'password'));
    const usuarioEvento = userEvent.setup();

    renderizarRegistro();
    await completarFormulario(usuarioEvento, { password: 'corta' });

    expect(await screen.findByText('La contraseña no cumple los requisitos.')).toBeInTheDocument();
  });

  it('correo ya registrado muestra el error del backend', async () => {
    registrar.mockRejectedValue(new ApiError(409, 'CORREO_YA_REGISTRADO', 'Ya existe una cuenta con ese correo.', 'correo'));
    const usuarioEvento = userEvent.setup();

    renderizarRegistro();
    await completarFormulario(usuarioEvento);

    expect(await screen.findByText('Ya existe una cuenta con ese correo.')).toBeInTheDocument();
  });
});
