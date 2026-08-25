import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import LicenciaFormulario from '../LicenciaFormulario';
import { registrarLicencia } from '../api';
import { ApiError } from '../../../shared/api/cliente';

vi.mock('../api', () => ({ registrarLicencia: vi.fn() }));

function Listado() {
  const location = useLocation();
  return (
    <div>
      <p>Listado de licencias</p>
      <p>{location.state?.mensaje}</p>
    </div>
  );
}

function renderizar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/admin/licencias/nueva']}>
        <Routes>
          <Route path="/admin/licencias/nueva" element={<LicenciaFormulario />} />
          <Route path="/admin/licencias" element={<Listado />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

async function completar(fin = '2026-12-31') {
  await userEvent.type(screen.getByLabelText('Inicio'), '2026-01-01');
  await userEvent.type(screen.getByLabelText('Vencimiento'), fin);
}

describe('LicenciaFormulario', () => {
  beforeEach(() => {
    registrarLicencia.mockReset().mockResolvedValue({ id: 1, estado: 'ACTIVA' });
  });

  /** CU-23/§3.6: los tres campos documentados, ni uno más. */
  it('pide inicio, vencimiento y observación', () => {
    renderizar();

    expect(screen.getByLabelText('Inicio')).toBeInTheDocument();
    expect(screen.getByLabelText('Vencimiento')).toBeInTheDocument();
    expect(screen.getByLabelText('Observación')).toBeInTheDocument();
  });

  /** P-11/P-12: no hay plan, ni precio, ni pasarela. */
  it('p11 p12 no pide plan ni precio ni medio de pago', () => {
    renderizar();

    for (const prohibido of [/plan/i, /precio/i, /pago/i, /tarjeta/i, /monto/i]) {
      expect(screen.queryByLabelText(prohibido)).not.toBeInTheDocument();
    }
  });

  it('registra el período con lo que se cargó', async () => {
    renderizar();
    await completar();
    await userEvent.type(screen.getByLabelText('Observación'), 'Renovación anual');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(registrarLicencia).toHaveBeenCalledWith({
      fechaInicio: '2026-01-01',
      fechaVencimiento: '2026-12-31',
      observacion: 'Renovación anual',
    });
  });

  /** §8.1 Regla 1: al guardar vuelve al listado con la confirmación. */
  it('al guardar vuelve al listado con su mensaje', async () => {
    renderizar();
    await completar();
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(await screen.findByText('Listado de licencias')).toBeInTheDocument();
    expect(screen.getByText('Período de licencia registrado.')).toBeInTheDocument();
  });

  /** §8.1 Regla 1: cancelar vuelve sin guardar. */
  it('cancelar vuelve al listado sin registrar', async () => {
    renderizar();
    await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(await screen.findByText('Listado de licencias')).toBeInTheDocument();
    expect(registrarLicencia).not.toHaveBeenCalled();
  });

  /**
   * El `422 FECHAS_LICENCIA_INVALIDAS` se muestra en el campo que devuelve el backend, y la
   * pantalla no adelanta la validación: manda y deja decidir (`Agente.md` §6.1).
   */
  it('muestra el 422 de fechas en el campo de vencimiento', async () => {
    registrarLicencia.mockRejectedValue(
      new ApiError(422, 'FECHAS_LICENCIA_INVALIDAS',
        'La fecha de vencimiento debe ser posterior a la fecha de inicio.', 'fechaVencimiento'),
    );
    renderizar();
    await completar('2025-01-01');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));

    const mensaje = await screen.findByText('La fecha de vencimiento debe ser posterior a la fecha de inicio.');
    expect(mensaje.closest('div')).toContainElement(screen.getByLabelText('Vencimiento'));
    // Con el rechazo, el formulario se queda: no navega al listado.
    expect(screen.queryByText('Listado de licencias')).not.toBeInTheDocument();
  });

  it('no bloquea el envío con fechas invertidas: lo rechaza el backend', async () => {
    renderizar();
    await completar('2025-01-01');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(registrarLicencia).toHaveBeenCalledWith(
      expect.objectContaining({ fechaInicio: '2026-01-01', fechaVencimiento: '2025-01-01' }),
    );
  });
});
