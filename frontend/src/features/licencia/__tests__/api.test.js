import { beforeEach, describe, expect, it, vi } from 'vitest';
import { listarLicencias, obtenerLicenciaVigente, registrarLicencia } from '../api';
import { apiFetch } from '../../../shared/api/cliente';

vi.mock('../../../shared/api/cliente', () => ({ apiFetch: vi.fn() }));

describe('features/licencia/api', () => {
  beforeEach(() => {
    apiFetch.mockReset().mockResolvedValue({});
  });

  /** §8.1 Regla 2: la paginación la resuelve el backend; acá solo viajan page y size. */
  it('listarLicencias envía page y size', () => {
    listarLicencias({ pagina: 2, tamano: 20 });
    expect(apiFetch).toHaveBeenCalledWith('/licencias?page=2&size=20');
  });

  it('obtenerLicenciaVigente llama a GET /licencias/vigente', () => {
    obtenerLicenciaVigente();
    expect(apiFetch).toHaveBeenCalledWith('/licencias/vigente');
  });

  /** P-11/P-12: solo fechas y observación; ni plan, ni precio, ni pasarela. */
  it('registrarLicencia manda solo las fechas y la observación', () => {
    registrarLicencia({
      fechaInicio: '2026-01-01',
      fechaVencimiento: '2026-12-31',
      observacion: 'Renovación anual',
    });

    const [ruta, opciones] = apiFetch.mock.calls[0];
    expect(ruta).toBe('/licencias');
    expect(opciones.method).toBe('POST');
    expect(Object.keys(JSON.parse(opciones.body)).sort()).toEqual([
      'fechaInicio',
      'fechaVencimiento',
      'observacion',
    ]);
  });
});
