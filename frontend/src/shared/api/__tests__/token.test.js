import { beforeEach, describe, expect, it } from 'vitest';
import {
  borrarSesion,
  borrarToken,
  borrarUsuario,
  guardarSesion,
  guardarToken,
  guardarUsuario,
  obtenerToken,
  obtenerUsuario,
} from '../token';

describe('token', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('obtenerToken devuelve null cuando no hay nada guardado', () => {
    expect(obtenerToken()).toBeNull();
  });

  it('guardarToken/obtenerToken hacen un round-trip', () => {
    guardarToken('jwt-de-prueba');
    expect(obtenerToken()).toBe('jwt-de-prueba');
  });

  it('borrarToken lo elimina', () => {
    guardarToken('jwt-de-prueba');
    borrarToken();
    expect(obtenerToken()).toBeNull();
  });

  it('obtenerUsuario devuelve null cuando no hay nada guardado', () => {
    expect(obtenerUsuario()).toBeNull();
  });

  it('guardarUsuario/obtenerUsuario serializan y deserializan el objeto', () => {
    const usuario = { id: 1, nombreCompleto: 'Dr. Juan Pérez', rol: 'ODONTOLOGO' };
    guardarUsuario(usuario);
    expect(obtenerUsuario()).toEqual(usuario);
  });

  it('borrarUsuario lo elimina', () => {
    guardarUsuario({ id: 1 });
    borrarUsuario();
    expect(obtenerUsuario()).toBeNull();
  });

  it('guardarSesion persiste token y usuario juntos', () => {
    const usuario = { id: 2, nombreCompleto: 'Ana', rol: 'ADMIN' };
    guardarSesion('jwt-sesion', usuario);
    expect(obtenerToken()).toBe('jwt-sesion');
    expect(obtenerUsuario()).toEqual(usuario);
  });

  it('borrarSesion limpia token y usuario juntos', () => {
    guardarSesion('jwt-sesion', { id: 2 });
    borrarSesion();
    expect(obtenerToken()).toBeNull();
    expect(obtenerUsuario()).toBeNull();
  });
});
