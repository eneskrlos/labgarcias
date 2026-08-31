import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { EncabezadoPantalla } from '../EncabezadoPantalla';

describe('EncabezadoPantalla', () => {
  it('muestra el título como encabezado de nivel 1', () => {
    render(<EncabezadoPantalla titulo="Odontólogos" />);

    expect(screen.getByRole('heading', { level: 1, name: 'Odontólogos' })).toBeInTheDocument();
  });

  it('renderiza el contenido de la derecha cuando se provee', () => {
    render(
      <EncabezadoPantalla titulo="Odontólogos">
        <a href="/admin/odontologos/nuevo">Nuevo</a>
      </EncabezadoPantalla>,
    );

    expect(screen.getByRole('link', { name: 'Nuevo' })).toBeInTheDocument();
  });

  it('sin children, no rompe y no agrega nada más', () => {
    render(<EncabezadoPantalla titulo="Usuarios" />);

    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });

  it('muestra la confirmación como texto de estado cuando se provee', () => {
    render(<EncabezadoPantalla titulo="Odontólogos" confirmacion="Cuenta creada." />);

    expect(screen.getByRole('status')).toHaveTextContent('Cuenta creada.');
  });

  it('sin confirmación, no renderiza el párrafo de estado', () => {
    render(<EncabezadoPantalla titulo="Odontólogos" />);

    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });
});
