import { beforeEach, describe, expect, it, vi } from 'vitest';
import { obtenerDashboardAdmin, obtenerPanelOdontologo } from '../api';
import { apiFetch } from '../../../shared/api/cliente';

vi.mock('../../../shared/api/cliente', () => ({
  apiFetch: vi.fn(),
}));

describe('api de paneles', () => {
  beforeEach(() => {
    apiFetch.mockReset().mockResolvedValue({});
  });

  /**
   * CU-02/RN-01: el panel del odontólogo se pide **sin ningún parámetro**. El dueño lo pone el
   * token; en cuanto la URL admitiera un id, un odontólogo podría pedir el panel de otro.
   */
  it('rn01 el panel del odontólogo se pide sin parámetros', async () => {
    await obtenerPanelOdontologo();

    expect(apiFetch).toHaveBeenCalledWith('/dashboard');
    expect(apiFetch.mock.calls[0][0]).not.toContain('?');
    expect(apiFetch.mock.calls[0][0]).not.toContain('odontologo');
  });

  it('el dashboard del laboratorio va a su propia ruta', async () => {
    await obtenerDashboardAdmin();

    expect(apiFetch).toHaveBeenCalledWith('/admin/dashboard');
  });
});
