import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiFetch } from '../../../shared/api/cliente';
import { actualizar, cambiarEstado, crear, listarActivos, listarPaginado, obtenerPorId } from '../api';

vi.mock('../../../shared/api/cliente', () => ({
  apiFetch: vi.fn(),
}));

describe('features/catalogos/api', () => {
  beforeEach(() => {
    apiFetch.mockReset();
  });

  it('listarActivos llama a GET /tipos-trabajo/activos', () => {
    listarActivos();
    expect(apiFetch).toHaveBeenCalledWith('/tipos-trabajo/activos');
  });

  it('listarPaginado envía page y size', () => {
    listarPaginado({ pagina: 2, tamano: 20 });
    expect(apiFetch).toHaveBeenCalledWith('/tipos-trabajo?page=2&size=20');
  });

  it('listarPaginado agrega activo y busqueda solo cuando están definidos', () => {
    listarPaginado({ pagina: 0, tamano: 10, activo: true, busqueda: 'plac' });
    expect(apiFetch).toHaveBeenCalledWith('/tipos-trabajo?page=0&size=10&activo=true&busqueda=plac');
  });

  it('obtenerPorId llama a GET /tipos-trabajo/{id}', () => {
    obtenerPorId(16);
    expect(apiFetch).toHaveBeenCalledWith('/tipos-trabajo/16');
  });

  it('crear llama a POST /tipos-trabajo con el cuerpo serializado', () => {
    const datos = { nombre: 'X', diasEstimados: 7, precio: 250 };
    crear(datos);
    expect(apiFetch).toHaveBeenCalledWith('/tipos-trabajo', { method: 'POST', body: JSON.stringify(datos) });
  });

  it('actualizar llama a PUT /tipos-trabajo/{id}', () => {
    const datos = { nombre: 'X', diasEstimados: 7, precio: 250 };
    actualizar(16, datos);
    expect(apiFetch).toHaveBeenCalledWith('/tipos-trabajo/16', { method: 'PUT', body: JSON.stringify(datos) });
  });

  it('cambiarEstado llama a PATCH /tipos-trabajo/{id}/estado con { activo }', () => {
    cambiarEstado(16, false);
    expect(apiFetch).toHaveBeenCalledWith('/tipos-trabajo/16/estado', {
      method: 'PATCH',
      body: JSON.stringify({ activo: false }),
    });
  });
});
