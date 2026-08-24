import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiFetch } from '../../../shared/api/cliente';
import { avanzarEstado, crearOrden, listarOrdenesAdmin } from '../api';

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

  /** §5.7: los tres filtros del laboratorio son opcionales y se combinan. */
  it('listarOrdenesAdmin manda los filtros que recibe', () => {
    listarOrdenesAdmin({ pagina: 1, tamano: 20, estado: 'LISTO', tipoOrden: 'URGENTE', odontologoId: 17 });

    expect(apiFetch).toHaveBeenCalledWith(
      '/admin/ordenes?page=1&size=20&estado=LISTO&tipoOrden=URGENTE&odontologoId=17',
    );
  });

  it('listarOrdenesAdmin omite los filtros vacíos', () => {
    listarOrdenesAdmin({ pagina: 0, tamano: 10, estado: null, tipoOrden: null, odontologoId: null });

    expect(apiFetch).toHaveBeenCalledWith('/admin/ordenes?page=0&size=10');
  });

  /** §5.5: el código que se manda es el que trajo `siguienteEstado`, no uno elegido en la pantalla. */
  it('avanzarEstado llama a PATCH /ordenes/{id}/estado con el código recibido', () => {
    avanzarEstado(7, 'CONTROL_CALIDAD');

    expect(apiFetch).toHaveBeenCalledWith('/ordenes/7/estado', {
      method: 'PATCH',
      body: JSON.stringify({ estadoCodigo: 'CONTROL_CALIDAD' }),
    });
  });
});
