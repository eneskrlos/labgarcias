import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import MenuAdmin from '../MenuAdmin';

function renderizar() {
  render(
    <MemoryRouter>
      <MenuAdmin />
    </MemoryRouter>,
  );
}

describe('MenuAdmin', () => {
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

  /** §8 los excluye expresamente: S-03, D-11 y P-08 siguen sin resolverse. */
  it('no ofrece Pacientes, Calendario, Mensajes, Reportes ni Facturación', () => {
    renderizar();

    for (const excluido of [/pacientes/i, /calendario/i, /mensaje/i, /reporte/i, /facturaci/i]) {
      expect(screen.queryByRole('link', { name: excluido })).not.toBeInTheDocument();
    }
  });
});
