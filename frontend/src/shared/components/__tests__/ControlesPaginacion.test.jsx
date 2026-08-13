import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ControlesPaginacion } from '../ControlesPaginacion';

describe('ControlesPaginacion', () => {
  it('muestra la página actual en base 1 y el total de páginas', () => {
    render(
      <ControlesPaginacion
        pagina={0}
        tamano={10}
        totalPaginas={5}
        onCambiarPagina={vi.fn()}
        onCambiarTamano={vi.fn()}
      />,
    );

    expect(screen.getByText('Página 1 de 5')).toBeInTheDocument();
  });

  it('deshabilita "Anterior" en la primera página y "Siguiente" en la última', () => {
    const { rerender } = render(
      <ControlesPaginacion
        pagina={0}
        tamano={10}
        totalPaginas={5}
        onCambiarPagina={vi.fn()}
        onCambiarTamano={vi.fn()}
      />,
    );
    expect(screen.getByRole('button', { name: 'Anterior' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Siguiente' })).toBeEnabled();

    rerender(
      <ControlesPaginacion
        pagina={4}
        tamano={10}
        totalPaginas={5}
        onCambiarPagina={vi.fn()}
        onCambiarTamano={vi.fn()}
      />,
    );
    expect(screen.getByRole('button', { name: 'Anterior' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Siguiente' })).toBeDisabled();
  });

  it('"Siguiente" llama a onCambiarPagina con pagina + 1', async () => {
    const onCambiarPagina = vi.fn();
    const usuarioEvento = userEvent.setup();

    render(
      <ControlesPaginacion
        pagina={1}
        tamano={10}
        totalPaginas={5}
        onCambiarPagina={onCambiarPagina}
        onCambiarTamano={vi.fn()}
      />,
    );
    await usuarioEvento.click(screen.getByRole('button', { name: 'Siguiente' }));

    expect(onCambiarPagina).toHaveBeenCalledWith(2);
  });

  it('el selector ofrece 10/20/30 y llama a onCambiarTamano', async () => {
    const onCambiarTamano = vi.fn();
    const usuarioEvento = userEvent.setup();

    render(
      <ControlesPaginacion
        pagina={0}
        tamano={10}
        totalPaginas={1}
        onCambiarPagina={vi.fn()}
        onCambiarTamano={onCambiarTamano}
      />,
    );

    const selector = screen.getByRole('combobox');
    expect(screen.getAllByRole('option')).toHaveLength(3);
    await usuarioEvento.selectOptions(selector, '30');

    expect(onCambiarTamano).toHaveBeenCalledWith(30);
  });
});
