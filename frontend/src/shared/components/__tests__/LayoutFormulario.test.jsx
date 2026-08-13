import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { LayoutFormulario } from '../LayoutFormulario';

describe('LayoutFormulario', () => {
  it('el formulario tiene noValidate para no bloquear el submit antes de llegar al backend', () => {
    const { container } = render(<LayoutFormulario titulo="Nuevo" onSubmit={vi.fn()} onCancelar={vi.fn()} />);

    expect(container.querySelector('form')).toHaveAttribute('novalidate');
  });

  it('muestra el título y ejecuta onSubmit al enviar', async () => {
    const onSubmit = vi.fn((evento) => evento.preventDefault());
    const usuarioEvento = userEvent.setup();

    render(<LayoutFormulario titulo="Nuevo tipo de trabajo" onSubmit={onSubmit} onCancelar={vi.fn()} />);

    expect(screen.getByText('Nuevo tipo de trabajo')).toBeInTheDocument();
    await usuarioEvento.click(screen.getByRole('button', { name: 'Guardar' }));
    expect(onSubmit).toHaveBeenCalled();
  });

  it('Cancelar llama a onCancelar', async () => {
    const onCancelar = vi.fn();
    const usuarioEvento = userEvent.setup();

    render(<LayoutFormulario titulo="Nuevo" onSubmit={vi.fn()} onCancelar={onCancelar} />);
    await usuarioEvento.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(onCancelar).toHaveBeenCalled();
  });

  it('guardando deshabilita el botón y cambia su texto', () => {
    render(<LayoutFormulario titulo="Nuevo" onSubmit={vi.fn()} onCancelar={vi.fn()} guardando />);

    expect(screen.getByRole('button', { name: 'Guardando...' })).toBeDisabled();
  });

  it('muestra el error general cuando se provee', () => {
    render(<LayoutFormulario titulo="Nuevo" onSubmit={vi.fn()} onCancelar={vi.fn()} error="Ya existe ese nombre." />);

    expect(screen.getByText('Ya existe ese nombre.')).toBeInTheDocument();
  });
});
