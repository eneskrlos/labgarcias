import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { EtiquetaEstado } from '../EtiquetaEstado';

describe('EtiquetaEstado', () => {
  it('muestra el nombre del estado, no el código', () => {
    render(<EtiquetaEstado estado="En produccion" estadoCodigo="EN_PRODUCCION" />);

    expect(screen.getByText('En produccion')).toBeInTheDocument();
    expect(screen.queryByText('EN_PRODUCCION')).not.toBeInTheDocument();
  });

  it('colorea CANCELADO con su color propio, no con el de error', () => {
    render(<EtiquetaEstado estado="Cancelado" estadoCodigo="CANCELADO" />);

    expect(screen.getByText('Cancelado')).toHaveStyle({ backgroundColor: '#F6E2E1', color: '#A03A32' });
  });

  it('un estadoCodigo desconocido no rompe el render', () => {
    render(<EtiquetaEstado estado="Algo nuevo" estadoCodigo="INEXISTENTE" />);

    expect(screen.getByText('Algo nuevo')).toBeInTheDocument();
  });
});
