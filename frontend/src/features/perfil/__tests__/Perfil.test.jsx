import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import Perfil from '../Perfil';
import { conectarTelegram, desvincularTelegram, obtenerPerfil } from '../api';
import { ApiError } from '../../../shared/api/cliente';

vi.mock('../api', () => ({
  obtenerPerfil: vi.fn(),
  conectarTelegram: vi.fn(),
  desvincularTelegram: vi.fn(),
}));

const PERFIL_SIN_VINCULAR = {
  id: 12,
  nombreCompleto: 'Dr. Juan Pérez',
  correo: 'juan@mail.com',
  nombreUsuario: 'jperez',
  direccion: 'Av. 18 de Julio 1234',
  telefono: '+59891234567',
  rol: 'ODONTOLOGO',
  telegramVinculado: false,
};

const PERFIL_VINCULADO = { ...PERFIL_SIN_VINCULAR, telegramVinculado: true };

const ENLACE = 'https://t.me/labgarcias_bot?start=abc123';

function renderizar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <Perfil />
    </QueryClientProvider>,
  );
}

describe('Perfil', () => {
  beforeEach(() => {
    obtenerPerfil.mockReset().mockResolvedValue(PERFIL_SIN_VINCULAR);
    conectarTelegram.mockReset().mockResolvedValue({ enlace: ENLACE });
    desvincularTelegram.mockReset().mockResolvedValue(null);
  });

  it('muestra los datos propios del usuario', async () => {
    renderizar();

    expect(await screen.findByText('Dr. Juan Pérez')).toBeInTheDocument();
    expect(screen.getByText('juan@mail.com')).toBeInTheDocument();
    expect(screen.getByText('jperez')).toBeInTheDocument();
  });

  /** §6.5 paso 1: el perfil muestra el estado y ofrece conectar. */
  it('sin vincular muestra el estado y el botón de conectar', async () => {
    renderizar();

    expect(await screen.findByText('Telegram: no vinculado')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Conectar Telegram' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Desvincular' })).not.toBeInTheDocument();
  });

  /** §6.5 paso 2: conectar devuelve el enlace profundo, y el usuario lo abre. */
  it('conectar muestra el enlace al bot', async () => {
    renderizar();
    await screen.findByText('Telegram: no vinculado');

    await userEvent.click(screen.getByRole('button', { name: 'Conectar Telegram' }));

    const enlace = await screen.findByRole('link', { name: 'Abrir el bot de Telegram' });
    expect(enlace).toHaveAttribute('href', ENLACE);
    expect(conectarTelegram).toHaveBeenCalledTimes(1);
  });

  /** §6.5 paso 5: vinculado, el perfil lo dice y ofrece desvincular. */
  it('vinculado muestra el estado y el botón de desvincular', async () => {
    obtenerPerfil.mockResolvedValue(PERFIL_VINCULADO);
    renderizar();

    expect(await screen.findByText('Telegram: vinculado ✅')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Conectar Telegram' })).not.toBeInTheDocument();
  });

  it('desvincular llama al endpoint y refresca el perfil', async () => {
    obtenerPerfil.mockResolvedValue(PERFIL_VINCULADO);
    renderizar();
    await screen.findByText('Telegram: vinculado ✅');
    obtenerPerfil.mockResolvedValue(PERFIL_SIN_VINCULAR);

    await userEvent.click(screen.getByRole('button', { name: 'Desvincular' }));

    expect(await screen.findByText('Telegram: no vinculado')).toBeInTheDocument();
    expect(desvincularTelegram).toHaveBeenCalledTimes(1);
  });

  /** §6.5: sin bot configurado (P-20) el backend rechaza, y el motivo tiene que verse. */
  it('muestra el motivo cuando la vinculación no está configurada', async () => {
    conectarTelegram.mockRejectedValue(
      new ApiError(422, 'TELEGRAM_NO_CONFIGURADO', 'La vinculación con Telegram no está disponible.'),
    );
    renderizar();
    await screen.findByText('Telegram: no vinculado');

    await userEvent.click(screen.getByRole('button', { name: 'Conectar Telegram' }));

    expect(
      await screen.findByText('La vinculación con Telegram no está disponible.'),
    ).toBeInTheDocument();
  });

  it('un error al traer el perfil ofrece reintentar', async () => {
    obtenerPerfil.mockRejectedValue(new ApiError(500, 'ERROR', 'Ocurrió un error inesperado.'));
    renderizar();

    expect(await screen.findByText('No pudimos traer tu perfil.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
