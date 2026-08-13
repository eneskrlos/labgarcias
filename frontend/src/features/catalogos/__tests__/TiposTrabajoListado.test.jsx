import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import TiposTrabajoListado from '../TiposTrabajoListado';
import { cambiarEstado, listarPaginado } from '../api';
import { ApiError } from '../../../shared/api/cliente';

vi.mock('../api', () => ({
  listarPaginado: vi.fn(),
  cambiarEstado: vi.fn(),
}));

const PAGINA_TIPOS = {
  contenido: [
    { id: 1, nombre: 'PLACA ACTIVA', diasEstimados: 7, precio: 250, activo: true },
    { id: 2, nombre: 'VIEJO TIPO', diasEstimados: 10, precio: 300, activo: false },
  ],
  total: 2,
  pagina: 0,
  tamano: 10,
  totalPaginas: 1,
};

function renderizarPantalla(entrada = ['/admin/tipos-trabajo'], estadoInicial) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const entradas = estadoInicial ? [{ pathname: entrada[0], state: estadoInicial }] : entrada;
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={entradas}>
        <Routes>
          <Route path="/admin/tipos-trabajo" element={<TiposTrabajoListado />} />
          <Route path="/admin/tipos-trabajo/nuevo" element={<div>Pantalla nuevo</div>} />
          <Route path="/admin/tipos-trabajo/:id/editar" element={<div>Pantalla editar</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('TiposTrabajoListado', () => {
  beforeEach(() => {
    listarPaginado.mockReset().mockResolvedValue(PAGINA_TIPOS);
    cambiarEstado.mockReset();
  });

  it('no contiene ningún formulario de alta o edición (§8.1 Regla 1, criterio 1)', async () => {
    renderizarPantalla();
    await screen.findByText('PLACA ACTIVA');

    expect(document.querySelector('form')).not.toBeInTheDocument();
  });

  it('muestra un botón "Nuevo" que navega a /admin/tipos-trabajo/nuevo', async () => {
    const usuarioEvento = userEvent.setup();
    renderizarPantalla();
    await screen.findByText('PLACA ACTIVA');

    await usuarioEvento.click(screen.getByRole('link', { name: 'Nuevo' }));

    expect(await screen.findByText('Pantalla nuevo')).toBeInTheDocument();
  });

  it('lista los tipos de trabajo de la página actual, incluidos los inactivos', async () => {
    renderizarPantalla();

    expect(await screen.findByText('PLACA ACTIVA')).toBeInTheDocument();
    expect(screen.getByText('VIEJO TIPO')).toBeInTheDocument();
    expect(screen.getByText('Activo')).toBeInTheDocument();
    expect(screen.getByText('Inactivo')).toBeInTheDocument();
  });

  it('estado de carga: pasa cargando=true a la tabla mientras no hay datos', () => {
    listarPaginado.mockReturnValue(new Promise(() => {}));
    renderizarPantalla();

    expect(document.querySelectorAll('tbody tr')).toHaveLength(10);
  });

  it('estado de error: muestra el mensaje del backend', async () => {
    listarPaginado.mockRejectedValue(new ApiError(500, 'ERROR_INTERNO', 'Ocurrió un error inesperado.'));
    renderizarPantalla();

    expect(await screen.findByText('Ocurrió un error inesperado.')).toBeInTheDocument();
  });

  it('estado vacío: muestra el mensaje y el acceso a "Nuevo"', async () => {
    listarPaginado.mockResolvedValue({ contenido: [], total: 0, pagina: 0, tamano: 10, totalPaginas: 0 });
    renderizarPantalla();

    expect(await screen.findByText('No hay tipos de trabajo cargados.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Crear el primero' })).toBeInTheDocument();
  });

  it('Editar navega a /admin/tipos-trabajo/{id}/editar', async () => {
    const usuarioEvento = userEvent.setup();
    renderizarPantalla();
    const fila = (await screen.findByText('PLACA ACTIVA')).closest('tr');

    await usuarioEvento.click(within(fila).getByRole('link', { name: 'Editar' }));

    expect(await screen.findByText('Pantalla editar')).toBeInTheDocument();
  });

  it('el botón activar/desactivar llama a cambiarEstado con el valor invertido', async () => {
    cambiarEstado.mockResolvedValue({ ...PAGINA_TIPOS.contenido[0], activo: false });
    const usuarioEvento = userEvent.setup();
    renderizarPantalla();
    const fila = (await screen.findByText('PLACA ACTIVA')).closest('tr');

    await usuarioEvento.click(within(fila).getByRole('button', { name: 'Desactivar' }));

    expect(cambiarEstado).toHaveBeenCalledWith(1, false);
  });

  it('muestra el mensaje de confirmación recibido por navegación desde el formulario', async () => {
    renderizarPantalla(['/admin/tipos-trabajo'], { mensaje: 'Tipo de trabajo creado.' });

    expect(await screen.findByText('Tipo de trabajo creado.')).toBeInTheDocument();
  });
});
