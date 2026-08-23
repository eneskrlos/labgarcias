import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiFetch } from '../../../shared/api/cliente';
import * as api from '../api';
import {
  cambiarPassword,
  crearOdontologo,
  listarSolicitudes,
  login,
  logout,
  rechazarSolicitud,
  solicitarAcceso,
} from '../api';

vi.mock('../../../shared/api/cliente', () => ({
  apiFetch: vi.fn(),
}));

describe('features/auth/api', () => {
  beforeEach(() => {
    apiFetch.mockReset();
  });

  it('login llama a POST /auth/login con el cuerpo serializado', () => {
    const datos = { correo: 'juan@mail.com', password: 'x' };
    login(datos);
    expect(apiFetch).toHaveBeenCalledWith('/auth/login', { method: 'POST', body: JSON.stringify(datos) });
  });

  it('logout llama a POST /auth/logout sin cuerpo', () => {
    logout();
    expect(apiFetch).toHaveBeenCalledWith('/auth/logout', { method: 'POST' });
  });

  it('solicitarAcceso llama a POST /auth/solicitud-acceso con el cuerpo serializado', () => {
    const datos = {
      nombreCompleto: 'Dr. Juan Pérez',
      correo: 'juan@mail.com',
      direccion: 'Av. 18 de Julio 1234',
      telefono: '+59891234567',
    };
    solicitarAcceso(datos);
    expect(apiFetch).toHaveBeenCalledWith('/auth/solicitud-acceso', {
      method: 'POST',
      body: JSON.stringify(datos),
    });
  });

  it('listarSolicitudes envía page, size y el estado cuando viene', () => {
    listarSolicitudes({ pagina: 1, tamano: 20, estado: 'PENDIENTE' });
    expect(apiFetch).toHaveBeenCalledWith('/solicitudes-acceso?page=1&size=20&estado=PENDIENTE');
  });

  it('listarSolicitudes omite el estado cuando no hay filtro', () => {
    listarSolicitudes({ pagina: 0, tamano: 10, estado: null });
    expect(apiFetch).toHaveBeenCalledWith('/solicitudes-acceso?page=0&size=10');
  });

  it('rechazarSolicitud llama a PATCH /solicitudes-acceso/{id}/rechazar', () => {
    rechazarSolicitud(12);
    expect(apiFetch).toHaveBeenCalledWith('/solicitudes-acceso/12/rechazar', { method: 'PATCH' });
  });

  it('crearOdontologo llama a POST /odontologos con el cuerpo serializado', () => {
    const datos = {
      nombreCompleto: 'Dr. Juan Pérez',
      correo: 'juan@mail.com',
      nombreUsuario: 'jperez',
      direccion: 'Av. 18 de Julio 1234',
      telefono: '+59891234567',
      solicitudId: 12,
    };
    crearOdontologo(datos);
    expect(apiFetch).toHaveBeenCalledWith('/odontologos', { method: 'POST', body: JSON.stringify(datos) });
  });

  /** §3.1.b: la contraseña la genera el backend; el cliente no la manda ni la puede elegir. */
  it('crearOdontologo no envía ninguna contraseña', () => {
    crearOdontologo({ nombreCompleto: 'X', correo: 'x@mail.com', nombreUsuario: 'x', solicitudId: null });

    expect(apiFetch.mock.calls[0][1].body).not.toMatch(/password/i);
  });

  it('cambiarPassword llama a POST /auth/cambiar-password', () => {
    const datos = { passwordActual: 'temporal', passwordNueva: 'MiClave2026$' };
    cambiarPassword(datos);
    expect(apiFetch).toHaveBeenCalledWith('/auth/cambiar-password', {
      method: 'POST',
      body: JSON.stringify(datos),
    });
  });

  // CR-01 (D-17/D-18): estas funciones se retiraron. El test las vigila para que no vuelvan por descuido.
  it.each(['registrar', 'loginGoogle', 'verificar', 'reenviarVerificacion'])(
    'no expone %s tras CR-01',
    (nombre) => {
      expect(api[nombre]).toBeUndefined();
    },
  );
});
