import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiFetch } from '../../../shared/api/cliente';
import { login, loginGoogle, logout, reenviarVerificacion, registrar, verificar } from '../api';

vi.mock('../../../shared/api/cliente', () => ({
  apiFetch: vi.fn(),
}));

describe('features/auth/api', () => {
  beforeEach(() => {
    apiFetch.mockReset();
  });

  it('registrar llama a POST /auth/registro con el cuerpo serializado', () => {
    const datos = { correo: 'juan@mail.com', password: 'x' };
    registrar(datos);
    expect(apiFetch).toHaveBeenCalledWith('/auth/registro', { method: 'POST', body: JSON.stringify(datos) });
  });

  it('login llama a POST /auth/login con el cuerpo serializado', () => {
    const datos = { correo: 'juan@mail.com', password: 'x' };
    login(datos);
    expect(apiFetch).toHaveBeenCalledWith('/auth/login', { method: 'POST', body: JSON.stringify(datos) });
  });

  it('loginGoogle envuelve el idToken en { idToken }', () => {
    loginGoogle('token-google');
    expect(apiFetch).toHaveBeenCalledWith('/auth/google', {
      method: 'POST',
      body: JSON.stringify({ idToken: 'token-google' }),
    });
  });

  it('verificar arma el query string con el token codificado', () => {
    verificar('a b&c');
    expect(apiFetch).toHaveBeenCalledWith('/auth/verificar?token=a%20b%26c');
  });

  it('reenviarVerificacion envuelve el correo en { correo }', () => {
    reenviarVerificacion('juan@mail.com');
    expect(apiFetch).toHaveBeenCalledWith('/auth/reenviar-verificacion', {
      method: 'POST',
      body: JSON.stringify({ correo: 'juan@mail.com' }),
    });
  });

  it('logout llama a POST /auth/logout sin cuerpo', () => {
    logout();
    expect(apiFetch).toHaveBeenCalledWith('/auth/logout', { method: 'POST' });
  });
});
