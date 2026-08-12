import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiFetch } from '../../../shared/api/cliente';
import { actualizar, cambiarEstado, crear, listarTodos } from '../api';

vi.mock('../../../shared/api/cliente', () => ({
  apiFetch: vi.fn(),
}));

describe('features/catalogos/api', () => {
  beforeEach(() => {
    apiFetch.mockReset();
  });

  it('listarTodos llama a GET /tipos-trabajo/todos', () => {
    listarTodos();
    expect(apiFetch).toHaveBeenCalledWith('/tipos-trabajo/todos');
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
