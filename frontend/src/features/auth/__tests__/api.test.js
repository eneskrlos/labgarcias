import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiFetch } from '../../../shared/api/cliente';
import * as api from '../api';
import { login, logout } from '../api';

vi.mock('../../../shared/api/cliente', () => ({
  apiFetch: vi.fn(),
}));

describe('features/auth/api', () => {
  beforeEach(() => {
    apiFetch.mockReset();
  });

  it('login llama a POST /auth/login con el cuerpo serializado', () => {
    const datos = { correo: 'juan@mail.com', password: 'x' };
    login(datos);
    expect(apiFetch).toHaveBeenCalledWith('/auth/login', { method: 'POST', body: JSON.stringify(datos) });
  });

  it('logout llama a POST /auth/logout sin cuerpo', () => {
    logout();
    expect(apiFetch).toHaveBeenCalledWith('/auth/logout', { method: 'POST' });
  });

  // CR-01 (D-17/D-18): estas funciones se retiraron. El test las vigila para que no vuelvan por descuido.
  it.each(['registrar', 'loginGoogle', 'verificar', 'reenviarVerificacion'])(
    'no expone %s tras CR-01',
    (nombre) => {
      expect(api[nombre]).toBeUndefined();
    },
  );
});
