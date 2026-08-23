import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiFetch } from '../../../shared/api/cliente';
import { conectarTelegram, desvincularTelegram, obtenerPerfil } from '../api';

vi.mock('../../../shared/api/cliente', () => ({
  apiFetch: vi.fn(),
}));

describe('features/perfil/api', () => {
  beforeEach(() => {
    apiFetch.mockReset();
  });

  it('obtenerPerfil llama a GET /perfil', () => {
    obtenerPerfil();
    expect(apiFetch).toHaveBeenCalledWith('/perfil');
  });

  it('conectarTelegram llama a POST /telegram/vinculacion', () => {
    conectarTelegram();
    expect(apiFetch).toHaveBeenCalledWith('/telegram/vinculacion', { method: 'POST' });
  });

  it('desvincularTelegram llama a DELETE /telegram/vinculacion', () => {
    desvincularTelegram();
    expect(apiFetch).toHaveBeenCalledWith('/telegram/vinculacion', { method: 'DELETE' });
  });

  /** §6.5: los dos endpoints operan sobre el usuario del token; mandar un id abriría el de otro. */
  it('ninguna llamada de vinculación manda un id de usuario', () => {
    conectarTelegram();
    desvincularTelegram();

    expect(apiFetch.mock.calls.every(([ruta]) => !ruta.includes('usuario'))).toBe(true);
  });
});
