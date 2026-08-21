import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import SolicitudesListado from '../SolicitudesListado';
import { listarSolicitudes, rechazarSolicitud } from '../api';
import { ApiError } from '../../../shared/api/cliente';

vi.mock('../api', () => ({
  listarSolicitudes: vi.fn(),
  rechazarSolicitud: vi.fn(),
}));

const PENDIENTE = {
  id: 12,
  nombreCompleto: 'Dr. Juan Pérez',
  correo: 'juan@mail.com',
  direccion: 'Av. 18 de Julio 1234',
  telefono: '+59891234567',
  estado: 'PENDIENTE',
  fechaCreacion: '2026-08-20T10:15:00-03:00',
  fechaResolucion: null,
};

const RECHAZADA = {
  ...PENDIENTE,
  id: 11,
  nombreCompleto: 'Dra. Ana Gómez',
  correo: 'ana@mail.com',
  estado: 'RECHAZADA',
  fechaResolucion: '2026-08-19T12:00:00-03:00',
};

const PAGINA = { contenido: [PENDIENTE], total: 1, pagina: 0, tamano: 10, totalPaginas: 1 };

function renderizar(entrada = '/admin/solicitudes') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[entrada]}>
        <Routes>
          <Route path="/admin/solicitudes" element={<SolicitudesListado />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('SolicitudesListado', () => {
  beforeEach(() => {
    listarSolicitudes.mockReset().mockResolvedValue(PAGINA);
    rechazarSolicitud.mockReset().mockResolvedValue({ ...PENDIENTE, estado: 'RECHAZADA' });
  });

  it('arranca mostrando las pendientes, que es lo que el admin tiene que resolver', async () => {
    renderizar();
    await screen.findByText('Dr. Juan Pérez');

    expect(listarSolicitudes).toHaveBeenCalledWith({ pagina: 0, tamano: 10, estado: 'PENDIENTE' });
  });

  it('respeta el estado que venga en la URL', async () => {
    listarSolicitudes.mockResolvedValue({ ...PAGINA, contenido: [RECHAZADA] });
    renderizar('/admin/solicitudes?page=0&size=10&estado=RECHAZADA');
    await screen.findByText('Dra. Ana Gómez');

    expect(listarSolicitudes).toHaveBeenCalledWith({ pagina: 0, tamano: 10, estado: 'RECHAZADA' });
  });

  it('"Todas" quita el filtro en vez de mandarlo como estado a la API', async () => {
    const usuarioEvento = userEvent.setup();
    renderizar();
    await screen.findByText('Dr. Juan Pérez');

    await usuarioEvento.selectOptions(screen.getByLabelText('Estado'), 'TODAS');

    await vi.waitFor(() => {
      expect(listarSolicitudes).toHaveBeenCalledWith({ pagina: 0, tamano: 10, estado: null });
    });
  });

  it('la paginación la resuelve el backend: pide la página siguiente', async () => {
    listarSolicitudes.mockResolvedValue({ ...PAGINA, total: 12, totalPaginas: 2 });
    const usuarioEvento = userEvent.setup();
    renderizar();
    await screen.findByText('Dr. Juan Pérez');

    await usuarioEvento.click(screen.getByRole('button', { name: 'Siguiente' }));

    await vi.waitFor(() => {
      expect(listarSolicitudes).toHaveBeenCalledWith({ pagina: 1, tamano: 10, estado: 'PENDIENTE' });
    });
  });

  it('"Rechazar" llama al endpoint con el id de la solicitud', async () => {
    const usuarioEvento = userEvent.setup();
    renderizar();
    const fila = (await screen.findByText('Dr. Juan Pérez')).closest('tr');

    await usuarioEvento.click(within(fila).getByRole('button', { name: 'Rechazar' }));

    expect(rechazarSolicitud).toHaveBeenCalledWith(12);
  });

  it('una solicitud ya resuelta no ofrece "Rechazar"', async () => {
    listarSolicitudes.mockResolvedValue({ ...PAGINA, contenido: [RECHAZADA] });
    renderizar('/admin/solicitudes?estado=RECHAZADA');
    const fila = (await screen.findByText('Dra. Ana Gómez')).closest('tr');

    expect(within(fila).queryByRole('button', { name: 'Rechazar' })).not.toBeInTheDocument();
  });

  it('no ofrece aprobar: aprobar es dar de alta la cuenta (§3.1.b)', async () => {
    renderizar();
    await screen.findByText('Dr. Juan Pérez');

    expect(screen.queryByRole('button', { name: /aprobar/i })).not.toBeInTheDocument();
  });

  it('no contiene ningún formulario de alta: las solicitudes nacen del formulario público', async () => {
    renderizar();
    await screen.findByText('Dr. Juan Pérez');

    expect(document.querySelector('form')).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Nuevo' })).not.toBeInTheDocument();
  });

  it('estado de carga: muestra el esqueleto de la tabla', () => {
    listarSolicitudes.mockReturnValue(new Promise(() => {}));
    renderizar();

    expect(document.querySelectorAll('tbody tr')).toHaveLength(10);
  });

  it('estado vacío: lo dice explícitamente', async () => {
    listarSolicitudes.mockResolvedValue({ contenido: [], total: 0, pagina: 0, tamano: 10, totalPaginas: 0 });
    renderizar();

    expect(await screen.findByText('No hay solicitudes para mostrar.')).toBeInTheDocument();
  });

  it('estado de error: muestra el mensaje del backend', async () => {
    listarSolicitudes.mockRejectedValue(new ApiError(403, 'ACCESO_DENEGADO', 'No tenés permiso.'));
    renderizar();

    expect(await screen.findByText('No tenés permiso.')).toBeInTheDocument();
  });
});
