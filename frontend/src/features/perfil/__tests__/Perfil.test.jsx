import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import Perfil from '../Perfil';
import { SesionProvider } from '../../../shared/hooks/useSesion';
import { guardarSesion } from '../../../shared/api/token';
import { actualizarPerfil, conectarTelegram, desvincularTelegram, obtenerPerfil } from '../api';
import { ApiError } from '../../../shared/api/cliente';

vi.mock('../api', () => ({
  obtenerPerfil: vi.fn(),
  actualizarPerfil: vi.fn(),
  conectarTelegram: vi.fn(),
  desvincularTelegram: vi.fn(),
}));

const PERFIL_SIN_VINCULAR = {
  id: 12,
  nombreCompleto: 'Dr. Juan Pérez',
  correo: 'juan@mail.com',
  nombreUsuario: 'jperez',
  direccion: 'Av. 18 de Julio 1234',
  telefono: '+59891234567',
  rol: 'ODONTOLOGO',
  telegramVinculado: false,
};

const PERFIL_VINCULADO = { ...PERFIL_SIN_VINCULAR, telegramVinculado: true };

const ENLACE = 'https://t.me/labgarcias_bot?start=abc123';

function renderizar() {
  guardarSesion('token-de-prueba', { id: 12, nombreCompleto: 'Dr. Juan Pérez', rol: 'ODONTOLOGO' });
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <SesionProvider>
        <Perfil />
      </SesionProvider>
    </QueryClientProvider>,
  );
}

