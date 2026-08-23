import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { LayoutAutenticado } from '../LayoutAutenticado';

describe('LayoutAutenticado', () => {
  it('muestra el contenido de la pantalla y las acciones del encabezado', () => {
    render(
      <LayoutAutenticado acciones={<button type="button">Campana</button>}>
        <p>Contenido de la pantalla</p>
      </LayoutAutenticado>,
    );

    expect(screen.getByText('Contenido de la pantalla')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Campana' })).toBeInTheDocument();
  });

  it('no incluye menú de navegación: el de §8 lo arma la tarea que cree sus destinos', () => {
    render(
      <LayoutAutenticado acciones={null}>
        <p>Contenido</p>
      </LayoutAutenticado>,
    );

    expect(screen.queryByRole('navigation')).not.toBeInTheDocument();
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });
});
