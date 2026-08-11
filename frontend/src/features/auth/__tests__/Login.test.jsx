import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import Login from '../Login';
import { SesionProvider } from '../../../shared/hooks/useSesion';
import { login, loginGoogle } from '../api';
import { iniciarGoogle, renderizarBotonGoogle } from '../../../shared/api/googleIdentity';
import { ApiError } from '../../../shared/api/cliente';

vi.mock('../api', () => ({
  login: vi.fn(),
  loginGoogle: vi.fn(),
}));

vi.mock('../../../shared/api/googleIdentity', () => ({
  iniciarGoogle: vi.fn().mockResolvedValue(undefined),
  renderizarBotonGoogle: vi.fn(),
}));

class ResizeObserverSimulado {
  observe() {}
  disconnect() {}
}

function renderizarLogin() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <SesionProvider>
        <MemoryRouter initialEntries={['/login']}>
          <Routes>
            <Route path="/login" element={<Login />} />
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
    vi.stubGlobal('ResizeObserver', ResizeObserverSimulado);
    vi.stubEnv('VITE_GOOGLE_CLIENT_ID', 'client-id-de-prueba');
    login.mockReset();
    loginGoogle.mockReset();
    iniciarGoogle.mockClear();
    renderizarBotonGoogle.mockClear();
  });

  it('renderiza el formulario de login', () => {
    renderizarLogin();

    expect(screen.getByLabelText('Correo')).toBeInTheDocument();
    expect(screen.getByLabelText('Contraseña')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ingresar' })).toBeInTheDocument();
  });

  it('inicializa el botón de Google con el clientId configurado', async () => {
    renderizarLogin();

    await waitFor(() => expect(iniciarGoogle).toHaveBeenCalledTimes(1));
    expect(iniciarGoogle.mock.calls[0][0].clientId).toBe('client-id-de-prueba');
  });

  it('la credencial que devuelve Google dispara loginGoogle y autentica', async () => {
    const usuario = { id: 5, nombreCompleto: 'Ana', rol: 'ADMIN' };
    loginGoogle.mockResolvedValue({ token: 'jwt-google', usuario });

    renderizarLogin();
    await waitFor(() => expect(iniciarGoogle).toHaveBeenCalledTimes(1));
    const { alObtenerCredencial } = iniciarGoogle.mock.calls[0][0];
    alObtenerCredencial('credencial-de-google');

    await waitFor(() => expect(loginGoogle).toHaveBeenCalledTimes(1));
    expect(loginGoogle.mock.calls[0][0]).toBe('credencial-de-google');
    await waitFor(() => expect(screen.getByText('Pantalla de inicio autenticada')).toBeInTheDocument());
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

  it('cuenta no verificada muestra el mensaje correspondiente', async () => {
    login.mockRejectedValue(new ApiError(403, 'CUENTA_NO_VERIFICADA', 'Todavía no verificaste tu cuenta por correo.'));
    const usuarioEvento = userEvent.setup();

    renderizarLogin();
    await usuarioEvento.type(screen.getByLabelText('Correo'), 'juan@mail.com');
    await usuarioEvento.type(screen.getByLabelText('Contraseña'), 'x');
    await usuarioEvento.click(screen.getByRole('button', { name: 'Ingresar' }));

    expect(await screen.findByText('Todavía no verificaste tu cuenta por correo.')).toBeInTheDocument();
  });
});
