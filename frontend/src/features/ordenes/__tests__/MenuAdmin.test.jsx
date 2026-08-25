import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it } from 'vitest';
import MenuAdmin from '../MenuAdmin';
import { SesionProvider } from '../../../shared/hooks/useSesion';
import { guardarSesion } from '../../../shared/api/token';

const ADMIN = { id: 10, nombreCompleto: 'Mona', rol: 'ADMIN' };
const SUPERADMIN = { id: 11, nombreCompleto: 'Ernesto Carlos', rol: 'SUPERADMIN' };

function renderizar(usuario = ADMIN) {
  guardarSesion('token-de-prueba', usuario);
  render(
    <SesionProvider>
      <MemoryRouter>
        <MenuAdmin />
      </MemoryRouter>
    </SesionProvider>,
  );
}

describe('MenuAdmin', () => {
  afterEach(() => {
    localStorage.clear();
  });

  /** §8: los seis ítems del menú del laboratorio, en ese orden y sin ninguno más. */
  it('tiene exactamente los seis ítems de §8', () => {
    renderizar();

    expect(screen.getAllByRole('link').map((enlace) => enlace.textContent)).toEqual([
      'Dashboard',
      'Trabajos',
      'Odontólogos',
      'Solicitudes',
      'Tipos de trabajo',
      'Configuración',
    ]);
  });

  it('cada ítem apunta a la ruta que le fija §8', () => {
    renderizar();

    expect(screen.getByRole('link', { name: 'Dashboard' })).toHaveAttribute('href', '/admin');
    expect(screen.getByRole('link', { name: 'Trabajos' })).toHaveAttribute('href', '/admin/ordenes');
    expect(screen.getByRole('link', { name: 'Odontólogos' })).toHaveAttribute('href', '/admin/odontologos');
    expect(screen.getByRole('link', { name: 'Solicitudes' })).toHaveAttribute('href', '/admin/solicitudes');
    expect(screen.getByRole('link', { name: 'Tipos de trabajo' })).toHaveAttribute('href', '/admin/tipos-trabajo');
    expect(screen.getByRole('link', { name: 'Configuración' })).toHaveAttribute('href', '/admin/configuracion');
  });

  /**
   * §8 le da a Licencias su propia fila con rol SUPERADMIN (CU-23). Sin este ítem, el SuperAdmin
   * solo llegaría desde `/bloqueado` —con el sistema ya caído— y no podría renovar **antes** del
   * vencimiento, que es lo que evita el corte.
   */
  it('cu17 cu23 el SUPERADMIN ve además Usuarios y Licencias', () => {
    renderizar(SUPERADMIN);

    expect(screen.getAllByRole('link').map((enlace) => enlace.textContent)).toEqual([
      'Dashboard',
      'Trabajos',
      'Odontólogos',
      'Solicitudes',
      'Tipos de trabajo',
      'Configuración',
      'Usuarios',
      'Licencias',
    ]);
    expect(screen.getByRole('link', { name: 'Usuarios' })).toHaveAttribute('href', '/admin/usuarios');
    expect(screen.getByRole('link', { name: 'Licencias' })).toHaveAttribute('href', '/admin/licencias');
  });

  /** §3.5: las licencias son del SUPERADMIN; al ADMIN los endpoints le responden 403. */
  it('cu17 cu23 el ADMIN no ve Usuarios ni Licencias', () => {
    renderizar(ADMIN);

    expect(screen.queryByRole('link', { name: 'Usuarios' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Licencias' })).not.toBeInTheDocument();
  });

  /** §8 los excluye expresamente: S-03, D-11 y P-08 siguen sin resolverse. */
  it('no ofrece Pacientes, Calendario, Mensajes, Reportes ni Facturación', () => {
    renderizar();

    for (const excluido of [/pacientes/i, /calendario/i, /mensaje/i, /reporte/i, /facturaci/i]) {
      expect(screen.queryByRole('link', { name: excluido })).not.toBeInTheDocument();
    }
  });
});
