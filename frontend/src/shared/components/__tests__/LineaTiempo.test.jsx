import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { LineaTiempo } from '../LineaTiempo';

const ETAPAS = [
  { estado: 'Recibido', fechaHora: '2026-08-18T10:00:00-03:00', autor: null },
  { estado: 'En evaluacion', fechaHora: '2026-08-19T09:30:00-03:00', autor: 'Mona' },
];

describe('LineaTiempo', () => {
  it('muestra cada etapa con su fecha y autor, "Sistema" cuando no hay autor', () => {
    render(<LineaTiempo etapas={ETAPAS} />);

    expect(screen.getByText('Recibido')).toBeInTheDocument();
    expect(screen.getByText('En evaluacion')).toBeInTheDocument();
    expect(screen.getByText('Mona')).toBeInTheDocument();
    expect(screen.getByText('Sistema')).toBeInTheDocument();
  });

  /** §5.4: ninguna etapa es un <button> — el avance no se dispara desde acá (RN-04, §5.5). */
  it('ninguna etapa es interactiva: no hay botones ni controles', () => {
    render(<LineaTiempo etapas={ETAPAS} />);

    expect(screen.queryAllByRole('button')).toHaveLength(0);
  });

  /** No se sintetiza una tercera categoría "pendiente": lineaTiempo solo trae lo ya ocurrido. */
  it('no agrega filas más allá de las etapas que ya ocurrieron', () => {
    render(<LineaTiempo etapas={ETAPAS} />);

    expect(screen.getAllByRole('listitem')).toHaveLength(2);
  });

  it('la lista está vacía si no hay etapas, sin romper', () => {
    render(<LineaTiempo etapas={[]} />);

    expect(screen.queryAllByRole('listitem')).toHaveLength(0);
  });
});
