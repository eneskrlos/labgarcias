import { act, renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it } from 'vitest';
import { SesionProvider, useSesion } from '../useSesion';
import { guardarUsuario, obtenerToken, obtenerUsuario } from '../../api/token';

function envolver({ children }) {
  return <SesionProvider>{children}</SesionProvider>;
}

describe('useSesion', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('lanza un error si se usa fuera de SesionProvider', () => {
    expect(() => renderHook(() => useSesion())).toThrow(/SesionProvider/);
  });

  it('arranca sin usuario cuando no hay sesión guardada', () => {
    const { result } = renderHook(() => useSesion(), { wrapper: envolver });

    expect(result.current.usuario).toBeNull();
  });

  it('restaura el usuario desde localStorage al montar (sesión persistida)', () => {
    guardarUsuario({ id: 1, nombreCompleto: 'Dr. Juan Pérez', rol: 'ODONTOLOGO' });

    const { result } = renderHook(() => useSesion(), { wrapper: envolver });

    expect(result.current.usuario).toEqual({ id: 1, nombreCompleto: 'Dr. Juan Pérez', rol: 'ODONTOLOGO' });
  });

  it('iniciarSesion actualiza el estado y persiste en localStorage', () => {
    const { result } = renderHook(() => useSesion(), { wrapper: envolver });
    const usuario = { id: 2, nombreCompleto: 'Ana', rol: 'ADMIN' };

    act(() => {
      result.current.iniciarSesion('jwt-abc', usuario);
    });

    expect(result.current.usuario).toEqual(usuario);
    expect(obtenerToken()).toBe('jwt-abc');
    expect(obtenerUsuario()).toEqual(usuario);
  });

  it('cerrarSesion limpia el estado y localStorage', () => {
    const { result } = renderHook(() => useSesion(), { wrapper: envolver });

    act(() => {
      result.current.iniciarSesion('jwt-abc', { id: 2 });
    });
    act(() => {
      result.current.cerrarSesion();
    });

    expect(result.current.usuario).toBeNull();
    expect(obtenerToken()).toBeNull();
    expect(obtenerUsuario()).toBeNull();
  });
});
