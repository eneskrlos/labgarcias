import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ConfiguracionNotificaciones from '../ConfiguracionNotificaciones';
import { guardarConfiguracionNotificaciones, obtenerConfiguracionNotificaciones } from '../api';
import { ApiError } from '../../../shared/api/cliente';

vi.mock('../api', () => ({
  obtenerConfiguracionNotificaciones: vi.fn(),
  guardarConfiguracionNotificaciones: vi.fn(),
}));

const CONFIGURACION = {
  canalAppActivo: true,
  canalCorreoActivo: true,
  canalTelegramActivo: false,
  canalWhatsappActivo: false,
  telegramChatId: null,
  fechaActualizacion: '2026-08-23T10:00:00-03:00',
};

function renderizar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/admin/configuracion']}>
        <Routes>
          <Route path="/admin/configuracion" element={<ConfiguracionNotificaciones />} />
          <Route path="/admin" element={<p>Dashboard</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('ConfiguracionNotificaciones', () => {
  beforeEach(() => {
    obtenerConfiguracionNotificaciones.mockReset().mockResolvedValue(CONFIGURACION);
    guardarConfiguracionNotificaciones.mockReset().mockResolvedValue(CONFIGURACION);
  });

  /** CU-21: la pantalla muestra la configuración vigente que devuelve el backend. */
  it('muestra los canales tal como los devuelve el backend', async () => {
    renderizar();

    expect(await screen.findByLabelText('Aplicación')).toBeChecked();
    expect(screen.getByLabelText('Correo')).toBeChecked();
    expect(screen.getByLabelText('Telegram')).not.toBeChecked();
    expect(screen.getByLabelText('Chat de Telegram')).toHaveValue('');
  });

  /** §8.1 Regla 1/CU-21: la ayuda del campo explica cuándo el chat de Telegram es obligatorio. */
  it('muestra la regla CU-21 como ayuda del campo de Telegram', async () => {
    renderizar();

    expect(await screen.findByText('CU-21: obligatorio si activás Telegram.')).toBeInTheDocument();
  });

  /** CU-21: el PUT reemplaza la configuración entera; las tres banderas viajan siempre. */
  it('guarda las tres banderas, también las que quedan apagadas', async () => {
    renderizar();
    await screen.findByLabelText('Aplicación');

    await userEvent.click(screen.getByLabelText('Correo'));
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(guardarConfiguracionNotificaciones).toHaveBeenCalledWith({
      canalAppActivo: true,
      canalCorreoActivo: false,
      canalTelegramActivo: false,
      telegramChatId: '',
    });
  });

  /**
   * Criterio 2 de T-34 y criterio 4 de §6: el `422 TELEGRAM_SIN_DESTINO` se muestra **en el campo
   * del chat**, con el mensaje que mandó el backend. La pantalla no adelanta el rechazo ni
   * reimplementa la regla de CU-21.
   */
  it('cu21 muestra el 422 TELEGRAM_SIN_DESTINO en el campo del chat', async () => {
    guardarConfiguracionNotificaciones.mockRejectedValue(
      new ApiError(422, 'TELEGRAM_SIN_DESTINO',
        'Para recibir por Telegram hace falta indicar el chat de destino.', 'telegramChatId'),
    );
    renderizar();
    await screen.findByLabelText('Aplicación');

    await userEvent.click(screen.getByLabelText('Telegram'));
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));

    const mensaje = await screen.findByText('Para recibir por Telegram hace falta indicar el chat de destino.');
    // El mensaje vive dentro del CampoFormulario del chat, no suelto en el encabezado.
    expect(mensaje.closest('div')).toContainElement(screen.getByLabelText('Chat de Telegram'));
  });

  /** La pantalla no valida CU-21 por su cuenta: manda y deja que el backend decida. */
  it('cu21 no bloquea el envío cuando falta el chat: lo rechaza el backend', async () => {
    renderizar();
    await screen.findByLabelText('Aplicación');

    await userEvent.click(screen.getByLabelText('Telegram'));
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(guardarConfiguracionNotificaciones).toHaveBeenCalledWith(
      expect.objectContaining({ canalTelegramActivo: true, telegramChatId: '' }),
    );
  });

  /** P-18: WhatsApp se informa y no se puede activar. */
  it('p18 la casilla de WhatsApp está deshabilitada', async () => {
    renderizar();

    expect(await screen.findByLabelText('WhatsApp')).toBeDisabled();
    expect(screen.getByText(/todavía no hay proveedor de WhatsApp/)).toBeInTheDocument();
  });

  /** P-18: aunque el backend lo informara en true, no se manda de vuelta. */
  it('p18 nunca manda canalWhatsappActivo al backend', async () => {
    obtenerConfiguracionNotificaciones.mockResolvedValue({ ...CONFIGURACION, canalWhatsappActivo: true });
    renderizar();
    await screen.findByLabelText('Aplicación');

    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(guardarConfiguracionNotificaciones.mock.calls[0][0]).not.toHaveProperty('canalWhatsappActivo');
  });

  /** Guardar deja al usuario acá, con la confirmación: es una pantalla de ajustes, no un alta. */
  it('al guardar se queda en la pantalla y muestra la confirmación', async () => {
    renderizar();
    await screen.findByLabelText('Aplicación');

    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(await screen.findByRole('status')).toHaveTextContent('Configuración guardada.');
    expect(screen.getByLabelText('Aplicación')).toBeInTheDocument();
    expect(screen.queryByText('Dashboard')).not.toBeInTheDocument();
  });

  /** El formulario se refresca con lo que devolvió el PUT, no con lo que se mandó. */
  it('el formulario queda con lo que devolvió el backend', async () => {
    guardarConfiguracionNotificaciones.mockResolvedValue({
      ...CONFIGURACION,
      canalTelegramActivo: true,
      telegramChatId: '123456789',
    });
    renderizar();
    await screen.findByLabelText('Aplicación');

    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));

    await screen.findByRole('status');
    expect(screen.getByLabelText('Telegram')).toBeChecked();
    expect(screen.getByLabelText('Chat de Telegram')).toHaveValue('123456789');
  });

  /** §8.1 Regla 5: cancelar vuelve sin guardar. Acá el destino es el dashboard, de donde se llega. */
  it('cancelar vuelve a /admin sin guardar', async () => {
    renderizar();
    await screen.findByLabelText('Aplicación');

    await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(await screen.findByText('Dashboard')).toBeInTheDocument();
    expect(guardarConfiguracionNotificaciones).not.toHaveBeenCalled();
  });

  /** §8.1 Regla 3: el error de carga se muestra y se puede reintentar. */
  it('muestra el error de carga con opción de reintentar', async () => {
    obtenerConfiguracionNotificaciones.mockRejectedValue(
      new ApiError(500, 'ERROR', 'No se pudo cargar la configuración.'),
    );
    renderizar();

    expect(await screen.findByText('No se pudo cargar la configuración.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });

  /** §8.1 Regla 4: la pantalla se arma con los componentes compartidos, con sus textos. */
  it('usa los botones Guardar y Cancelar del layout compartido', async () => {
    renderizar();

    expect(await screen.findByRole('button', { name: 'Guardar' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeInTheDocument();
  });
});
