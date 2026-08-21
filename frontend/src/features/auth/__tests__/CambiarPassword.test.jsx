import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import CambiarPassword from '../CambiarPassword';
import { SesionProvider } from '../../../shared/hooks/useSesion';
import { cambiarPassword } from '../api';
import { ApiError } from '../../../shared/api/cliente';
import { guardarSesion, obtenerToken, obtenerUsuario } from '../../../shared/api/token';

vi.mock('../api', () => ({
  cambiarPassword: vi.fn(),
}));

const USUARIO_CON_CAMBIO_PENDIENTE = {
  id: 7,
  nombreCompleto: 'Dr. Juan Pérez',
  rol: 'ODONTOLOGO',
  debeCambiarPassword: true,
};

function renderizar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <SesionProvider>
        <MemoryRouter initialEntries={['/cambiar-password']}>
          <Routes>
            <Route path="/cambiar-password" element={<CambiarPassword />} />
            <Route path="/" element={<div>Pantalla de inicio</div>} />
            <Route path="/login" element={<div>Pantalla de login</div>} />
          </Routes>
        </MemoryRouter>
      </SesionProvider>
    </QueryClientProvider>,
  );
}

async function completar(usuarioEvento, actual = 'Ab3$Kd9!Xz2P', nueva = 'MiClave2026$') {
  await usuarioEvento.type(screen.getByLabelText('Contraseña temporal'), actual);
  await usuarioEvento.type(screen.getByLabelText('Contraseña nueva'), nueva);
  await usuarioEvento.click(screen.getByRole('button', { name: 'Cambiar contraseña' }));
}

describe('CambiarPassword', () => {
  beforeEach(() => {
    localStorage.clear();
    guardarSesion('token-restringido', USUARIO_CON_CAMBIO_PENDIENTE);
    cambiarPassword.mockReset().mockResolvedValue({
      token: 'token-normal',
      debeCambiarPassword: false,
      usuario: { id: 7, nombreCompleto: 'Dr. Juan Pérez', rol: 'ODONTOLOGO' },
    });
  });

  it('envía la contraseña actual y la nueva', async () => {
    const usuarioEvento = userEvent.setup();
    renderizar();

    await completar(usuarioEvento);

    expect(cambiarPassword).toHaveBeenCalledWith({
      passwordActual: 'Ab3$Kd9!Xz2P',
      passwordNueva: 'MiClave2026$',
    });
  });

  it('muestra la regla RN-15 como ayuda del campo', () => {
    renderizar();

    expect(screen.getByText(/mínimo 9 caracteres/i)).toBeInTheDocument();
  });

  /** §3.1.b: el token nuevo reemplaza al restringido y la sesión deja de exigir el cambio. */
  it('al cambiarla, guarda el token nuevo y apaga la bandera de la sesión', async () => {
    const usuarioEvento = userEvent.setup();
    renderizar();

    await completar(usuarioEvento);

    expect(await screen.findByText('Pantalla de inicio')).toBeInTheDocument();
    expect(obtenerToken()).toBe('token-normal');
    expect(obtenerUsuario().debeCambiarPassword).toBe(false);
  });

  it('una contraseña que no cumple RN-15 muestra el error sobre el campo', async () => {
    cambiarPassword.mockRejectedValue(
      new ApiError(400, 'PASSWORD_INVALIDA', 'La contraseña debe tener al menos 9 caracteres.', 'passwordNueva'),
    );
    const usuarioEvento = userEvent.setup();
    renderizar();

    await completar(usuarioEvento, 'Ab3$Kd9!Xz2P', 'corta');

    expect(await screen.findByText('La contraseña debe tener al menos 9 caracteres.')).toBeInTheDocument();
    expect(obtenerToken()).toBe('token-restringido');
  });

  it('una contraseña temporal incorrecta muestra el error y no cambia la sesión', async () => {
    cambiarPassword.mockRejectedValue(
      new ApiError(422, 'PASSWORD_ACTUAL_INCORRECTA', 'La contraseña actual no es correcta.', 'passwordActual'),
    );
    const usuarioEvento = userEvent.setup();
    renderizar();

    await completar(usuarioEvento, 'otra', 'MiClave2026$');

    expect(await screen.findByText('La contraseña actual no es correcta.')).toBeInTheDocument();
    expect(obtenerUsuario().debeCambiarPassword).toBe(true);
  });

  /** No hay "Cancelar": con el cambio pendiente, ninguna otra pantalla responde. */
  it('no ofrece cancelar, solo cerrar sesión', async () => {
    const usuarioEvento = userEvent.setup();
    renderizar();

    expect(screen.queryByRole('button', { name: 'Cancelar' })).not.toBeInTheDocument();

    await usuarioEvento.click(screen.getByRole('button', { name: 'Cerrar sesión' }));

    expect(await screen.findByText('Pantalla de login')).toBeInTheDocument();
    expect(obtenerToken()).toBeNull();
  });
});
