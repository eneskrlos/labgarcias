import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import Verificar from '../Verificar';
import { reenviarVerificacion, verificar } from '../api';
import { ApiError } from '../../../shared/api/cliente';

vi.mock('../api', () => ({
  verificar: vi.fn(),
  reenviarVerificacion: vi.fn(),
}));

function renderizarVerificar(ruta) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[ruta]}>
        <Routes>
          <Route path="/verificar" element={<Verificar />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('Verificar', () => {
  beforeEach(() => {
    verificar.mockReset();
    reenviarVerificacion.mockReset();
  });

  it('sin token en la URL muestra el mensaje de enlace faltante', () => {
    renderizarVerificar('/verificar');

    expect(screen.getByText(/falta el enlace de verificación/i)).toBeInTheDocument();
    expect(verificar).not.toHaveBeenCalled();
  });

  it('con un token válido consume el endpoint y muestra el mensaje de éxito', async () => {
    verificar.mockResolvedValue({ mensaje: 'Cuenta verificada correctamente. Ya podés iniciar sesión.' });

    renderizarVerificar('/verificar?token=abc123');

    expect(verificar.mock.calls[0][0]).toBe('abc123');
    expect(await screen.findByText('Cuenta verificada correctamente. Ya podés iniciar sesión.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Iniciar sesión' })).toBeInTheDocument();
  });

  it('con un token inválido muestra el error y el formulario de reenvío, no el link a login', async () => {
    verificar.mockRejectedValue(new ApiError(400, 'TOKEN_INVALIDO', 'El enlace de verificación no es válido, venció o ya fue usado.'));

    renderizarVerificar('/verificar?token=vencido');

    expect(await screen.findByText('El enlace de verificación no es válido, venció o ya fue usado.')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Iniciar sesión' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Reenviar enlace' })).toBeInTheDocument();
  });

  it('el reenvío muestra el mensaje genérico del backend', async () => {
    reenviarVerificacion.mockResolvedValue({ mensaje: 'Si el correo está registrado, vas a recibir un nuevo enlace de verificación.' });
    const usuarioEvento = userEvent.setup();

    renderizarVerificar('/verificar');
    await usuarioEvento.type(screen.getByLabelText('Correo'), 'juan@mail.com');
    await usuarioEvento.click(screen.getByRole('button', { name: 'Reenviar enlace' }));

    expect(reenviarVerificacion.mock.calls[0][0]).toBe('juan@mail.com');
    expect(await screen.findByText('Si el correo está registrado, vas a recibir un nuevo enlace de verificación.')).toBeInTheDocument();
  });
});
