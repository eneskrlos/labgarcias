import { act, renderHook } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { usePaginacion } from '../usePaginacion';

function envoltorio(entradaInicial) {
  return function Envoltorio({ children }) {
    return <MemoryRouter initialEntries={[entradaInicial]}>{children}</MemoryRouter>;
  };
}

describe('usePaginacion', () => {
  it('por defecto, sin parámetros en la URL, usa page=0 y size=10', () => {
    const { result } = renderHook(() => usePaginacion(), { wrapper: envoltorio('/admin/tipos-trabajo') });

    expect(result.current.pagina).toBe(0);
    expect(result.current.tamano).toBe(10);
  });

  it('lee page y size desde la URL', () => {
    const { result } = renderHook(() => usePaginacion(), {
      wrapper: envoltorio('/admin/tipos-trabajo?page=2&size=20'),
    });

    expect(result.current.pagina).toBe(2);
    expect(result.current.tamano).toBe(20);
  });

  it('un size fuera de 10/20/30 cae al valor por defecto', () => {
    const { result } = renderHook(() => usePaginacion(), {
      wrapper: envoltorio('/admin/tipos-trabajo?size=15'),
    });

    expect(result.current.tamano).toBe(10);
  });

  it('cambiarPagina actualiza page conservando size', () => {
    const { result } = renderHook(() => usePaginacion(), {
      wrapper: envoltorio('/admin/tipos-trabajo?page=0&size=20'),
    });

    act(() => result.current.cambiarPagina(3));

    expect(result.current.pagina).toBe(3);
    expect(result.current.tamano).toBe(20);
  });

  it('cambiarTamano vuelve a page=0', () => {
    const { result } = renderHook(() => usePaginacion(), {
      wrapper: envoltorio('/admin/tipos-trabajo?page=4&size=10'),
    });

    act(() => result.current.cambiarTamano(30));

    expect(result.current.pagina).toBe(0);
    expect(result.current.tamano).toBe(30);
  });

  it('lee un filtro de la URL y devuelve cadena vacía si no está', () => {
    const { result } = renderHook(() => usePaginacion(), {
      wrapper: envoltorio('/admin/solicitudes?estado=RECHAZADA'),
    });

    expect(result.current.filtro('estado')).toBe('RECHAZADA');
    expect(result.current.filtro('busqueda')).toBe('');
  });

  it('cambiarFiltro escribe el valor en la URL y vuelve a page=0', () => {
    const { result } = renderHook(() => usePaginacion(), {
      wrapper: envoltorio('/admin/solicitudes?page=3&size=20'),
    });

    act(() => result.current.cambiarFiltro('estado', 'APROBADA'));

    expect(result.current.filtro('estado')).toBe('APROBADA');
    expect(result.current.pagina).toBe(0);
    expect(result.current.tamano).toBe(20);
  });

  it('cambiarFiltro con valor vacío quita el parámetro de la URL', () => {
    const { result } = renderHook(() => usePaginacion(), {
      wrapper: envoltorio('/admin/solicitudes?estado=APROBADA'),
    });

    act(() => result.current.cambiarFiltro('estado', ''));

    expect(result.current.filtro('estado')).toBe('');
  });
});
