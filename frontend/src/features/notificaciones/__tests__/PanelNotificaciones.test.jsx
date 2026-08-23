import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { render, screen, within } from '@testing-library/react';
import { SesionProvider } from '../../../shared/hooks/useSesion';
import { guardarUsuario } from '../../../shared/api/token';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import PanelNotificaciones from '../PanelNotificaciones';
import { CLAVE_CONTADOR } from '../claves';
import { contarNoLeidas, listarPaginado, marcarLeida, marcarTodasLeidas } from '../api';
import { ApiError } from '../../../shared/api/cliente';

vi.mock('../api', () => ({
  contarNoLeidas: vi.fn(),
  listarPaginado: vi.fn(),
  marcarLeida: vi.fn(),
  marcarTodasLeidas: vi.fn(),
}));

const NOTIFICACION_SIN_LEER = {
  id: 12,
  tipoEvento: 'CAMBIO_ESTADO',
  mensaje: 'El trabajo del paciente Código 1000 pasó a la etapa de En producción.',
  ordenId: 7,
  leida: false,
  fechaCreacion: '2026-08-20T10:15:00-03:00',
  fechaLectura: null,
};

const NOTIFICACION_LEIDA = {
  id: 11,
  tipoEvento: 'SOLICITUD_ACCESO',
  mensaje: 'Nueva solicitud de acceso.',
  ordenId: null,
  leida: true,
  fechaCreacion: '2026-08-19T09:00:00-03:00',
  fechaLectura: '2026-08-19T09:30:00-03:00',
};

const PAGINA = {
  contenido: [NOTIFICACION_SIN_LEER, NOTIFICACION_LEIDA],
  total: 2,
  pagina: 0,
  tamano: 10,
  totalPaginas: 1,
};

/**
 * El panel vive dentro del encabezado autenticado, así que en la aplicación siempre tiene sesión
 * y router alrededor. Desde T-25 los necesita de verdad: el `ordenId` es enlace, y solo para el
 * odontólogo (§8, §5.6).
 */
function renderizar(rol = 'ODONTOLOGO') {
  guardarUsuario({ id: 1, nombreCompleto: 'Dr. Juan Pérez', rol });
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <SesionProvider>
          <PanelNotificaciones onCerrar={vi.fn()} />
        </SesionProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
  return queryClient;
}

