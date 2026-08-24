import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiFetch } from '../../../shared/api/cliente';
import {
  contarNoLeidas,
  guardarConfiguracionNotificaciones,
  listarPaginado,
  marcarLeida,
  marcarTodasLeidas,
  obtenerConfiguracionNotificaciones,
} from '../api';

vi.mock('../../../shared/api/cliente', () => ({
  apiFetch: vi.fn(),
}));

describe('features/notificaciones/api', () => {
  beforeEach(() => {
    apiFetch.mockReset();
  });

  it('listarPaginado envía page y size', () => {
    listarPaginado({ pagina: 2, tamano: 10 });
    expect(apiFetch).toHaveBeenCalledWith('/notificaciones?page=2&size=10');
  });

  it('listarPaginado no envía ningún identificador de usuario (§6 criterio 3)', () => {
    listarPaginado({ pagina: 0, tamano: 10 });
    expect(apiFetch.mock.calls[0][0]).not.toMatch(/usuario|destinatario/i);
  });

  it('contarNoLeidas llama a GET /notificaciones/contador', () => {
    contarNoLeidas();
    expect(apiFetch).toHaveBeenCalledWith('/notificaciones/contador');
  });

  it('marcarLeida llama a PATCH /notificaciones/{id}/leer', () => {
    marcarLeida(12);
    expect(apiFetch).toHaveBeenCalledWith('/notificaciones/12/leer', { method: 'PATCH' });
  });

  it('marcarTodasLeidas llama a PATCH /notificaciones/leer-todas', () => {
    marcarTodasLeidas();
    expect(apiFetch).toHaveBeenCalledWith('/notificaciones/leer-todas', { method: 'PATCH' });
  });

  /** §6.4: la ruta no lleva id; cada administrador configura la suya, tomada del token. */
  it('obtenerConfiguracionNotificaciones llama a GET /configuracion-notificaciones sin id', () => {
    obtenerConfiguracionNotificaciones();
    expect(apiFetch).toHaveBeenCalledWith('/configuracion-notificaciones');
    expect(apiFetch.mock.calls[0][0]).not.toMatch(/usuario|destinatario|\d/);
  });

  /** CU-21: el PUT reemplaza la configuración entera, así que las tres banderas viajan siempre. */
  it('guardarConfiguracionNotificaciones manda las tres banderas, también las apagadas', () => {
    guardarConfiguracionNotificaciones({
      canalAppActivo: true,
      canalCorreoActivo: false,
      canalTelegramActivo: false,
      telegramChatId: '',
    });

    const [ruta, opciones] = apiFetch.mock.calls[0];
    expect(ruta).toBe('/configuracion-notificaciones');
    expect(opciones.method).toBe('PUT');
    expect(JSON.parse(opciones.body)).toEqual({
      canalAppActivo: true,
      canalCorreoActivo: false,
      canalTelegramActivo: false,
      telegramChatId: '',
    });
  });

  /** P-18: WhatsApp es estructura y el request del backend no lo acepta; no se manda. */
  it('p18 guardarConfiguracionNotificaciones no manda canalWhatsappActivo', () => {
    guardarConfiguracionNotificaciones({
      canalAppActivo: true,
      canalCorreoActivo: true,
      canalTelegramActivo: false,
      telegramChatId: '',
    });

    expect(JSON.parse(apiFetch.mock.calls[0][1].body)).not.toHaveProperty('canalWhatsappActivo');
  });
});
