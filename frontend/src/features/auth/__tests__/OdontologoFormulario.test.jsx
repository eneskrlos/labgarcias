import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import OdontologoFormulario from '../OdontologoFormulario';
import { crearOdontologo } from '../api';
import { ApiError } from '../../../shared/api/cliente';

vi.mock('../api', () => ({
  crearOdontologo: vi.fn(),
}));

const SOLICITUD = {
  id: 12,
  nombreCompleto: 'Dr. Juan Pérez',
  correo: 'juan@mail.com',
  direccion: 'Av. 18 de Julio 1234',
  telefono: '+59891234567',
};

/** Muestra el mensaje con el que vuelve el formulario, para poder afirmarlo en el test. */
function DestinoDeVuelta({ nombre }) {
  const location = useLocation();
  return (
    <div>
      <p>{nombre}</p>
      <p>{location.state?.mensaje}</p>
    </div>
  );
}

function renderizar(estado) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[{ pathname: '/admin/odontologos/nuevo', state: estado }]}>
        <Routes>
          <Route path="/admin/odontologos/nuevo" element={<OdontologoFormulario />} />
          <Route path="/admin/solicitudes" element={<DestinoDeVuelta nombre="Pantalla de solicitudes" />} />
          <Route path="/" element={<DestinoDeVuelta nombre="Pantalla de inicio" />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('OdontologoFormulario', () => {
  beforeEach(() => {
    crearOdontologo.mockReset().mockResolvedValue({
      id: 20,
      nombreCompleto: 'Dr. Juan Pérez',
      correo: 'juan@mail.com',
      nombreUsuario: 'jperez',
      estadoCuenta: 'ACTIVA',
      debeCambiarPassword: true,
    });
  });

  /** §3.1.b: la contraseña la genera el backend; el formulario no la pide. */
  it('no pide contraseña', () => {
    renderizar();

    expect(screen.queryByLabelText(/contrase/i)).not.toBeInTheDocument();
    expect(screen.getByText(/la contraseña la genera el sistema/i)).toBeInTheDocument();
  });

  it('precarga los datos de la solicitud de la que viene', () => {
    renderizar({ solicitud: SOLICITUD, origen: '/admin/solicitudes' });

    expect(screen.getByLabelText('Nombre completo')).toHaveValue('Dr. Juan Pérez');
    expect(screen.getByLabelText('Correo')).toHaveValue('juan@mail.com');
    expect(screen.getByLabelText('Teléfono')).toHaveValue('+59891234567');
    expect(screen.getByLabelText('Dirección')).toHaveValue('Av. 18 de Julio 1234');
    expect(screen.getByText('Alta a partir de la solicitud de acceso #12.')).toBeInTheDocument();
  });

  /** §3.1.b criterio 4: el solicitudId viaja con el alta, que es lo que aprueba la solicitud. */
  it('criterio 4: envía el solicitudId cuando viene de una solicitud', async () => {
    const usuarioEvento = userEvent.setup();
    renderizar({ solicitud: SOLICITUD, origen: '/admin/solicitudes' });

    await usuarioEvento.type(screen.getByLabelText('Nombre de usuario'), 'jperez');
    await usuarioEvento.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(crearOdontologo).toHaveBeenCalledWith({
      nombreCompleto: 'Dr. Juan Pérez',
      correo: 'juan@mail.com',
      nombreUsuario: 'jperez',
      direccion: 'Av. 18 de Julio 1234',
      telefono: '+59891234567',
      solicitudId: 12,
    });
  });

  it('el alta directa manda solicitudId nulo', async () => {
    const usuarioEvento = userEvent.setup();
    renderizar();

    await usuarioEvento.type(screen.getByLabelText('Nombre completo'), 'Dra. Ana Gómez');
    await usuarioEvento.type(screen.getByLabelText('Correo'), 'ana@mail.com');
    await usuarioEvento.type(screen.getByLabelText('Nombre de usuario'), 'agomez');
    await usuarioEvento.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(crearOdontologo).toHaveBeenCalledWith(expect.objectContaining({ solicitudId: null }));
  });

  it('al guardar vuelve al origen con la confirmación', async () => {
    const usuarioEvento = userEvent.setup();
    renderizar({ solicitud: SOLICITUD, origen: '/admin/solicitudes' });

    await usuarioEvento.type(screen.getByLabelText('Nombre de usuario'), 'jperez');
    await usuarioEvento.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(await screen.findByText('Pantalla de solicitudes')).toBeInTheDocument();
    expect(
      screen.getByText('Cuenta creada. Las credenciales se enviaron por correo al odontólogo.'),
    ).toBeInTheDocument();
  });

  it('cancelar vuelve al origen sin guardar', async () => {
    const usuarioEvento = userEvent.setup();
    renderizar({ solicitud: SOLICITUD, origen: '/admin/solicitudes' });

    await usuarioEvento.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(await screen.findByText('Pantalla de solicitudes')).toBeInTheDocument();
    expect(crearOdontologo).not.toHaveBeenCalled();
  });

  it('un nombre de usuario repetido muestra el error sobre su campo', async () => {
    crearOdontologo.mockRejectedValue(
      new ApiError(409, 'NOMBRE_USUARIO_YA_REGISTRADO', 'Ya existe una cuenta con ese nombre de usuario.',
        'nombreUsuario'),
    );
    const usuarioEvento = userEvent.setup();
    renderizar({ solicitud: SOLICITUD, origen: '/admin/solicitudes' });

    await usuarioEvento.type(screen.getByLabelText('Nombre de usuario'), 'jperez');
    await usuarioEvento.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(await screen.findByText('Ya existe una cuenta con ese nombre de usuario.')).toBeInTheDocument();
  });
});