describe('PanelNotificaciones', () => {
  beforeEach(() => {
    localStorage.clear();
    listarPaginado.mockReset().mockResolvedValue(PAGINA);
    marcarLeida.mockReset().mockResolvedValue({ ...NOTIFICACION_SIN_LEER, leida: true });
    marcarTodasLeidas.mockReset().mockResolvedValue({ noLeidas: 0 });
    contarNoLeidas.mockReset().mockResolvedValue({ noLeidas: 1 });
  });

  /** Pendiente que dejó T-23: el `ordenId` lleva al seguimiento de §5.4. */
  it('el ordenId es enlace al seguimiento para el odontólogo', async () => {
    renderizar('ODONTOLOGO');

    const enlace = await screen.findByRole('link', { name: `Orden #${NOTIFICACION_SIN_LEER.ordenId}` });
    expect(enlace).toHaveAttribute('href', `/ordenes/${NOTIFICACION_SIN_LEER.ordenId}`);
  });

  /**
   * §8 asigna `/ordenes/:id` al odontólogo y §5.6 reserva la cancelación al propietario: el
   * administrador vería una pantalla que no es suya. Su destino lo construye T-26.
   */
  it('para el administrador el ordenId sigue siendo texto, sin enlace', async () => {
    renderizar('ADMIN');

    await screen.findByText(NOTIFICACION_SIN_LEER.mensaje);
    expect(screen.getByText(`Orden #${NOTIFICACION_SIN_LEER.ordenId}`)).toBeInTheDocument();
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });

  it('pide la primera página al backend con tamaño 10 (Agente.md §6.2)', async () => {
    renderizar();
    await screen.findByText(NOTIFICACION_SIN_LEER.mensaje);

    expect(listarPaginado).toHaveBeenCalledWith({ pagina: 0, tamano: 10 });
  });

  it('lista las notificaciones y distingue las que no están leídas', async () => {
    renderizar();

    expect(await screen.findByText(NOTIFICACION_SIN_LEER.mensaje)).toBeInTheDocument();
    expect(screen.getByText(NOTIFICACION_LEIDA.mensaje)).toBeInTheDocument();
    expect(screen.getAllByText('Sin leer')).toHaveLength(1);
    expect(screen.getAllByRole('button', { name: 'Marcar como leída' })).toHaveLength(1);
  });

  it('una notificación sin orden no muestra ninguna referencia de orden (§6.4)', async () => {
    renderizar();
    const item = (await screen.findByText(NOTIFICACION_LEIDA.mensaje)).closest('li');

    expect(within(item).queryByText(/Orden #/)).not.toBeInTheDocument();
  });

  it('"Marcar como leída" llama al endpoint con el id de la notificación', async () => {
    const usuarioEvento = userEvent.setup();
    renderizar();
    await screen.findByText(NOTIFICACION_SIN_LEER.mensaje);

    await usuarioEvento.click(screen.getByRole('button', { name: 'Marcar como leída' }));

    expect(marcarLeida).toHaveBeenCalledWith(12);
  });

  it('"Marcar todas como leídas" usa el contador que devuelve el endpoint, sin volver a pedirlo', async () => {
    const usuarioEvento = userEvent.setup();
    const queryClient = renderizar();
    await screen.findByText(NOTIFICACION_SIN_LEER.mensaje);

    await usuarioEvento.click(screen.getByRole('button', { name: 'Marcar todas como leídas' }));

    expect(marcarTodasLeidas).toHaveBeenCalledTimes(1);
    await vi.waitFor(() => {
      expect(queryClient.getQueryData(CLAVE_CONTADOR)).toEqual({ noLeidas: 0 });
    });
    expect(contarNoLeidas).not.toHaveBeenCalled();
  });

  it('"Siguiente" pide la página siguiente al backend, no corta en el cliente', async () => {
    listarPaginado.mockResolvedValue({ ...PAGINA, total: 12, totalPaginas: 2 });
    const usuarioEvento = userEvent.setup();
    renderizar();
    await screen.findByText(NOTIFICACION_SIN_LEER.mensaje);

    await usuarioEvento.click(screen.getByRole('button', { name: 'Siguiente' }));

    await vi.waitFor(() => {
      expect(listarPaginado).toHaveBeenCalledWith({ pagina: 1, tamano: 10 });
    });
  });

  it('no ofrece selector de tamaño de página: el panel no es una vista CRUD de §8.1', async () => {
    renderizar();
    await screen.findByText(NOTIFICACION_SIN_LEER.mensaje);

    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
  });

  it('estado de carga: avisa mientras no hay datos', () => {
    listarPaginado.mockReturnValue(new Promise(() => {}));
    renderizar();

    expect(screen.getByText('Cargando notificaciones...')).toBeInTheDocument();
  });

  it('estado vacío: lo dice explícitamente', async () => {
    listarPaginado.mockResolvedValue({ contenido: [], total: 0, pagina: 0, tamano: 10, totalPaginas: 0 });
    renderizar();

    expect(await screen.findByText('No tenés notificaciones.')).toBeInTheDocument();
  });

  it('estado de error: muestra el mensaje del backend y permite reintentar', async () => {
    listarPaginado.mockRejectedValue(new ApiError(400, 'TAMANO_PAGINA_INVALIDO', 'El tamaño de página debe ser 10, 20 o 30.'));
    const usuarioEvento = userEvent.setup();
    renderizar();

    expect(await screen.findByText('El tamaño de página debe ser 10, 20 o 30.')).toBeInTheDocument();

    listarPaginado.mockResolvedValue(PAGINA);
    await usuarioEvento.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByText(NOTIFICACION_SIN_LEER.mensaje)).toBeInTheDocument();
  });
});
