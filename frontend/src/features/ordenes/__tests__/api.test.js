import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiFetch } from '../../../shared/api/cliente';
import { crearOrden } from '../api';

vi.mock('../../../shared/api/cliente', () => ({
  apiFetch: vi.fn(),
}));

describe('features/ordenes/api', () => {
  beforeEach(() => {
    apiFetch.mockReset();
  });

  it('crearOrden llama a POST /ordenes con el cuerpo serializado', () => {
    const datos = {
      odontologoId: 3,
      pacienteNombre: 'Martín Pérez',
      fechaIngreso: '2026-08-06',
      tipoTrabajoId: 16,
      tipoOrdenCodigo: 'NORMAL',
      descripcion: 'Disyuntor superior',
    };

    crearOrden(datos);

    expect(apiFetch).toHaveBeenCalledWith('/ordenes', { method: 'POST', body: JSON.stringify(datos) });
  });

  /** Agente.md 6.1: precio, recargo, estado y fecha estimada los deriva el backend. */
  it('crearOrden no manda ningún dato calculado', () => {
    crearOrden({ odontologoId: 3, pacienteNombre: 'X', fechaIngreso: '2026-08-06', tipoTrabajoId: 1, tipoOrdenCodigo: 'URGENTE' });

    const cuerpo = apiFetch.mock.calls[0][1].body;
    expect(cuerpo).not.toMatch(/precio|recargo|fechaEstimada|estadoId/i);
  });
});
