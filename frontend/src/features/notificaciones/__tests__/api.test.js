import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiFetch } from '../../../shared/api/cliente';
import { contarNoLeidas, listarPaginado, marcarLeida, marcarTodasLeidas } from '../api';

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
});
