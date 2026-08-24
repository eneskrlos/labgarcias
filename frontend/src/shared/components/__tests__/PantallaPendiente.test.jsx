import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { PantallaPendiente } from '../PantallaPendiente';

describe('PantallaPendiente', () => {
  /** §8: un ítem del menú cuya pantalla todavía no existe no puede llevar a una página en blanco. */
  it('anuncia la pantalla que falta y ofrece una salida', () => {
    render(
      <MemoryRouter>
        <PantallaPendiente titulo="Historial" detalle="Vas a poder consultar tus trabajos entregados." />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: 'Historial' })).toBeInTheDocument();
    expect(screen.getByText('Disponible próximamente.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Volver al inicio' })).toHaveAttribute('href', '/');
  });

  /** No tiene funcionalidad, y no debe adquirirla: la pantalla real es de otra tarea. */
  it('no ofrece ninguna acción', () => {
    render(
      <MemoryRouter>
        <PantallaPendiente titulo="Dashboard" />
      </MemoryRouter>,
    );

    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });
});
