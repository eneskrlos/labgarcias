import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import SolicitarAcceso from '../SolicitarAcceso';
import { solicitarAcceso } from '../api';
import { ApiError } from '../../../shared/api/cliente';

vi.mock('../api', () => ({
  solicitarAcceso: vi.fn(),
}));

const DATOS = {
  nombreCompleto: 'Dr. Juan Pérez',
  correo: 'juan@mail.com',
  direccion: 'Av. 18 de Julio 1234',
  telefono: '+59891234567',
};

function renderizar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/solicitar-acceso']}>
        <Routes>
          <Route path="/solicitar-acceso" element={<SolicitarAcceso />} />
          <Route path="/login" element={<div>Pantalla de login</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

async function completarFormulario(usuarioEvento) {
  await usuarioEvento.type(screen.getByLabelText('Nombre completo'), DATOS.nombreCompleto);
  await usuarioEvento.type(screen.getByLabelText('Correo'), DATOS.correo);
  await usuarioEvento.type(screen.getByLabelText('Dirección'), DATOS.direccion);
  await usuarioEvento.type(screen.getByLabelText('Teléfono'), DATOS.telefono);
}

describe('SolicitarAcceso', () => {
  beforeEach(() => {
    solicitarAcceso.mockReset().mockResolvedValue({
      mensaje: 'Solicitud enviada. El laboratorio se pondrá en contacto.',
    });
  });

  it('envía los cuatro campos de §3.1 al backend', async () => {
    const usuarioEvento = userEvent.setup();
    renderizar();
    await completarFormulario(usuarioEvento);

    await usuarioEvento.click(screen.getByRole('button', { name: 'Enviar solicitud' }));

    expect(solicitarAcceso).toHaveBeenCalledWith(DATOS);
  });

  it('no pide contraseña: la solicitud no crea cuenta (§3.1 criterio 1)', () => {
    renderizar();

    expect(screen.queryByLabelText(/contrase/i)).not.toBeInTheDocument();
  });

  it('muestra el mensaje de confirmación que devuelve el backend', async () => {
    const usuarioEvento = userEvent.setup();
    renderizar();
    await completarFormulario(usuarioEvento);

    await usuarioEvento.click(screen.getByRole('button', { name: 'Enviar solicitud' }));

    expect(
      await screen.findByText('Solicitud enviada. El laboratorio se pondrá en contacto.'),
    ).toBeInTheDocument();
  });

  it('un correo ya registrado muestra el error sobre el campo correo', async () => {
    solicitarAcceso.mockRejectedValue(
      new ApiError(409, 'CORREO_YA_REGISTRADO', 'Ya existe una cuenta con ese correo.', 'correo'),
    );
    const usuarioEvento = userEvent.setup();
    renderizar();
    await completarFormulario(usuarioEvento);

    await usuarioEvento.click(screen.getByRole('button', { name: 'Enviar solicitud' }));

    expect(await screen.findByText('Ya existe una cuenta con ese correo.')).toBeInTheDocument();
  });

  it('un error sin campo se muestra como error general del formulario', async () => {
    solicitarAcceso.mockRejectedValue(
      new ApiError(409, 'SOLICITUD_YA_EXISTENTE', 'Ya hay una solicitud pendiente con ese correo.'),
    );
    const usuarioEvento = userEvent.setup();
    renderizar();
    await completarFormulario(usuarioEvento);

    await usuarioEvento.click(screen.getByRole('button', { name: 'Enviar solicitud' }));

    expect(await screen.findByText('Ya hay una solicitud pendiente con ese correo.')).toBeInTheDocument();
  });

  it('ofrece volver al login', async () => {
    const usuarioEvento = userEvent.setup();
    renderizar();

    await usuarioEvento.click(screen.getByRole('link', { name: 'Volver al inicio de sesión' }));

    expect(await screen.findByText('Pantalla de login')).toBeInTheDocument();
  });
});
