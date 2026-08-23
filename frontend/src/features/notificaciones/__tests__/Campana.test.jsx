import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import Campana from '../Campana';
import { contarNoLeidas, listarPaginado, marcarLeida, marcarTodasLeidas } from '../api';

vi.mock('../api', () => ({
  contarNoLeidas: vi.fn(),
  listarPaginado: vi.fn(),
  marcarLeida: vi.fn(),
  marcarTodasLeidas: vi.fn(),
}));

const PAGINA_VACIA = { contenido: [], total: 0, pagina: 0, tamano: 10, totalPaginas: 0 };

function renderizar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <Campana />
    </QueryClientProvider>,
  );
}

describe('Campana', () => {
  beforeEach(() => {
    contarNoLeidas.mockReset().mockResolvedValue({ noLeidas: 3 });
    listarPaginado.mockReset().mockResolvedValue(PAGINA_VACIA);
    marcarLeida.mockReset();
    marcarTodasLeidas.mockReset();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('muestra la cantidad de notificaciones sin leer', async () => {
    renderizar();

    expect(await screen.findByText('3')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Notificaciones: 3 sin leer' })).toBeInTheDocument();
  });

  it('sin notificaciones pendientes no muestra el globo del contador', async () => {
    contarNoLeidas.mockResolvedValue({ noLeidas: 0 });
    renderizar();

    expect(
      await screen.findByRole('button', { name: 'Notificaciones: ninguna sin leer' }),
    ).toBeInTheDocument();
    expect(screen.queryByText('0')).not.toBeInTheDocument();
  });

  it('refresca el contador por polling cada 60 s, no antes (§6.4)', async () => {
    // shouldAdvanceTime: el reloj falso avanza solo, si no las esperas de Testing Library
    // nunca se resuelven.
    vi.useFakeTimers({ shouldAdvanceTime: true });
    renderizar();
    await screen.findByText('3');
    expect(contarNoLeidas).toHaveBeenCalledTimes(1);

    contarNoLeidas.mockResolvedValue({ noLeidas: 5 });
    await act(async () => {
      await vi.advanceTimersByTimeAsync(59_000);
    });
    expect(contarNoLeidas).toHaveBeenCalledTimes(1);

    await act(async () => {
      await vi.advanceTimersByTimeAsync(1_000);
    });

    expect(contarNoLeidas).toHaveBeenCalledTimes(2);
    expect(await screen.findByText('5')).toBeInTheDocument();
  });

  it('el panel está cerrado hasta que se toca la campana, y se cierra desde el panel', async () => {
    const usuarioEvento = userEvent.setup();
    renderizar();
    const boton = await screen.findByRole('button', { name: 'Notificaciones: 3 sin leer' });

    expect(screen.queryByRole('dialog', { name: 'Notificaciones' })).not.toBeInTheDocument();

    await usuarioEvento.click(boton);
    expect(await screen.findByRole('dialog', { name: 'Notificaciones' })).toBeInTheDocument();

    await usuarioEvento.click(screen.getByRole('button', { name: 'Cerrar notificaciones' }));
    expect(screen.queryByRole('dialog', { name: 'Notificaciones' })).not.toBeInTheDocument();
  });
});
