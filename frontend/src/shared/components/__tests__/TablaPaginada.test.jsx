import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { TablaPaginada } from '../TablaPaginada';

const COLUMNAS = [
  { clave: 'nombre', encabezado: 'Nombre' },
  { clave: 'estado', encabezado: 'Estado', render: (fila) => (fila.activo ? 'Activo' : 'Inactivo') },
];

const PROPS_BASE = {
  columnas: COLUMNAS,
  pagina: 0,
  tamano: 10,
  totalPaginas: 1,
  onCambiarPagina: vi.fn(),
  onCambiarTamano: vi.fn(),
};

describe('TablaPaginada', () => {
  it('estado cargando: muestra filas esqueleto según el tamaño de página, sin datos', () => {
    const { container } = render(<TablaPaginada {...PROPS_BASE} filas={[]} cargando tamano={10} />);

    expect(container.querySelectorAll('tbody tr')).toHaveLength(10);
  });

  it('estado error: muestra el mensaje y un botón Reintentar', async () => {
    const onReintentar = vi.fn();
    const usuarioEvento = userEvent.setup();

    render(<TablaPaginada {...PROPS_BASE} filas={[]} error="No se pudo cargar." onReintentar={onReintentar} />);

    expect(screen.getByText('No se pudo cargar.')).toBeInTheDocument();
    await usuarioEvento.click(screen.getByRole('button', { name: 'Reintentar' }));
    expect(onReintentar).toHaveBeenCalled();
  });

  it('estado vacío: muestra el mensaje y la acción provista', () => {
    render(
      <TablaPaginada
        {...PROPS_BASE}
        filas={[]}
        mensajeVacio="No hay tipos de trabajo."
        accionVacio={<a href="/admin/tipos-trabajo/nuevo">Nuevo</a>}
      />,
    );

    expect(screen.getByText('No hay tipos de trabajo.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Nuevo' })).toBeInTheDocument();
  });

  it('con datos: renderiza una fila por elemento usando las columnas dadas', () => {
    render(
      <TablaPaginada
        {...PROPS_BASE}
        filas={[
          { id: 1, nombre: 'PLACA ACTIVA', activo: true },
          { id: 2, nombre: 'VIEJO TIPO', activo: false },
        ]}
      />,
    );

    expect(screen.getByText('PLACA ACTIVA')).toBeInTheDocument();
    expect(screen.getByText('Activo')).toBeInTheDocument();
    expect(screen.getByText('VIEJO TIPO')).toBeInTheDocument();
    expect(screen.getByText('Inactivo')).toBeInTheDocument();
  });

  it('siempre renderiza los controles de paginación', () => {
    render(<TablaPaginada {...PROPS_BASE} filas={[]} />);

    expect(screen.getByText('Página 1 de 1')).toBeInTheDocument();
  });
});
