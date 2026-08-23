import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import Login from '../Login';
import { SesionProvider } from '../../../shared/hooks/useSesion';
import { login } from '../api';
import { ApiError } from '../../../shared/api/cliente';

vi.mock('../api', () => ({
  login: vi.fn(),
}));

function renderizarLogin() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <SesionProvider>
        <MemoryRouter initialEntries={['/login']}>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/solicitar-acceso" element={<div>Pantalla de solicitud de acceso</div>} />
            <Route path="/cambiar-password" element={<div>Pantalla de cambio de contraseña</div>} />
            <Route path="/" element={<div>Pantalla de inicio autenticada</div>} />
          </Routes>
        </MemoryRouter>
      </SesionProvider>
    </QueryClientProvider>,
  );
}

describe('Login', () => {
  beforeEach(() => {
    localStorage.clear();
    login.mockReset();
  });

  it('renderiza el formulario de login', () => {
    renderizarLogin();

    expect(screen.getByLabelText('Correo')).toBeInTheDocument();
    expect(screen.getByLabelText('Contraseña')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ingresar' })).toBeInTheDocument();
  });

  // CR-01 (D-17): Google se retiró; el botón "Solicitar acceso" llega en T-30.
  it('no ofrece acceso con Google ni enlace de auto-registro', () => {
    renderizarLogin();

    expect(screen.queryByText(/google/i)).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /registrate/i })).not.toBeInTheDocument();
  });

  it('login exitoso guarda la sesión y navega a "/"', async () => {
    const usuario = { id: 1, nombreCompleto: 'Dr. Juan Pérez', rol: 'ODONTOLOGO' };
    login.mockResolvedValue({ token: 'jwt-123', usuario });
    const usuarioEvento = userEvent.setup();

    renderizarLogin();
    await usuarioEvento.type(screen.getByLabelText('Correo'), 'juan@mail.com');
    await usuarioEvento.type(screen.getByLabelText('Contraseña'), '*38Op5)l6');
    await usuarioEvento.click(screen.getByRole('button', { name: 'Ingresar' }));

    expect(login.mock.calls[0][0]).toEqual({ correo: 'juan@mail.com', password: '*38Op5)l6' });
    await waitFor(() => expect(screen.getByText('Pantalla de inicio autenticada')).toBeInTheDocument());
  });

  it('credenciales inválidas muestran el mensaje de error del backend', async () => {
    login.mockRejectedValue(new ApiError(401, 'CREDENCIALES_INVALIDAS', 'Correo o contraseña incorrectos.'));
    const usuarioEvento = userEvent.setup();

    renderizarLogin();
    await usuarioEvento.type(screen.getByLabelText('Correo'), 'juan@mail.com');
    await usuarioEvento.type(screen.getByLabelText('Contraseña'), 'incorrecta');
    await usuarioEvento.click(screen.getByRole('button', { name: 'Ingresar' }));

    expect(await screen.findByText('Correo o contraseña incorrectos.')).toBeInTheDocument();
  });

  it('cuenta inactiva muestra el mensaje correspondiente', async () => {
    login.mockRejectedValue(new ApiError(403, 'CUENTA_INACTIVA', 'Tu cuenta está inactiva. Contactá al laboratorio.'));
    const usuarioEvento = userEvent.setup();

    renderizarLogin();
    await usuarioEvento.type(screen.getByLabelText('Correo'), 'juan@mail.com');
    await usuarioEvento.type(screen.getByLabelText('Contraseña'), 'x');
    await usuarioEvento.click(screen.getByRole('button', { name: 'Ingresar' }));

    expect(await screen.findByText('Tu cuenta está inactiva. Contactá al laboratorio.')).toBeInTheDocument();
  });

  /** §3.1.b: con el cambio pendiente, el login no lleva al inicio sino al cambio de contraseña. */
  it('un login con debeCambiarPassword lleva a /cambiar-password', async () => {
    login.mockResolvedValue({
      token: 'token-restringido',
      debeCambiarPassword: true,
      usuario: { id: 7, nombreCompleto: 'Dr. Juan Pérez', rol: 'ODONTOLOGO' },
    });
    const usuarioEvento = userEvent.setup();

    renderizarLogin();
    await usuarioEvento.type(screen.getByLabelText('Correo'), 'juan@mail.com');
    await usuarioEvento.type(screen.getByLabelText('Contraseña'), 'Ab3$Kd9!Xz2P');
    await usuarioEvento.click(screen.getByRole('button', { name: 'Ingresar' }));

    expect(await screen.findByText('Pantalla de cambio de contraseña')).toBeInTheDocument();
    expect(JSON.parse(localStorage.getItem('labgarcias_usuario')).debeCambiarPassword).toBe(true);
  });

  /** D-17: el botón "Solicitar acceso" del mockup reemplaza al registro y a Google. */
  it('el botón "Solicitar acceso" lleva al formulario público', async () => {
    const usuarioEvento = userEvent.setup();
    renderizarLogin();

    await usuarioEvento.click(screen.getByRole('link', { name: 'Solicitar acceso' }));

    expect(await screen.findByText('Pantalla de solicitud de acceso')).toBeInTheDocument();
  });
});
