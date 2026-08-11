import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { apiFetch, ApiError } from '../cliente';
import { guardarToken } from '../token';

function respuestaSimulada({ status, ok, cuerpo }) {
  return {
    status,
    ok,
    json: () => Promise.resolve(cuerpo),
  };
}

describe('apiFetch', () => {
  beforeEach(() => {
    localStorage.clear();
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: { ...window.location, assign: vi.fn() },
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('no agrega Authorization si no hay token guardado', async () => {
    const fetchSimulado = vi.fn().mockResolvedValue(respuestaSimulada({ status: 200, ok: true, cuerpo: {} }));
    vi.stubGlobal('fetch', fetchSimulado);

    await apiFetch('/algo');

    const headers = fetchSimulado.mock.calls[0][1].headers;
    expect(headers.has('Authorization')).toBe(false);
  });

  it('agrega el header Authorization: Bearer <token> cuando hay sesión', async () => {
    guardarToken('jwt-123');
    const fetchSimulado = vi.fn().mockResolvedValue(respuestaSimulada({ status: 200, ok: true, cuerpo: {} }));
    vi.stubGlobal('fetch', fetchSimulado);

    await apiFetch('/algo');

    const headers = fetchSimulado.mock.calls[0][1].headers;
    expect(headers.get('Authorization')).toBe('Bearer jwt-123');
  });

  it('arma la URL concatenando VITE_API_BASE_URL con el path', async () => {
    const fetchSimulado = vi.fn().mockResolvedValue(respuestaSimulada({ status: 200, ok: true, cuerpo: {} }));
    vi.stubGlobal('fetch', fetchSimulado);

    await apiFetch('/auth/login');

    expect(fetchSimulado.mock.calls[0][0]).toBe('http://localhost:8080/api/v1/auth/login');
  });

  it('en un 423 redirige a /bloqueado y lanza ApiError LICENCIA_VENCIDA', async () => {
    const fetchSimulado = vi.fn().mockResolvedValue(respuestaSimulada({ status: 423, ok: false, cuerpo: {} }));
    vi.stubGlobal('fetch', fetchSimulado);

    await expect(apiFetch('/algo')).rejects.toMatchObject({ codigo: 'LICENCIA_VENCIDA', status: 423 });
    expect(window.location.assign).toHaveBeenCalledWith('/bloqueado');
  });

  it('en un error de negocio propaga codigo/mensaje/campo del backend', async () => {
    const fetchSimulado = vi.fn().mockResolvedValue(respuestaSimulada({
      status: 409,
      ok: false,
      cuerpo: { codigo: 'CORREO_YA_REGISTRADO', mensaje: 'Ya existe una cuenta con ese correo.', campo: 'correo' },
    }));
    vi.stubGlobal('fetch', fetchSimulado);

    const error = await apiFetch('/auth/registro').catch((e) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(409);
    expect(error.codigo).toBe('CORREO_YA_REGISTRADO');
    expect(error.mensaje).toBe('Ya existe una cuenta con ese correo.');
    expect(error.campo).toBe('correo');
  });

  it('si el cuerpo de error no se puede parsear usa valores por defecto', async () => {
    const fetchSimulado = vi.fn().mockResolvedValue({
      status: 500,
      ok: false,
      json: () => Promise.reject(new Error('no es JSON')),
    });
    vi.stubGlobal('fetch', fetchSimulado);

    const error = await apiFetch('/algo').catch((e) => e);

    expect(error.codigo).toBe('ERROR_DESCONOCIDO');
    expect(error.mensaje).toBe('Ocurrió un error inesperado.');
    expect(error.campo).toBeNull();
  });

  it('en un 204 devuelve null sin intentar parsear JSON', async () => {
    const fetchSimulado = vi.fn().mockResolvedValue({
      status: 204,
      ok: true,
      json: () => Promise.reject(new Error('no debería llamarse')),
    });
    vi.stubGlobal('fetch', fetchSimulado);

    await expect(apiFetch('/algo')).resolves.toBeNull();
  });

  it('en una respuesta exitosa devuelve el JSON parseado', async () => {
    const fetchSimulado = vi.fn().mockResolvedValue(
      respuestaSimulada({ status: 200, ok: true, cuerpo: { mensaje: 'ok' } }),
    );
    vi.stubGlobal('fetch', fetchSimulado);

    await expect(apiFetch('/algo')).resolves.toEqual({ mensaje: 'ok' });
  });
});
