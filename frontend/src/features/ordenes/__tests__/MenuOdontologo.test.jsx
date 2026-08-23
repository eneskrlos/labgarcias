import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import MenuOdontologo from '../MenuOdontologo';

function renderizar() {
  render(
    <MemoryRouter>
      <MenuOdontologo />
    </MemoryRouter>,
  );
}

describe('MenuOdontologo', () => {
  /** §8: los cuatro ítems del menú del odontólogo, en ese orden y sin ninguno más. */
  it('tiene exactamente Inicio, Mis trabajos, Historial y Perfil', () => {
    renderizar();

    expect(screen.getAllByRole('link').map((enlace) => enlace.textContent)).toEqual([
      'Inicio',
      'Mis trabajos',
      'Historial',
      'Perfil',
    ]);
  });

  it('cada ítem apunta a la ruta que le fija §8', () => {
    renderizar();

    expect(screen.getByRole('link', { name: 'Inicio' })).toHaveAttribute('href', '/');
    expect(screen.getByRole('link', { name: 'Mis trabajos' })).toHaveAttribute('href', '/ordenes');
    expect(screen.getByRole('link', { name: 'Historial' })).toHaveAttribute('href', '/historial');
    expect(screen.getByRole('link', { name: 'Perfil' })).toHaveAttribute('href', '/perfil');
  });

  /** D-19: la creación pasó al laboratorio. D-11: la mensajería está pospuesta. */
  it('no ofrece "Nueva orden" ni "Mensajes"', () => {
    renderizar();

    expect(screen.queryByRole('link', { name: /nueva orden/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /mensaje/i })).not.toBeInTheDocument();
  });
});
