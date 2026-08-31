import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { DonaDistribucion } from '../DonaDistribucion';

const DISTRIBUCION = [
  { estadoCodigo: 'RECIBIDO', estadoNombre: 'Recibido', cantidad: 1 },
  { estadoCodigo: 'EN_PRODUCCION', estadoNombre: 'En producción', cantidad: 5 },
  { estadoCodigo: 'ENTREGADO', estadoNombre: 'Entregado', cantidad: 0 },
];

describe('DonaDistribucion', () => {
  it('describe la distribución completa en el aria-label, para quien no la ve', () => {
    render(<DonaDistribucion distribucion={DISTRIBUCION} cargando={false} />);

    const grafico = screen.getByRole('img');
    expect(grafico).toHaveAccessibleName(/Recibido: 1/);
    expect(grafico).toHaveAccessibleName(/En producción: 5/);
    // Un estado en cero no aporta nada a la lectura de la distribución.
    expect(grafico).toHaveAccessibleName(expect.not.stringMatching(/Entregado: 0/));
  });

  it('muestra el total en el centro, sumado sobre lo que ya trae el backend', () => {
    render(<DonaDistribucion distribucion={DISTRIBUCION} cargando={false} />);

    expect(screen.getByText('6')).toBeInTheDocument();
  });

  it('la leyenda incluye todas las etapas, también las que están en cero', () => {
    render(<DonaDistribucion distribucion={DISTRIBUCION} cargando={false} />);

    expect(screen.getByText('Recibido')).toBeInTheDocument();
    expect(screen.getByText('En producción')).toBeInTheDocument();
    expect(screen.getByText('Entregado')).toBeInTheDocument();
  });

  it('sin órdenes, el aria-label lo dice y no rompe', () => {
    render(<DonaDistribucion distribucion={[]} cargando={false} />);

    expect(screen.getByRole('img')).toHaveAccessibleName(/Sin órdenes registradas/);
  });

  it('con la lista vacía y sin cargar, muestra el mensaje de vacío', () => {
    render(<DonaDistribucion distribucion={[]} cargando={false} />);

    expect(screen.getByText('No hay etapas cargadas.')).toBeInTheDocument();
  });
});