describe('Perfil', () => {
  afterEach(() => {
    localStorage.clear();
  });

  beforeEach(() => {
    obtenerPerfil.mockReset().mockResolvedValue(PERFIL_SIN_VINCULAR);
    conectarTelegram.mockReset().mockResolvedValue({ enlace: ENLACE });
    desvincularTelegram.mockReset().mockResolvedValue(null);
  });

  /** §7: nombre y dirección son campos editables; el resto se muestra como dato. */
  it('muestra los datos propios del usuario', async () => {
    renderizar();

    expect(await screen.findByLabelText('Nombre')).toHaveValue('Dr. Juan Pérez');
    expect(screen.getByLabelText('Dirección')).toHaveValue('Av. 18 de Julio 1234');
    expect(screen.getByText('juan@mail.com')).toBeInTheDocument();
    expect(screen.getByText('jperez')).toBeInTheDocument();
  });

  /** §6.5 paso 1: el perfil muestra el estado y ofrece conectar. */
  it('sin vincular muestra el estado y el botón de conectar', async () => {
    renderizar();

    expect(await screen.findByText('Telegram: no vinculado')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Conectar Telegram' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Desvincular' })).not.toBeInTheDocument();
  });

  /** §6.5 paso 2: conectar devuelve el enlace profundo, y el usuario lo abre. */
  it('conectar muestra el enlace al bot', async () => {
    renderizar();
    await screen.findByText('Telegram: no vinculado');

    await userEvent.click(screen.getByRole('button', { name: 'Conectar Telegram' }));

    const enlace = await screen.findByRole('link', { name: 'Abrir el bot de Telegram' });
    expect(enlace).toHaveAttribute('href', ENLACE);
    expect(conectarTelegram).toHaveBeenCalledTimes(1);
  });

  /** §6.5 paso 5: vinculado, el perfil lo dice y ofrece desvincular. */
  it('vinculado muestra el estado y el botón de desvincular', async () => {
    obtenerPerfil.mockResolvedValue(PERFIL_VINCULADO);
    renderizar();

    expect(await screen.findByText('Telegram: vinculado ✅')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Conectar Telegram' })).not.toBeInTheDocument();
  });

  it('desvincular llama al endpoint y refresca el perfil', async () => {
    obtenerPerfil.mockResolvedValue(PERFIL_VINCULADO);
    renderizar();
    await screen.findByText('Telegram: vinculado ✅');
    obtenerPerfil.mockResolvedValue(PERFIL_SIN_VINCULAR);

    await userEvent.click(screen.getByRole('button', { name: 'Desvincular' }));

    expect(await screen.findByText('Telegram: no vinculado')).toBeInTheDocument();
    expect(desvincularTelegram).toHaveBeenCalledTimes(1);
  });

  /** §6.5: sin bot configurado (P-20) el backend rechaza, y el motivo tiene que verse. */
  it('muestra el motivo cuando la vinculación no está configurada', async () => {
    conectarTelegram.mockRejectedValue(
      new ApiError(422, 'TELEGRAM_NO_CONFIGURADO', 'La vinculación con Telegram no está disponible.'),
    );
    renderizar();
    await screen.findByText('Telegram: no vinculado');

    await userEvent.click(screen.getByRole('button', { name: 'Conectar Telegram' }));

    expect(
      await screen.findByText('La vinculación con Telegram no está disponible.'),
    ).toBeInTheDocument();
  });

  it('un error al traer el perfil ofrece reintentar', async () => {
    obtenerPerfil.mockRejectedValue(new ApiError(500, 'ERROR', 'Ocurrió un error inesperado.'));
    renderizar();

    expect(await screen.findByText('No pudimos traer tu perfil.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });

  // ---------- T-28: §7, perfil editable ----------

  /** §7: se editan nombre y dirección, y solo eso llega al backend. */
  it('guarda el nombre y la dirección', async () => {
    actualizarPerfil.mockResolvedValue({ ...PERFIL_SIN_VINCULAR, nombreCompleto: 'Dr. Juan P. Pérez' });
    renderizar();
    await screen.findByLabelText('Nombre');

    await userEvent.clear(screen.getByLabelText('Nombre'));
    await userEvent.type(screen.getByLabelText('Nombre'), 'Dr. Juan P. Pérez');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(actualizarPerfil).toHaveBeenCalledWith({
      nombreCompleto: 'Dr. Juan P. Pérez',
      direccion: 'Av. 18 de Julio 1234',
    });
    expect(await screen.findByRole('status')).toHaveTextContent('Perfil actualizado.');
  });

  /**
   * §7: **ni el rol ni el correo son editables.** Se muestran como dato, no como campo: que no se
   * puedan cambiar tiene que verse en la pantalla, no depender de que el backend los rechace.
   */
  it('§7 no ofrece editar el rol ni el correo', async () => {
    renderizar();
    await screen.findByLabelText('Nombre');

    expect(screen.queryByLabelText('Correo')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Rol')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Usuario')).not.toBeInTheDocument();
    expect(screen.getByText('ODONTOLOGO')).toBeInTheDocument();
  });

  /**
   * El nombre se muestra en el saludo del panel y en el encabezado, que leen la sesión guardada:
   * si no se refresca, el usuario cambia su nombre y sigue viendo el anterior hasta salir.
   *
   * **La sesión no puede guardar más de lo que ya guardaba.** `GET/PUT /perfil` devuelve correo,
   * dirección, teléfono y el estado de Telegram; en `localStorage` solo van las cuatro claves que
   * puso el login. Este test falla si alguien guarda la respuesta entera.
   */
  it('§7 refrescar la sesión no amplía lo que se guarda en localStorage', async () => {
    actualizarPerfil.mockResolvedValue({ ...PERFIL_SIN_VINCULAR, nombreCompleto: 'Dr. Juan P. Pérez' });
    renderizar();
    // Se toma después de renderizar: es `renderizar` quien guarda la sesión inicial.
    const antes = Object.keys(JSON.parse(localStorage.getItem('labgarcias_usuario')));
    await screen.findByLabelText('Nombre');

    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    await screen.findByRole('status');

    const guardado = JSON.parse(localStorage.getItem('labgarcias_usuario'));
    expect(Object.keys(guardado).sort()).toEqual(antes.sort());
    expect(Object.keys(guardado).sort()).toEqual(['id', 'nombreCompleto', 'rol']);
    expect(guardado.nombreCompleto).toBe('Dr. Juan P. Pérez');
    expect(guardado.id).toBe(12);
    expect(guardado.rol).toBe('ODONTOLOGO');
    for (const filtrado of ['correo', 'direccion', 'telefono', 'telegramVinculado']) {
      expect(guardado).not.toHaveProperty(filtrado);
    }
  });

  /** El error de validación del backend se muestra en su campo. */
  it('muestra el error del backend en el campo que indica', async () => {
    actualizarPerfil.mockRejectedValue(
      new ApiError(400, 'VALIDACION', 'El nombre completo es obligatorio.', 'nombreCompleto'),
    );
    renderizar();
    await screen.findByLabelText('Nombre');

    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));

    const mensaje = await screen.findByText('El nombre completo es obligatorio.');
    expect(mensaje.closest('div')).toContainElement(screen.getByLabelText('Nombre'));
  });
});
