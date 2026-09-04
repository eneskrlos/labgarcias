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

  /** Cada etapa lleva su ícono de flujo más el círculo de verificación: dos SVG por fila. */
  it('cada etapa trae su ícono y su círculo de verificación', () => {
    const { container } = render(<LineaTiempo etapas={ETAPAS} />);

    expect(container.querySelectorAll('svg')).toHaveLength(ETAPAS.length * 2);
  });

  /**
   * RN-04: sin `estadoActualCodigo`, la última etapa usa el ícono de su posición en el flujo
   * normal (acá, la 2ª: "En producción"), no el de cancelación.
   */
  it('sin cancelación, la última etapa usa el ícono de su posición en el flujo', () => {
    const etapas = [...ETAPAS, { estado: 'En produccion', fechaHora: '2026-08-20T08:00:00-03:00', autor: 'Mona' }];
    const { container } = render(<LineaTiempo etapas={etapas} estadoActualCodigo="EN_PRODUCCION" />);

    expect(container.querySelector('path[d="M6 6l12 12M18 6 6 18"]')).not.toBeInTheDocument();
  });

  /**
   * Una orden cancelada no respeta la posición del flujo lineal —puede caer en cualquier paso—,
   * así que el ícono de la última etapa no puede salir de `ICONOS_FLUJO` por índice: tiene que
   * venir de `estadoActualCodigo`, el único dato estable disponible para esa fila.
   */
  it('con estadoActualCodigo CANCELADO, la última etapa muestra el ícono de cancelación', () => {
    const etapas = [...ETAPAS, { estado: 'Cancelado', fechaHora: '2026-08-20T08:00:00-03:00', autor: 'Mona' }];
    const { container } = render(<LineaTiempo etapas={etapas} estadoActualCodigo="CANCELADO" />);

    expect(container.querySelector('path[d="M6 6l12 12M18 6 6 18"]')).toBeInTheDocument();
  });
});
